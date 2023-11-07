import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PJOGL;
import util.async.Async;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;


public class Main extends PApplet {

    public enum Fractal {
        MANDELBROT("Mandelbrot Set", "C", new Complex(0, 0)),
        JULIA("Julia Set", "Z", new Complex(0, 0));

        public final String displayName;
        public final String seedLabel;
        @NotNull
        public final Complex defaultSeed;

        Fractal(String displayName, String seedLabel, @NotNull Complex defaultSeed) {
            this.displayName = displayName;
            this.seedLabel = seedLabel;
            this.defaultSeed = defaultSeed;
        }
    }

    public enum SeedAnimationMode {
        OFF("OFF"),
        PERIODIC("Periodic"),
        BY_MOUSE("Mouse");

        public final String displayName;

        SeedAnimationMode(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum ColorScheme {
        MONO_DARK("Mono Dark"),
        MONO_LIGHT("Mono Light"),
        HUE("Hue Cycle"),
        ;

        public final String displayName;

        ColorScheme(String displayName) {
            this.displayName = displayName;
        }
    }


    private static final Fractal DEFAULT_FRACTAL = Fractal.JULIA;
    private static final SeedAnimationMode DEFAULT_ANIMATION_MODE = SeedAnimationMode.PERIODIC;
    private static final ColorScheme DEFAULT_COLOR_SCHEME = ColorScheme.HUE;

    private static final double DEFAULT_X_MIN = -2;
    private static final double DEFAULT_X_MAX = 2;

    private static final double DEFAULT_Y_MIN = -2;
    private static final double DEFAULT_Y_MAX = 2;

    private static final double DEFAULT_CONTINUOUS_ZOOM_STEP_FRACTION = 0.05;     // 5% of the current length
    private static final double DEFAULT_CONTINUOUS_TRANSLATE_STEP_FRACTION = 0.05;     // 5% of the current length

    public static final boolean DEFAULT_DRAW_HUD = true;

    // Number of worker threads
    private static final int THREADS_MIN = 1;
    private static final int THREADS_MAX = Async.NO_CPU_CORES * 2;
    private static final int THREADS_DEFAULT = Async.NO_CPU_CORES;

    // Maximum number of iterations
    private static final int ITERATIONS_MIN = 10;
    private static final int ITERATIONS_MAX = 1000;
    private static final int ITERATIONS_DEFAULT = 100;
    private static final int ITERATIONS_STEP = 10;

    // Minimum Distance after which the computation is flagged as diverging
    private static final int DIVERGENCE_DISTANCE_MIN = 2;
    private static final int DIVERGENCE_DISTANCE_MAX = 100;
    private static final int DIVERGENCE_DISTANCE_DEFAULT = 4;
    private static final int DIVERGENCE_DISTANCE_STEP = 2;

    // Colors
    public static final Color ACCENT = new Color(107, 196, 255, 255);
    public static final Color ACCENT_HIGHLIGHT = new Color(255, 219, 77, 255);

    public static final boolean SHOW_TITLE = true;
    public static final float TITLE_SIZE = 0.026f;
    public static final Color FG_TITLE = ACCENT_HIGHLIGHT;

    public static final boolean SHOW_MAIN_STATUS = true;
    public static final float STATUS_MAIN_TEXT_SIZE = 0.02f;
    public static final Color FG_STATUS_MAIN = Color.WHITE;

    public static final boolean SHOW_SEC_STATUS = true;
    public static final float STATUS_SEC_TEXT_SIZE = 0.02f;
    public static final Color FG_STATUS_SEC = Color.WHITE;

    public static <T extends Enum<T>> T cycleEnum(@NotNull Class<T> clazz, int curOrdinal) {
        final T[] values = clazz.getEnumConstants();

        if (values.length == 0)
            return null;

        int nextI = curOrdinal + 1;
        if (nextI >= values.length) {
            nextI = 0;
        }

        return values[nextI];
    }

    public static <T extends Enum<T>> T cycleEnum(@NotNull Class<T> clazz, T current) {
        return cycleEnum(clazz, current != null ? current.ordinal() : -1);
    }


    public static int iterateMandelbrot(@NotNull Complex z0, @NotNull Complex c, int maxIterations, double divergeDistance) {
        final double dsq = divergeDistance * divergeDistance;
        int itr = 0;

        double re = z0.re;
        double img = z0.img;

        double nre;
        double nimg;
        while (itr < maxIterations) {
            nre = (re * re - img * img) + c.re;
            nimg = (2 * re * img) + c.img;

            // If distance of new point from the seed is greater than divergenceDistance, break
            double re_temp = nre - z0.re;
            double img_temp = nimg - z0.img;
            if ((re_temp * re_temp + img_temp * img_temp) >= dsq)
                break;      // diverges

            re = nre;
            img = nimg;
            itr++;
        }

        return itr;
    }

    public static double map(double value, double start1, double stop1, double start2, double stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }


    @NotNull
    public static Dimension windowSize(int displayW, int displayH) {
        return new Dimension(Math.round(displayW / 1.4f), Math.round(displayH / 1.3f));
    }

    public static float getTextSize(float width, float height, float size) {
        return Math.min(width, height) * size;
    }

    public float getTextSize(float size) {
        return getTextSize(width, height, size);
    }


    @NotNull
    private Fractal fractal = DEFAULT_FRACTAL;
    @NotNull
    private Complex seed = fractal.defaultSeed;
    private int maxIterations = ITERATIONS_DEFAULT;
    private int divergenceDistance = DIVERGENCE_DISTANCE_DEFAULT;
    private int numThreads = THREADS_DEFAULT;     // number of worker threads

    @NotNull
    private Main.SeedAnimationMode animMode = DEFAULT_ANIMATION_MODE;
    private float animAngle = 0f;
    private float animAngleStep = 0.02f;

    @NotNull
    private ColorScheme colorScheme = DEFAULT_COLOR_SCHEME;

    private double xMin = DEFAULT_X_MIN;
    private double xMax = DEFAULT_X_MAX;
    private double yMin = DEFAULT_Y_MIN;
    private double yMax = DEFAULT_Y_MAX;

    /* Ui */
    private int _w, _h;
    @Nullable
    private KeyEvent mKeyEvent;
    private PFont pdSans, pdSansMedium;
    private boolean drawHud = DEFAULT_DRAW_HUD;
    private int mFrameInvalidated = 0;

    /* Setup and Drawing */

    @Override
    public void settings() {
        final Dimension size = windowSize(displayWidth, displayHeight);
        size(size.width, size.height, P2D);
//        fullScreen(P2D);
        smooth(4);

        _w = width;
        _h = height;

        ensureYAspectRatio();

        if (R.MANDELBROT_ICON != null) {
            PJOGL.setIcon(R.MANDELBROT_ICON.toString());       // icon
        }
    }

    @Override
    public void setup() {
        surface.setTitle(R.TITLE_MANDELBROT);
        surface.setResizable(true);

        colorMode(HSB, 1.0f, 1.0f, 1.0f, 1.0f);
        pixelDensity(1);

        pdSans = createFont(R.FONT_PD_SANS_REGULAR.toString(), 20);
        pdSansMedium = createFont(R.FONT_PD_SANS_MEDIUM.toString(), 20);

        textFont(pdSans);       // Default
    }

    public void zoom(double step /* -ve for zoom in */) {
        final double x_delta = Math.abs(xMax - xMin) * step * 0.5;
        xMin -= x_delta;
        xMax += x_delta;

        final double y_delta = Math.abs(yMax - yMin) * step * 0.5;
        yMin -= y_delta;
        yMax += y_delta;

        invalidateFrame();
    }

    public void continuousZoom(boolean zoomIn) {
        zoom((zoomIn ? -1 : 1) * DEFAULT_CONTINUOUS_ZOOM_STEP_FRACTION);
    }

    public void translate(double x_step, double y_step) {
        xMin += x_step;
        xMax += x_step;

        yMin += y_step;
        yMax += y_step;

        invalidateFrame();
    }

    public void continuousTranslate(int tx /* [-1,0,1] */, int ty /* [-1,0,1] */) {
        final double x_step = Math.abs(xMax - xMin) * tx * DEFAULT_CONTINUOUS_TRANSLATE_STEP_FRACTION;
        final double y_step = Math.abs(yMax - yMin) * ty * DEFAULT_CONTINUOUS_TRANSLATE_STEP_FRACTION;

        translate(x_step, y_step);
    }

    protected void onResized(int w, int h) {
        ensureYAspectRatio();
        invalidateFrame();
    }

    public void preDraw() {
        if (_w != width || _h != height) {
            _w = width;
            _h = height;
            onResized(width, height);
        }

        /* Handle Keys [Continuous] */
        if (keyPressed && mKeyEvent != null) {
            onContinuousKeyPressed(mKeyEvent);
        }
    }

    @Nullable
    public String getMainStatusText() {
        return String.format("Iterations: %d  |  Divergence: %d  |  %s: %.3f %+.3fi  ", maxIterations, divergenceDistance, fractal.seedLabel, seed.re, seed.img);
    }

    @Nullable
    public String getSecStatusText() {
        return String.format("Threads: %d  |  Animation: %s  |  Colors: %s", numThreads, animMode.displayName, colorScheme.displayName);
    }

    @Override
    public void draw() {
        preDraw();

        switch (animMode) {
            case PERIODIC -> {
                seed = Complex.polar(0.7885, animAngle);
                animAngle += animAngleStep;
                drawFrame();
            }
            case BY_MOUSE -> {
                seed = new Complex(map(mouseX, 0, width, xMin, xMax), map(mouseY, 0, height, yMax, yMin));
                drawFrame();
            }
            default -> {
                if (mFrameInvalidated < 2) {
                    drawFrame();
                    mFrameInvalidated++;
                }
            }
        }

        // Zoom Rect

//        if (mousePivot1 != null && mousePivot2 != null) {
//            pushStyle();
//            fill(0f, 0.1f);
//            strokeWeight(0);
//            rectMode(CORNERS);
//            rect(Math.min(mousePivot1.x, mousePivot2.x), Math.min(mousePivot1.y, mousePivot2.y), Math.max(mousePivot1.x, mousePivot2.x), Math.max(mousePivot1.y, mousePivot2.y));
//            popStyle();
//        }



//        noLoop();

        postDraw();
    }

    protected void postDraw() {
    }


    @Override
    public void keyPressed(KeyEvent event) {
        super.keyPressed(event);
        mKeyEvent = event;

//        final char key = event.getKey();
        final int keyCode = event.getKeyCode();
//        println("KeyEvent-> key: " + key + ", code: " + keyCode + ", ctrl: " + event.isControlDown() + ", alt: " + event.isAltDown() + ", shift: " + event.isShiftDown());

        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_F -> nextFractal();
            case java.awt.event.KeyEvent.VK_C -> nextColorScheme();
            case java.awt.event.KeyEvent.VK_H -> toggleHud();

            case java.awt.event.KeyEvent.VK_R -> {
                if (event.isControlDown()) {
                    resetAll();
                } else if (event.isShiftDown()) {
                    resetView(true);
                } else {
                    resetSeed(true);
                }
            }

            case java.awt.event.KeyEvent.VK_S -> {
                if (event.isControlDown()) {
                    snapshot();
                } else {
                    nextSeedAnimationMode();
                }
            }

            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> {
                if (event.isShiftDown()) {
                    changeNumberOfThreads(true);
                }
            }

            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
                if (event.isShiftDown()) {
                    changeNumberOfThreads(false);
                }
            }
        }
    }

