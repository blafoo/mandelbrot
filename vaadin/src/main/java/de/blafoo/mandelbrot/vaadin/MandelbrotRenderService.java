package de.blafoo.mandelbrot.vaadin;

import de.blafoo.mandelbrot.core.MandelbrotRenderer;
import de.blafoo.mandelbrot.core.PngWriter;
import de.blafoo.mandelbrot.core.RenderParams;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// Service, der ein PNG-Bild der Mandelbrot-Menge parallel rendert.
///
/// Nutzt den geteilten `MandelbrotRenderer` aus dem Core-Modul (paralleles
/// Rendering über Virtual Threads) und den geteilten `PngWriter` (PNG-Encoding).
@Service
public class MandelbrotRenderService {

    private final ExecutorService renderPool = Executors.newVirtualThreadPerTaskExecutor();
    private final MandelbrotRenderer renderer = new MandelbrotRenderer(renderPool);

    public byte[] renderPng(RenderParams params) {
        int[] pixels = renderer.renderBlocking(params);
        return PngWriter.toBytes(pixels, params.width(), params.height());
    }

    @PreDestroy
    void shutdown() {
        renderPool.shutdownNow();
    }
}

