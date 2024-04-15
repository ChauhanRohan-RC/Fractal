=================  Fractal Rendering Engine  =================
This is an interactive fractal rendering engine, consisting of Mandelbrot Set and Julia Set

## CONTROLS
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

## COMMANDS
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
