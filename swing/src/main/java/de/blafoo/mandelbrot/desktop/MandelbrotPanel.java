package de.blafoo.mandelbrot.desktop;

import de.blafoo.mandelbrot.core.*;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Rendering-Panel mit Maus-Interaktion.
 */
public class MandelbrotPanel extends JPanel {

    private double centerX = -0.5;
    private double centerY = 0.0;
    private double zoom = 1.0;
    private int maxIterations = 500;
    private ColorScheme colorScheme = ColorScheme.defaultScheme();

    private @Nullable BufferedImage image;
    private final DesktopRenderer renderer = new DesktopRenderer();

    private @Nullable JLabel coordLabel, zoomLabel, iterLabel;

    private @Nullable Point dragStart;
    private double dragCenterX, dragCenterY;

    public MandelbrotPanel() {
        setBackground(Color.BLACK);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        addMouseWheelListener(this::onMouseWheel);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStart = e.getPoint();
                    dragCenterX = centerX;
                    dragCenterY = centerY;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && dragStart != null) {
                    dragStart = null;
                    startRender();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null && SwingUtilities.isLeftMouseButton(e)) {
                    var dx = e.getX() - dragStart.x;
                    var dy = e.getY() - dragStart.y;
                    var scale = getViewScale();
                    centerX = dragCenterX - dx * scale;
                    centerY = dragCenterY - dy * scale;
                    repaint();
                    updateStatus(e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateStatus(e);
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                startRender();
            }
        });
    }

    private double getViewScale() {
        return currentParams().scale();
    }

    private Complex screenToComplex(int sx, int sy) {
        return currentParams().pixelToComplex(sx, sy);
    }

    private void onMouseWheel(MouseWheelEvent e) {
        var factor = e.getWheelRotation() < 0 ? 1.5 : 1.0 / 1.5;
        // Zentrale Zoom-am-Cursor-Mathematik (RenderParams.withZoomAt).
        var p = currentParams().withZoomAt(e.getX(), e.getY(), factor);
        centerX = p.centerX();
        centerY = p.centerY();
        zoom    = p.zoom();
        startRender();
        updateStatus(e);
    }

    public void resetView() {
        centerX = -0.5;
        centerY = 0.0;
        zoom = 1.0;
        startRender();
    }

    public void setMaxIterations(int maxIter) {
        this.maxIterations = maxIter;
        startRender();
    }

    public void setColorScheme(ColorScheme scheme) {
        this.colorScheme = scheme;
        startRender();
    }

    public void setStatusLabels(JLabel coord, JLabel zoom, JLabel iter) {
        this.coordLabel = coord;
        this.zoomLabel = zoom;
        this.iterLabel = iter;
    }

    private void updateStatus(MouseEvent e) {
        if (coordLabel == null || zoomLabel == null || iterLabel == null) return;
        var pos = screenToComplex(e.getX(), e.getY());
        coordLabel.setText("Re: %.12f  Im: %.12f".formatted(pos.re(), pos.im()));
        zoomLabel.setText("Zoom: %.2fx".formatted(zoom));

        // Java 21+: Pattern Matching for switch + Record Patterns
        var iterText = switch (MandelbrotEngine.computePixel(pos, maxIterations)) {
            case IterationResult.InSet _ -> "∞ (in Menge)";
            case IterationResult.Escaped(int iter, _) -> String.valueOf(iter);
        };
        iterLabel.setText("Iteration: " + iterText);
    }

    private RenderParams currentParams() {
        return new RenderParams(getWidth(), getHeight(), centerX, centerY,
                zoom, maxIterations, colorScheme);
    }

    public void startRender() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        renderer.render(currentParams(), img -> {
            this.image = img;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            if (dragStart != null) {
                var mouse = getMousePosition();
                if (mouse != null) {
                    var dx = mouse.x - dragStart.x;
                    var dy = mouse.y - dragStart.y;
                    g.drawImage(image, dx, dy, null);
                    return;
                }
            }
            g.drawImage(image, 0, 0, null);
        }
    }

    public void saveImage(JFrame parent) {
        if (image == null) return;
        var chooser = new JFileChooser();
        chooser.setSelectedFile(new File("mandelbrot.png"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(image, "PNG", chooser.getSelectedFile());
                JOptionPane.showMessageDialog(parent, "Bild gespeichert!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Fehler: %s".formatted(ex.getMessage()),
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

