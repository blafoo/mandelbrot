package de.blafoo.mandelbrot.javafx;

/// Launcher-Klasse als Workaround für JavaFX-Module-Path-Anforderungen
/// beim Start aus einem klassischen JAR (ohne `--module-path`).
/// Die eigentliche Anwendung erbt von `Application`.
public final class MandelbrotFxLauncher {

    void main(String[] args) {
        MandelbrotFxApp.main(args);
    }
}

