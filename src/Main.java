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

        MANDELBROT("Mandelbrot Set",
                "Z",
                new Complex(0, 0)
        ),

        JULIA("Julia Set",
                "C",
                new Complex(0, 0)
        );


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
        OFF("OFF", false),
        PERIODIC("Periodic", true),
        BY_MOUSE("Mouse", true);

        public final String displayName;
        public final boolean supportsPause;

        SeedAnimationMode(String displayName, boolean supportsPause) {
            this.displayName = displayName;
            this.supportsPause = supportsPause;
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
    private static final int THREAD_COUNT_MIN = 1;
    private static final int THREAD_COUNT_MAX = Async.NO_CPU_CORES * 4;
    private static final int THREAD_COUNT_DEFAULT = Async.NO_CPU_CORES;
    private static final int THREAD_COUNT_STEP = 1;

    // Maximum number of iterations
    private static final int ITERATIONS_MIN = 10;
    private static final int ITERATIONS_MAX = 10000;
    private static final int ITERATIONS_DEFAULT = 100;
    private static final int ITERATIONS_STEP = 10;

    // Minimum Distance after which the computation is flagged as diverging
    private static final double DIVERGENCE_DISTANCE_MIN = 2;
    private static final double DIVERGENCE_DISTANCE_MAX = 1000;
    private static final double DIVERGENCE_DISTANCE_DEFAULT = 4;
    private static final double DIVERGENCE_DISTANCE_STEP = 2;

    // Colors
    public static final Color COLOR_ACCENT = new Color(137, 207, 252, 255);
    public static final Color COLOR_ACCENT_HIGHLIGHT = new Color(255, 224, 99, 255);

    public static final boolean SHOW_TITLE = true;
    public static final float TITLE_SIZE = 0.026f;
    public static final Color FG_TITLE = COLOR_ACCENT_HIGHLIGHT;

    public static final boolean SHOW_PAUSED = true;
    public static final float PAUSED_TEXT_SIZE = 0.026f;
    public static final Color FG_PAUSED_TEXT = COLOR_ACCENT_HIGHLIGHT;

    public static final Color BG_STATUS = new Color(30, 30, 30);
    public static final Color FG_STATUS = COLOR_ACCENT;

    public static final boolean SHOW_MAIN_STATUS = true;
    public static final float STATUS_MAIN_TEXT_SIZE = 0.02f;

    public static final boolean SHOW_SEC_STATUS = true;
    public static final float STATUS_SEC_TEXT_SIZE = 0.02f;

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
    private Complex mSeed = fractal.defaultSeed;
    private int mMaxIterations = ITERATIONS_DEFAULT;
    private double mDivergenceDistance = DIVERGENCE_DISTANCE_DEFAULT;
    private int mThreadCount = THREAD_COUNT_DEFAULT;     // number of worker threads

    @NotNull
    private Main.SeedAnimationMode animMode = DEFAULT_ANIMATION_MODE;
    private double mAnimAngle = 0f;
    private double mAnimAngleStep = Math.toRadians(1);
    private boolean mAnimPaused;

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
        return String.format("Max Iters: %d   |   Divg Dist: %.2f   |   Seed (%s): %.3f %+.3fi  ", mMaxIterations, mDivergenceDistance, fractal.seedLabel, mSeed.re, mSeed.img);
    }

    @Nullable
    public String getSecStatusText() {
        return String.format("Threads: %d   |   Animation: %s   |   Colors: %s", mThreadCount, animMode.displayName, colorScheme.displayName);
    }

    @Override
    public void draw() {
        preDraw();

        switch (animMode) {
            case PERIODIC -> {
                if (!mAnimPaused) {
                    mSeed = Complex.polar(0.7885, mAnimAngle);
                    mAnimAngle += mAnimAngleStep;
                    drawFrame();
                } else if (mFrameInvalidated < 2) {
                    drawFrame();
                    mFrameInvalidated++;
                }
            }
            case BY_MOUSE -> {
                if (!mAnimPaused) {
                    mSeed = new Complex(map(mouseX, 0, width, xMin, xMax), map(mouseY, 0, height, yMax, yMin));
                    drawFrame();
                } else if (mFrameInvalidated < 2) {
                    drawFrame();
                    mFrameInvalidated++;
                }
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
            case java.awt.event.KeyEvent.VK_SPACE -> toggleAnimationPaused();

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
                    changeThreadCount(true, true);
                }
            }

            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
                if (event.isShiftDown()) {
                    changeThreadCount(false, true);
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
                    changeDivergenceDistance(true, true);
                } else if (!(event.isShiftDown() || event.isAltDown())) {
                    changeMaxIterations(true, true);
                }
            }

            case java.awt.event.KeyEvent.VK_DEAD_OGONEK, java.awt.event.KeyEvent.VK_MINUS -> {
                if (event.isControlDown()) {
                    changeDivergenceDistance(false, true);
                } else if (!(event.isShiftDown() || event.isAltDown())) {
                    changeMaxIterations(false, true);
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

            ensureYAspectRatio();

            invalidateFrame();
        }

        mousePivot1 = mousePivot2 = null;
    }

    private void onAnimationModeChanged(@Nullable Main.SeedAnimationMode prev, @NotNull Main.SeedAnimationMode cur) {
        println(R.SHELL_ROOT + "Animation Mode: " + cur.displayName);

        mAnimPaused = false;    // reset
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


    protected void onAnimationPauseChanged(boolean paused) {
        println(R.SHELL_ROOT + "Animation " + (paused? "Paused" : "Resumed"));
        invalidateFrame();
    }

    private void setAnimationPaused(boolean paused) {
        if (animMode.supportsPause && mAnimPaused != paused) {
            mAnimPaused = paused;
            onAnimationPauseChanged(paused);
        }
    }

    private void toggleAnimationPaused() {
        setAnimationPaused(!mAnimPaused);
    }

    private void nextFractal() {
        final Fractal prev = fractal;
        fractal = cycleEnum(Fractal.class, fractal);

        if (prev != fractal) {
            onFractalChanged(prev, fractal);
        }
    }

    private void onFractalChanged(@Nullable Fractal prev, @NotNull Fractal cur) {
        println(R.SHELL_ROOT + "Fractal: " + cur.displayName);
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
        println(R.SHELL_ROOT + "Color Scheme: " + cur.displayName);
        invalidateFrame();
    }



    protected void onThreadCountChanged(int prevThreadCount, int threadCount, boolean update) {
        println(R.SHELL_THREADS + "Thread Count: %d -> %d".formatted(prevThreadCount, threadCount));

        if (update) {
            invalidateFrame();
        }
    }

    private void setThreadCount(int threadCount, boolean update) throws IllegalArgumentException {
        if (threadCount < THREAD_COUNT_MIN || threadCount > THREAD_COUNT_MAX)
            throw new IllegalArgumentException(String.format("Thread count must be in range [%d, %d], given: %d", THREAD_COUNT_MIN, THREAD_COUNT_MAX, threadCount));

        final int prev = mThreadCount;
        if (threadCount != prev) {
            mThreadCount = threadCount;

            onThreadCountChanged(prev, threadCount, update);
        }
    }


    private void changeThreadCount(boolean inc, boolean update) {
        final int cur = mThreadCount;
        final int _new = cur + ((inc ? 1 : -1) * THREAD_COUNT_STEP);

        if (_new < THREAD_COUNT_MIN || _new > THREAD_COUNT_MAX)
            return;
        setThreadCount(_new, update);
    }


    protected void onMaxIterationsChanged(int prevMaxIters, int maxIters, boolean update) {
        println(R.SHELL_MAX_ITERATIONS + "Max Iterations: %d -> %d".formatted(prevMaxIters, maxIters));

        if (update) {
            invalidateFrame();
        }
    }


    private void setMaxIterations(int maxIterations, boolean update) throws IllegalArgumentException {
        if (maxIterations < ITERATIONS_MIN || maxIterations > ITERATIONS_MAX)
            throw new IllegalArgumentException(String.format("Maximum Iterations must be in range [%d, %d], given: %d", ITERATIONS_MIN, ITERATIONS_MAX, maxIterations));

        final int prev = mMaxIterations;
        if (maxIterations != prev) {
            mMaxIterations = maxIterations;

            onMaxIterationsChanged(prev, maxIterations, update);
        }
    }

    private void changeMaxIterations(boolean inc, boolean update) {
        final int cur = mMaxIterations;
        final int _new = cur + ((inc ? 1 : -1) * ITERATIONS_STEP);

        if (_new < ITERATIONS_MIN || _new > ITERATIONS_MAX)
            return;

        setMaxIterations(_new, update);
    }



    protected void onDivergenceDistanceChanged(double prevDivergenceDistance, double divergenceDistance, boolean update) {
        println(R.SHELL_DIVERGENCE_DISTANCE + "Divergence Distance: %f -> %f".formatted(prevDivergenceDistance, divergenceDistance));

        if (update) {
            invalidateFrame();
        }
    }

    private void setDivergenceDistance(double divergenceDistance, boolean update) {
        if (divergenceDistance < DIVERGENCE_DISTANCE_MIN || divergenceDistance > DIVERGENCE_DISTANCE_MAX)
            throw new IllegalArgumentException("Divergence Distance must be in range [%f, %f], given: %f".formatted(DIVERGENCE_DISTANCE_MIN, DIVERGENCE_DISTANCE_MAX, divergenceDistance));

        final double prev = mDivergenceDistance;
        if (prev != divergenceDistance) {
            mDivergenceDistance = divergenceDistance;

            onDivergenceDistanceChanged(prev, divergenceDistance, update);
        }
    }


    private void changeDivergenceDistance(boolean inc, boolean update) {
        final double cur = mDivergenceDistance;
        final double _new = cur + ((inc ? 1 : -1) * DIVERGENCE_DISTANCE_STEP);

        if (_new < DIVERGENCE_DISTANCE_MIN || _new > DIVERGENCE_DISTANCE_MAX)
            return;

        setDivergenceDistance(_new, update);
    }



    protected void onSeedChanged(@NotNull Complex prevSeed, @NotNull Complex seed, boolean update) {
        println(R.SHELL_SEED + "Seed: %s -> %s".formatted(prevSeed.toString(), seed.toString()));

        if (update) {
            invalidateFrame();
        }
    }

    public void setSeed(@NotNull Complex seed, boolean stopAnimation, boolean update) {
        if (mSeed.equals(seed))
            return;

        if (stopAnimation) {
            setSeedAnimationMode(SeedAnimationMode.OFF);
        }

        final Complex prev = mSeed;
        mSeed = seed;
        onSeedChanged(prev, seed, update);
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
        println(R.SHELL_ROOT + "HUD " + (drawHud? "ON": "OFF"));
    }

    private void snapshot() {
        String file_name = fractal.displayName + "_" + colorScheme.displayName + "_seed_" + mSeed + ".png";
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
        mSeed = fractal.defaultSeed;
        if (update || animMode == SeedAnimationMode.OFF) {
            invalidateFrame();
        }
    }

    public void resetAll() {
        resetView(false);
        resetSeed(false);
        setThreadCount(THREAD_COUNT_DEFAULT, false);
        setMaxIterations(ITERATIONS_DEFAULT, false);
        setDivergenceDistance(DIVERGENCE_DISTANCE_DEFAULT, false);

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
            case MANDELBROT -> iterateMandelbrot(mSeed, pixelValue, mMaxIterations, mDivergenceDistance);

            // .................  Julia Set (Input space: each pixel is mapped to Z0, C = constant)  ..........................
            case JULIA -> iterateMandelbrot(pixelValue, mSeed, mMaxIterations, mDivergenceDistance);
        };

        return toColor(itr, mMaxIterations);
    }

    private void drawFrame() {
        loadPixels();

        if (mThreadCount > 1) {
            List<Callable<Void>> tasks = createUpdateTasks(mThreadCount);

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
        if (SHOW_MAIN_STATUS) {
            final String mainStatusText = getMainStatusText();
            if (mainStatusText != null && !mainStatusText.isEmpty()) {
                pushStyle();
                textFont(pdSans, getTextSize(STATUS_MAIN_TEXT_SIZE));

                float w = textWidth(mainStatusText) + (h_offset * 2);
                float h = (textAscent() + textDescent()) + (v_offset * 2);

                noStroke();
                fill(BG_STATUS.getRGB());
                rectMode(CORNER);
                rect(h_offset, height - h - v_offset, w, h, 10);

                fill(FG_STATUS.getRGB());
                textAlign(LEFT, BOTTOM);
                text(mainStatusText, h_offset * 2, height - (v_offset * 2));
                popStyle();
            }
        }

        if (SHOW_SEC_STATUS) {
            final String secStatusText = getSecStatusText();
            if (secStatusText != null && !secStatusText.isEmpty()) {
                pushStyle();
                textFont(pdSans, getTextSize(STATUS_MAIN_TEXT_SIZE));

                float w = textWidth(secStatusText) + (h_offset * 2);
                float h = (textAscent() + textDescent()) + (v_offset * 2);

                noStroke();
                fill(BG_STATUS.getRGB());
                rectMode(CORNER);
                rect(width - w - h_offset, height - h - v_offset, w, h, 10);

                fill(FG_STATUS.getRGB());
                textAlign(RIGHT, BOTTOM);
                text(secStatusText, width - (h_offset * 2), height - (v_offset * 2));
                popStyle();
            }
        }

        // Title
        if (SHOW_TITLE) {
            pushStyle();
            textFont(pdSans, getTextSize(TITLE_SIZE));

            final String text = fractal.displayName;

            float w = textWidth(text) + (h_offset * 2);
            float h = (textAscent() + textDescent()) + (v_offset * 2);

            noStroke();
            fill(BG_STATUS.getRGB());
            rectMode(CORNER);
            rect(h_offset, v_offset, w, h, 10);

            fill(FG_TITLE.getRGB());
            textAlign(LEFT, TOP);
            text(text, h_offset * 2, v_offset * 2);
            popStyle();
        }

        // Paused
        if (SHOW_PAUSED && animMode.supportsPause && mAnimPaused) {
            pushStyle();
            textFont(pdSans, getTextSize(PAUSED_TEXT_SIZE));

            final String text = "Paused";

            float w = textWidth(text) + (h_offset * 2);
            float h = (textAscent() + textDescent()) + (v_offset * 2);

            noStroke();
            fill(BG_STATUS.getRGB());
            rectMode(CORNER);
            rect(width - w - h_offset, v_offset, w, h, 10);

            fill(FG_PAUSED_TEXT.getRGB());
            textAlign(RIGHT, TOP);
            text(text, width - (h_offset * 2), v_offset * 2);
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

        println(R.DES_GENERAL_WITH_HELP);
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
            } else if (cmd.startsWith("help")) {
                final Runnable usage_pr =  () -> println(R.SHELL_HELP + "Prints usage information: Usage: help [commands | controls | all]");

                final String left = cmd.substring(4).trim();
                if (left.equals("controls")) {
                    println(R.DES_CONTROLS);
                } else if (left.equals("commands")) {
                    println(R.DES_COMMANDS);
                } else if (left.equals("all") || left.isEmpty()) {
                    usage_pr.run();
                    println("\n" + R.DES_FULL);
                } else {
                    usage_pr.run();
                }
            } else if (cmd.equals("play") || cmd.equals("pause")) {
                if (app.animMode.supportsPause) {
                    app.setAnimationPaused(cmd.equals("pause"));
                } else {
                    System.out.println(R.SHELL_ROOT + "Current animation mode (%s) does not support Play/Pause".formatted(app.animMode.displayName));
                }
            } else if (cmd.equals("hud") || cmd.equals("toggle hud")) {
                app.toggleHud();
            } else if (cmd.equals("color") || cmd.equals("change color") || cmd.equals("color scheme")) {
                app.nextColorScheme();
            } else if (cmd.equals("anim") || cmd.equals("animation") || cmd.equals("change anim") || cmd.equals("sa") ||  cmd.equals("change sa")) {
                app.nextSeedAnimationMode();
            } else if (cmd.equals("fractal") || cmd.equals("change fractal")) {
                app.nextFractal();
            } else if (cmd.equals("save") || cmd.equals("screenshot") || cmd.equals("snapshot")) {
                app.snapshot();
            } else if (cmd.startsWith("seed")) {
                final String left = cmd.substring(4).trim();
                final Runnable usage_pr = () -> println(R.SHELL_SEED + "Current Value: %s | Default: %s\nUsage: seed <complex number>\nExample: seed -0.8 + 0.156i".formatted(app.mSeed, app.fractal.defaultSeed));

                if (left.isEmpty()) {
                    usage_pr.run();
                    continue;
                }

                try {
                    Complex seed = Complex.parse(left);
                    app.setSeed(seed, true, true);
                } catch (NumberFormatException exc) {
                    System.err.println(R.SHELL_SEED + "Failed to parse complex number\n" + exc);
                    usage_pr.run();
                } catch (Throwable t) {
                    System.err.println(R.SHELL_SEED + "Unknown Error\n");
                    t.printStackTrace(System.err);
                    usage_pr.run();
                }
            } else if (cmd.startsWith("itr")) {
                final String left = cmd.substring(3).trim();
                final Runnable usage_pr = () -> println(R.SHELL_MAX_ITERATIONS + String.format("Set Maximum Iterations. Current: %d  |  Default: %d\nUsage: itr <max_iterations>. Should be an integer in range [%d, %d]\nExample: itr 72", app.mMaxIterations, ITERATIONS_DEFAULT, ITERATIONS_MIN, ITERATIONS_MAX));

                if (left.isEmpty()) {
                    usage_pr.run();
                    continue;
                }

                try {
                    final int itr = Integer.parseInt(left);
                    app.setMaxIterations(itr, true);
                }  catch (NumberFormatException nfe) {
                    System.err.println(R.SHELL_MAX_ITERATIONS + "Maximum iterations must be an INTEGER, given: " + left);
                    usage_pr.run();
                } catch (IllegalArgumentException iae) {
                    System.err.println(R.SHELL_MAX_ITERATIONS + iae.getMessage());
                    usage_pr.run();
                } catch (Throwable t) {
                    System.err.println(R.SHELL_MAX_ITERATIONS + "Failed to set Max Iterations");
                    t.printStackTrace(System.err);
                    usage_pr.run();
                }
            } else if (cmd.startsWith("divdist")) {
                final String left = cmd.substring(7).trim();
                final Runnable usage_pr = () -> println(R.SHELL_DIVERGENCE_DISTANCE + String.format("Set Divergence Distance. Current: %f  |  Default: %f\nUsage: divdist <divergence_distance>. Should be a float in range [%f, %f]\nExample: divdist 21.87", app.mDivergenceDistance, DIVERGENCE_DISTANCE_DEFAULT, DIVERGENCE_DISTANCE_MIN, DIVERGENCE_DISTANCE_MAX));

                if (left.isEmpty()) {
                    usage_pr.run();
                    continue;
                }

                try {
                    final double divdist = Double.parseDouble(left);
                    app.setDivergenceDistance(divdist, true);
                }  catch (NumberFormatException nfe) {
                    System.err.println(R.SHELL_DIVERGENCE_DISTANCE + "Divergence Distance must be an integer or a floating point number, given: " + left);
                    usage_pr.run();
                } catch (IllegalArgumentException iae) {
                    System.err.println(R.SHELL_DIVERGENCE_DISTANCE + iae.getMessage());
                    usage_pr.run();
                } catch (Throwable t) {
                    System.err.println(R.SHELL_DIVERGENCE_DISTANCE + "Failed to set Divergence Distance");
                    t.printStackTrace(System.err);
                    usage_pr.run();
                }
            } else if (cmd.startsWith("threads")) {
                final String left = cmd.substring(7).trim();
                final Runnable usage_pr = () -> println(R.SHELL_THREADS + String.format("Set the number of Threads. Current: %d  |  Default: %d\nUsage: threads <count>. Should be an integer in range [%d, %d]\nExample: threads 2", app.mThreadCount, THREAD_COUNT_DEFAULT, THREAD_COUNT_MIN, THREAD_COUNT_MAX));

                if (left.isEmpty()) {
                    usage_pr.run();
                    continue;
                }

                try {
                    final int threads = Integer.parseInt(left);
                    app.setThreadCount(threads, true);
                }  catch (NumberFormatException nfe) {
                    System.err.println(R.SHELL_THREADS + "Thread count must be an INTEGER, given: " + left);
                    usage_pr.run();
                } catch (IllegalArgumentException iae) {
                    System.err.println(R.SHELL_THREADS + iae.getMessage());
                    usage_pr.run();
                } catch (Throwable t) {
                    System.err.println(R.SHELL_THREADS + "Failed to set thread count");
                    t.printStackTrace(System.err);
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
