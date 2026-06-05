package de.blafoo.mandelbrot.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;

/// PNG-Encoder für ARGB-Pixel-Arrays.
///
/// **JVM-only** – verwendet `java.awt.image.BufferedImage` und
/// `javax.imageio.ImageIO`, daher **nicht** auf Android nutzbar.
///
/// Wird von Desktop, JavaFX, OpenGL, Kotlin, Spring Boot und Vaadin gemeinsam
/// genutzt, damit das Speichern als PNG nur an einer Stelle implementiert ist.
public final class PngWriter {

    private PngWriter() {}

    /// Encodiert das ARGB-Pixel-Array in einen PNG-Byte-Array.
    public static byte[] toBytes(int[] argbPixels, int width, int height) {
        var out = new ByteArrayOutputStream(64 * 1024);
        write(argbPixels, width, height, out);
        return out.toByteArray();
    }

    /// Schreibt das ARGB-Pixel-Array als PNG in den gegebenen [OutputStream].
    public static void write(int[] argbPixels, int width, int height, OutputStream out) {
        if (argbPixels.length != width * height) {
            throw new IllegalArgumentException(
                    "Pixel-Array hat falsche Länge: %d, erwartet %d"
                            .formatted(argbPixels.length, width * height));
        }
        var img  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        System.arraycopy(argbPixels, 0, data, 0, argbPixels.length);
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Speichert das ARGB-Pixel-Array als PNG-Datei.
    public static void writeFile(int[] argbPixels, int width, int height, File file) {
        try (var os = Files.newOutputStream(file.toPath())) {
            write(argbPixels, width, height, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}

