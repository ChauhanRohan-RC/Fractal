import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class R {

    public static final boolean FROZEN = false;         // TODO: set true before packaging


    // Dir structure
    public static final Path DIR_MAIN = (FROZEN? Path.of("app") : Path.of("")).toAbsolutePath();
    public static final Path DIR_RES = DIR_MAIN.resolve("res");
    public static final Path DIR_IMAGE = DIR_RES.resolve("image");
    public static final Path DIR_FONT = DIR_RES.resolve("font");

    // Resources
    @Nullable
    public static final Path MANDELBROT_ICON = DIR_IMAGE.resolve("mandelbrot_icon.png");

//    @Nullable
//    public static final Path IMAGE_BG = DIR_IMAGE.resolve("deep_space_2.jpg");

    @Nullable
    public static final Path IMAGE_BG = null;

    public static final Path FONT_PD_SANS_REGULAR = DIR_FONT.resolve("product_sans_regular.ttf");
    public static final Path FONT_PD_SANS_MEDIUM = DIR_FONT.resolve("product_sans_medium.ttf");


    public static final String TITLE_MANDELBROT = "Fractal Simulation (Mandelbrot and Julia Sets)";

    // Shell

    private static final String SHELL_ROOT_NS = "fractal";       // Name Space

    @NotNull
    public static String shellPath(@Nullable String child) {
        return (child == null || child.isEmpty()? SHELL_ROOT_NS: SHELL_ROOT_NS + "\\" + child) + "> ";
    }

    public static final String SHELL_ROOT = shellPath(null);
    public static final String SHELL_HELP = shellPath("help");
    public static final String SHELL_SEED = shellPath("seed");
    public static final String SHELL_MAX_ITERATIONS = shellPath("max-itr");
    public static final String SHELL_DIVERGENCE_DISTANCE = shellPath("div-dist");
    public static final String SHELL_THREADS = shellPath("thread");

//    public static final String SHELL_SCRAMBLE = shellPath("scramble");
//    public static final String SHELL_SOLVER = shellPath("solve");
//    public static final String SHELL_MOVE = shellPath("move");


    // Instructions

    public static final String DES_GENERAL =
            """
            =================  Fractal Rendering Engine  =================
            This is an interactive fractal rendering engine, consisting of Mandelbrot Set and Julia Set
            """;

    public static final String DES_GENERAL_WITH_HELP = DES_GENERAL + "\n\t\tType \"help\" for usage information\n";

    public static final String DES_CONTROLS =
            """
            -> F: Change Fractal [Mandelbrot Set | Julia Set]
            -> S: Change Seed Animation Mode [Fixed | Periodic | Mouse]
            -> R: Reset Seed
            -> C: Change Color Scheme [Light | Dark | Hue]
            -> H: Toggle HUD (Overlay text)
            -> SPACE: Play/Pause Seed Animation
            -> Ctrl-S: Screenshot
            
            -> +/- : Change max iterations
            -> Ctrl +/- : Change divergence distance
            -> Shift +/- : Change number of worker threads
            
            -> Up/Down/Left/Right : Translate
            -> Ctrl-Up/Down : Zoom
            -> Shift-R: Reset View
            -> Ctrl-R: Reset All
            """;

    public static final String DES_COMMANDS =
            """
            -> help [controls | commands | all] : Usage information
            
            -> fractal : switch to next fractal [Mandelbrot Set | Julia Set]
            -> color : next color scheme [Light | Dark | Hue]
            -> anim : next seed animation mode [Fixed | Periodic | Mouse]
            -> play/pause : Play or Pause seed animation
            
            -> seed <complex_number> : Set the fractal seed. Example: seed -0.8 + 0.156i
            -> itr <max_iterations> : Set maximum iterations. Example: itr 73
            -> divdist <divergence_distance> : Set divergence distance. Example: divdist 24.82
            -> threads <count> : Set the number of worker threads
            
            -> reset [view | seed | all] : Reset scope
            -> toggle hud : toggle HUD
            -> save : save current frame
            """;

    public static final String DES_FULL = DES_GENERAL + "\n## CONTROLS\n" + DES_CONTROLS + "\n## COMMANDS\n" + DES_COMMANDS;

    // Readme

    public static boolean createReadme(@NotNull String instructions) {
        try (PrintWriter w = new PrintWriter("readme.txt", StandardCharsets.UTF_8)) {
            w.print(instructions);
            w.flush();
            return true;
        } catch (Throwable exc) {
            exc.printStackTrace();
        }

        return false;
    }

    public static boolean createDescriptionReadme() {
        return R.createReadme(DES_FULL);
    }




}
