package de.blafoo.mandelbrot.desktop;
import de.blafoo.mandelbrot.core.MandelbrotEngine;
import de.blafoo.mandelbrot.core.RenderParams;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
/**
 * Desktop-spezifischer Renderer mit Virtual Threads.
 */
public class DesktopRenderer {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile long currentTaskId = 0;

    public void render(RenderParams params, Consumer<BufferedImage> onComplete) {
        var taskId = ++currentTaskId;
        executor.submit(() -> {
            var img = new BufferedImage(params.width(), params.height(), BufferedImage.TYPE_INT_ARGB);
            var pixels = new int[params.width() * params.height()];
            var palette = params.colorScheme().generatePalette(params.maxIterations());
            var stripCount = Runtime.getRuntime().availableProcessors() * 2;
            var rowsPerStrip = Math.ceilDiv(params.height(), stripCount);
            var threads = new Thread[stripCount];
            for (int i = 0; i < stripCount; i++) {
                var startY = i * rowsPerStrip;
                var endY = Math.min(startY + rowsPerStrip, params.height());
                threads[i] = Thread.startVirtualThread(() -> {
                    if (taskId != currentTaskId) return;
                    MandelbrotEngine.renderStrip(pixels, params, palette, startY, endY);
                });
            }
            for (var thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (taskId == currentTaskId) {
                img.setRGB(0, 0, params.width(), params.height(), pixels, 0, params.width());
                SwingUtilities.invokeLater(() -> onComplete.accept(img));
            }
        });
    }
}