    private void onContinuousKeyPressed(@NotNull KeyEvent event) {
        switch (event.getKeyCode()) {
            case java.awt.event.KeyEvent.VK_UP -> {
                if (event.isControlDown()) {
                    continuousZoom(true);
                } else {
                    continuousTranslate(0, 1);      // up
                }
            }

            case java.awt.event.KeyEvent.VK_DOWN -> {
                if (event.isControlDown()) {
                    continuousZoom(false);
                } else {
                    continuousTranslate(0, -1);      // down
                }
            }

            case java.awt.event.KeyEvent.VK_LEFT -> continuousTranslate(-1, 0);      // left

            case java.awt.event.KeyEvent.VK_RIGHT -> continuousTranslate(1, 0);      // right

            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA, java.awt.event.KeyEvent.VK_PLUS -> {
                if (event.isControlDown()) {
                    changeDivergenceDistance(true);
                } else if (!(event.isShiftDown() || event.isAltDown())) {
                    changeMaxIterations(true);
                }
            }

            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
                if (event.isControlDown()) {
                    changeDivergenceDistance(false);
                } else if (!(event.isShiftDown() || event.isAltDown())) {
                    changeMaxIterations(false);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        super.keyReleased(event);
        if (mKeyEvent != null && mKeyEvent.getKeyCode() == event.getKeyCode()) {
            mKeyEvent = null;
        }
    }


    @Nullable
    private Point mousePivot1, mousePivot2;

    @Override
    public void mousePressed(MouseEvent event) {
        super.mousePressed(event);

        mousePivot1 = mousePivot2 = null;
        if (event.getButton() == LEFT) {
            mousePivot1 = new Point(event.getX(), event.getY());
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        super.mouseDragged(event);

        if (event.getButton() == LEFT && mousePivot1 != null) {
            mousePivot2 = new Point(event.getX(), event.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        super.mouseReleased(event);

        if (mousePivot1 != null && mousePivot2 != null) {
            final int x1 = Math.min(mousePivot1.x, mousePivot2.x);
            final int x2 = Math.max(mousePivot1.x, mousePivot2.x);
            final int y1 = Math.min(mousePivot1.y, mousePivot2.y);
            final int y2 = Math.max(mousePivot1.y, mousePivot2.y);

            final double cx1 = map(x1, 0, width, xMin, xMax);
            final double cx2 = map(x2, 0, width, xMin, xMax);
            final double cy1 = map(y1, 0, height, yMax, yMin);
            final double cy2 = map(y2, 0, height, yMax, yMin);

            xMin = cx1;
            xMax = cx2;
            yMin = cy2;
            yMax = cy1;

            invalidateFrame();
        }

        mousePivot1 = mousePivot2 = null;

    }

    private void onAnimationModeChanged(@Nullable Main.SeedAnimationMode prev, @NotNull Main.SeedAnimationMode cur) {
        println("Animation Mode: " + cur.displayName);
        invalidateFrame();
    }

    private void setSeedAnimationMode(@NotNull SeedAnimationMode mode) {
        if (animMode == mode)
            return;

        final SeedAnimationMode prev = animMode;
        animMode = mode;
        onAnimationModeChanged(prev, animMode);
    }

    private void nextSeedAnimationMode() {
        setSeedAnimationMode(cycleEnum(SeedAnimationMode.class, animMode));
    }

    private void nextFractal() {
        final Fractal prev = fractal;
        fractal = cycleEnum(Fractal.class, fractal);

        if (prev != fractal) {
            onFractalChanged(prev, fractal);
        }
    }

    private void onFractalChanged(@Nullable Fractal prev, @NotNull Fractal cur) {
        println("Fractal: " + cur.displayName);
//        resetSeed(false);
        invalidateFrame();
    }

    private void nextColorScheme() {
        final ColorScheme prev = colorScheme;
        colorScheme = cycleEnum(ColorScheme.class, colorScheme);

        if (prev != colorScheme) {
            onColorSchemeChanged(prev, colorScheme);
        }
    }

    private void onColorSchemeChanged(@Nullable ColorScheme prev, @NotNull ColorScheme cur) {
        println("Color Scheme: " + cur.displayName);
        invalidateFrame();
    }


    private void changeNumberOfThreads(boolean inc, boolean update) {
        final int prev = numThreads;
        numThreads = constrain(numThreads + (inc ? 1 : -1), THREADS_MIN, THREADS_MAX);

        if (prev != numThreads) {
            println("Threads: " + numThreads);
            if (update) {
                invalidateFrame();
            }
        }
    }

    private void changeNumberOfThreads(boolean inc) {
        changeNumberOfThreads(inc, true);
    }

    private void changeMaxIterations(boolean inc, boolean update) {
        final int prev = maxIterations;
        maxIterations = constrain(maxIterations + ((inc ? 1 : -1) * ITERATIONS_STEP), ITERATIONS_MIN, ITERATIONS_MAX);

        if (prev != maxIterations) {
            println("Maximum Iterations: " + maxIterations);

            if (update) {
                invalidateFrame();
            }
        }
    }

    private void changeMaxIterations(boolean inc) {
        changeMaxIterations(inc, true);
    }

    private void changeDivergenceDistance(boolean inc, boolean update) {
        final int prev = divergenceDistance;
        divergenceDistance = constrain(divergenceDistance + ((inc ? 1 : -1) * DIVERGENCE_DISTANCE_STEP), DIVERGENCE_DISTANCE_MIN, DIVERGENCE_DISTANCE_MAX);

        if (prev != divergenceDistance) {
            println("Divergence Distance: " + divergenceDistance);

            if (update) {
                invalidateFrame();
            }
        }
    }

    private void changeDivergenceDistance(boolean inc) {
        changeDivergenceDistance(inc, true);
    }

    public void setSeed(@NotNull Complex seed, boolean stopAnimation) {
        if (this.seed.equals(seed))
            return;

        if (stopAnimation) {
            setSeedAnimationMode(SeedAnimationMode.OFF);
        }

        this.seed = seed;
        println("Seed: " + seed);
        invalidateFrame();
    }

    public void setDrawHud(boolean drawHud) {
        if (this.drawHud == drawHud)
            return;

        this.drawHud = drawHud;
        onDrawHudChanged(this.drawHud);
    }

    public void toggleHud() {
        setDrawHud(!drawHud);
    }

    private void onDrawHudChanged(boolean drawHud) {
        invalidateFrame();
        println("HUD " + (drawHud? "ON": "OFF"));
    }

    private void snapshot() {
        String file_name = fractal.displayName + "_" + colorScheme.displayName + "_seed_" + seed + ".png";
        file_name = Format.replaceAllWhiteSpaces(file_name.toLowerCase(), "_");

        saveFrame(file_name);
        println(R.SHELL_ROOT + "Frame saved to " + file_name);
    }


    public void ensureYAspectRatio() {
        final float asp = (float) width / height;
        final double y_range = Math.abs(xMax - xMin) / asp;

        final double delta = (y_range - Math.abs(yMax - yMin)) / 2;
        yMin -= delta;
        yMax += delta;
    }

    public void resetView(boolean update) {
        xMin = DEFAULT_X_MIN;
        xMax = DEFAULT_X_MAX;
        yMin = DEFAULT_Y_MIN;
        yMax = DEFAULT_Y_MAX;

        ensureYAspectRatio();
        if (update)
            invalidateFrame();
    }

    public void resetSeed(boolean update) {
        seed = fractal.defaultSeed;
        if (update || animMode == SeedAnimationMode.OFF) {
            invalidateFrame();
        }
    }

    public void resetAll() {
        resetView(false);
        resetSeed(false);

        numThreads = THREADS_DEFAULT;
        divergenceDistance = DIVERGENCE_DISTANCE_DEFAULT;
        maxIterations = ITERATIONS_DEFAULT;

        invalidateFrame();
    }

    public int toColor(int itr, int maxIterations) {
        return switch (colorScheme) {
            case MONO_DARK ->
                    itr == maxIterations ? color(0, 0, 0) : color(0.68f, 1, sqrt((float) itr / maxIterations));
            case MONO_LIGHT ->
                    itr == maxIterations ? color(0, 0, 0) : color(0.86f, 1, sqrt((float) itr / maxIterations));
            case HUE -> itr == maxIterations ? color(0, 0, 0) : color(sqrt((float) itr / maxIterations), 1, 1);
        };
    }

    public int computePixelColor(int x, int y, @NotNull Fractal fractal) {
        // Mapping pixel position to complex coordinates
        final Complex pixelValue = new Complex(map(x, 0, pixelWidth, xMin, xMax), map(y, 0, pixelHeight, yMax, yMin));

        final int itr = switch (fractal) {

            // .................  Mandelbrot Set (Parameter space: each pixel is mapped to C, Z0 = constant)  ..........................
            case MANDELBROT -> iterateMandelbrot(seed, pixelValue, maxIterations, divergenceDistance);

            // .................  Julia Set (Input space: each pixel is mapped to Z0, C = constant)  ..........................
            case JULIA -> iterateMandelbrot(pixelValue, seed, maxIterations, divergenceDistance);
        };

        return toColor(itr, maxIterations);
    }

    private void drawFrame() {
        loadPixels();

        if (numThreads > 1) {
            List<Callable<Void>> tasks = createUpdateTasks(numThreads);

            try {
                Async.THREAD_POOL_EXECUTOR.invokeAll(tasks);
            } catch (InterruptedException ignored) {
            }
        } else {
            for (int y = 0; y < pixelHeight; y++) {
                for (int x = 0; x < pixelWidth; x++) {
                    pixels[x + y * pixelWidth] = computePixelColor(x, y, fractal);
                }
            }

            updatePixels();
        }

        drawHud();
    }

    private void drawHud() {
        if (!drawHud)
            return;

        final float h_offset = width * 0.009f;
        final float v_offset = height / 96f;

        // Status bar
        final float statusMainTextSize = getTextSize(STATUS_MAIN_TEXT_SIZE);
        final float statusSecTextSize = getTextSize(STATUS_SEC_TEXT_SIZE);

        if (SHOW_MAIN_STATUS) {
            final String mainStatusText = getMainStatusText();
            if (mainStatusText != null && !mainStatusText.isEmpty()) {
                pushStyle();
                fill(FG_STATUS_MAIN.getRGB());
                textFont(pdSans, statusMainTextSize);
                textAlign(LEFT, BOTTOM);
                text(mainStatusText, h_offset, height - v_offset);
                popStyle();
            }
        }

        if (SHOW_SEC_STATUS) {
            final String secStatusText = getSecStatusText();
            if (secStatusText != null && !secStatusText.isEmpty()) {
                pushStyle();
                fill(FG_STATUS_SEC.getRGB());
                textFont(pdSans, statusSecTextSize);
                textAlign(RIGHT, BOTTOM);
                text(secStatusText, width - h_offset, height - v_offset);
                popStyle();
            }
        }

        // Title
        if (SHOW_TITLE) {
            pushStyle();
            fill(FG_TITLE.getRGB());
            textFont(pdSans, getTextSize(TITLE_SIZE));
            textAlign(LEFT, TOP);
            text(fractal.displayName, h_offset, v_offset);
            popStyle();
        }
    }

    public void invalidateFrame(boolean hard) {
        mFrameInvalidated = hard? 0: 1;
    }

    public void invalidateFrame() {
        invalidateFrame(false);
    }

    @NotNull
    private List<Callable<Void>> createUpdateTasks(int threads) {
        List<Callable<Void>> tasks = new LinkedList<>();
        final int y_step = pixelHeight / threads;

        for (int i = 0; i < threads; i++) {
            final int y_start = i * y_step;
            final int y_end = (i == threads - 1) ? pixelHeight : y_start + y_step;

            tasks.add(() -> {
                for (int y = y_start; y < y_end; y++) {
                    for (int x = 0; x < pixelWidth; x++) {
                        pixels[x + y * pixelWidth] = computePixelColor(x, y, fractal);
                    }
                }

                updatePixels(0, y_start, pixelWidth, y_end);
                return null;
            });
        }
        return tasks;
    }


    public static void init(String[] args) {
        R.createDescriptionReadme();
    }

    public static void main(String[] args) {
        init(args);

        final Main app = new Main();
        PApplet.runSketch(PApplet.concat(new String[]{app.getClass().getName()}, args), app);

        println(R.DES_FULL);
        boolean running = true;
        Scanner sc;

        while (running) {
            sc = new Scanner(System.in);
            print(R.SHELL_ROOT);

            final String cmd = sc.nextLine().trim();
            if (Format.isEmpty(cmd))
                continue;

            if (cmd.equals("exit") || cmd.equals("quit")) {
                running = false;
            } else if (cmd.equals("hud") || cmd.equals("toggle hud")) {
                app.toggleHud();
            } else if (cmd.equals("color") || cmd.equals("change color") || cmd.equals("color scheme")) {
                app.nextColorScheme();
            } else if (cmd.equals("anim") || cmd.equals("animation") || cmd.equals("change anim") || cmd.equals("change sc")) {
                app.nextSeedAnimationMode();
            } else if (cmd.equals("fractal") || cmd.equals("change fractal")) {
                app.nextFractal();
            } else if (cmd.equals("save") || cmd.equals("screenshot") || cmd.equals("snapshot")) {
                app.snapshot();
            }  else if (cmd.startsWith("seed")) {
                final String left = cmd.substring(4).trim();
                final Runnable usage_pr = () -> println(R.SHELL_SEED + "Usage: seed <complex number>\nExample: seed -0.8 + 0.156i");

                if (left.isEmpty()) {
                    usage_pr.run();
                    continue;
                }

                try {
                    Complex seed = Complex.parse(left);
                    app.setSeed(seed, true);
                } catch (Exception exc) {
                    System.err.println(R.SHELL_SEED + " Failed to parse complex number\n" + exc);
                    usage_pr.run();
                }
            } else if (cmd.startsWith("reset")) {
                final String left = cmd.substring(5).trim();
                final Runnable usage_pr = () -> println(R.SHELL_ROOT + "Usage: reset [view | seed | all]\nExample: reset view");

                if (left.isEmpty() || left.equals("all")) {
                    app.resetAll();
                } else if (left.equals("view")) {
                    app.resetView(true);
                } else if (left.equals("seed")) {
                    app.resetSeed(true);
                } else {
                    System.err.println(R.SHELL_ROOT + "Invalid reset option <" + left + ">");
                    usage_pr.run();
                }
            }
        }

        app.exit();
    }
}
