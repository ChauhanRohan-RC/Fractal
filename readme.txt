=================  Fractal Rendering Engine  =================
This is an interactive fractal rendering engine, consisting of Mandelbrot Set and Julia Set

## CONTROLS
-> F: Change Fractal [Mandelbrot Set | Julia Set]
-> S: Change Seed Control Mode [Fixed | Periodic | Mouse]
-> R: Reset Seed
-> C: Change Color Scheme [Light | Dark | Hue]
-> H: Toggle HUD (Overlay text)
-> Ctrl-S: Screenshot

-> +/- : Change max iterations
-> Ctrl +/- : Change divergence distance
-> Shift +/- : Change number of worker threads

-> Up/Down/Left/Right : Translate
-> Ctrl-Up/Down : Zoom
-> Shift-R: Reset View
-> Ctrl-R: Reset All

## COMMANDS
-> seed <complex number> : Set the fractal seed. example: seed -0.8 + 0.156i
-> change fractal : switch to next fractal [Mandelbrot Set | Julia Set]
-> change color : change color scheme [Light | Dark | Hue]
-> change sc : change seed control mode [Fixed | Periodic | Mouse]
-> toggle hud : toggle HUD
-> save : save current frame
-> reset [view | seed | all] : Reset scope
