[![Java CI with Maven](https://github.com/blafoo/mandelbrot/actions/workflows/maven.yml/badge.svg)](https://github.com/blafoo/mandelbrot/actions/workflows/maven.yml)

# Apfelmännchen aka Mandelbrot Set

My own little application to visualize Apfelmännchen (aka the Mandelbrot set) very, very fast. Faster than any other application!!1!

## Background

The **Mandelbrot set** is the set of complex numbers *c* for which the iteration

> z_(n+1) = z_n² + c,  z_0 = 0

does **not** diverge to infinity. Named after mathematician Benoît B. Mandelbrot (1924–2010), it is one of the most famous examples of a fractal – a geometric shape exhibiting self-similarity at every scale.

### Key Properties

* **Infinite detail** – zooming into the boundary reveals ever more intricate structures that resemble the whole set.
* **Connected** – the set is a single connected component (proven by Douady & Hubbard, 1982).
* **Cardioid & Bulbs** – the main body is a cardioid; attached period-2 bulb, period-3 bulb, etc. produce the characteristic "Apfelmännchen" silhouette.
* **Smooth Coloring** – points outside the set are coloured based on a continuous (smooth) iteration count, producing the vivid gradient images.

### Optimisations Used in This Project

| Technique | Effect |
|-----------|--------|
| Cardioid / Period-2 Bulb test | Skips the two largest in-set regions immediately |
| Periodicity detection | Detects orbits trapped in cycles – speeds up deep in-set areas by 2–5× |
| Smooth iteration count | Avoids colour banding via continuous escape-time colouring |
| Parallel strip rendering | Splits the image into horizontal strips processed by Virtual Threads |

## Project Structure

Multi-module Maven project (Java 25) with a shared **core** library and multiple UI front-ends:

```
mandelbrot-viewer (parent pom)
├── core          – Engine, renderer, colour schemes, PNG writer
├── swing         – Desktop GUI (Swing)
├── javafx        – Desktop GUI (JavaFX)
├── opengl        – Real-time GPU viewer (LWJGL / OpenGL 3.3 fragment shader)
├── springboot    – Web application (Spring Boot 4, REST API + HTML/JS frontend)
├── vaadin        – Web application (Vaadin Flow)
└── android       – Mobile app (Android, Gradle build)
```

## Modules

### Core

Platform-independent library containing:

* `Complex` – immutable complex-number record
* `MandelbrotEngine` – optimised iteration with cardioid test, periodicity detection, smooth colouring
* `MandelbrotRenderer` – parallel CPU rendering via Virtual Threads
* `ColorScheme` – sealed interface with five schemes (Classic, Fire, Ocean, Neon, Grayscale)
* `RenderParams` – immutable parameter record with zoom/pan/pixel-to-complex helpers
* `PngWriter` – PNG encoding for JVM targets

### Swing

Classic Java Swing desktop application with mouse-driven zoom (scroll wheel), pan (drag), colour scheme selection, and image export.

### JavaFX

JavaFX desktop viewer with progressive rendering (coarse preview + full render), `PixelBuffer`-based display, and integrated save dialog.

### OpenGL

Real-time viewer using **LWJGL** and an OpenGL 3.3 fragment shader. The entire Mandelbrot computation runs on the GPU – enabling smooth pan & zoom at any iteration depth.

### Spring Boot

Spring Boot 4 web application providing:

* REST endpoint `/api/mandelbrot.png` with query parameters (center, zoom, iterations, scheme, download)
* Interactive HTML/JS frontend with drag-pan, scroll-zoom, and save functionality

### Vaadin

Vaadin Flow web application with server-side rendering and browser-based interaction (wheel-zoom, drag-pan). Uses `DownloadHandler` for image export.

### Android

Native Android app with touch-based pinch-to-zoom and drag-to-pan, rendering on background threads via the shared core engine.

## Building & Running

```bash
# Build all modules (default profile)
mvn clean package

# Run individual modules
mvn -pl swing exec:java -Pswing
mvn -pl javafx javafx:run -Pjavafx
mvn -pl opengl exec:java -Popengl
mvn -pl springboot spring-boot:run -Pspringboot
mvn -pl vaadin spring-boot:run -Pvaadin

# Android (requires Android SDK)
cd android && ./gradlew installDebug
```

## Example Images

###### Fire
![Fire colour scheme](https://github.com/blafoo/mandelbrot/blob/main/images/mandelbrot_-0%2C237953_-0%2C826216_z35%2C53x.png?raw=true)

###### Ocean
![Ocean colour scheme](https://github.com/blafoo/mandelbrot/blob/main/images/mandelbrot_0%2C373753_0%2C596020_z4814%2C82x.png?raw=true)

## License

This project is provided for educational and personal use. Enjoy!
