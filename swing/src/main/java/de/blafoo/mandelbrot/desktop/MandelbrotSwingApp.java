package de.blafoo.mandelbrot.desktop;

import de.blafoo.mandelbrot.core.ColorScheme;

import javax.swing.*;
import java.awt.*;

public class MandelbrotSwingApp {

    void main() {
        SwingUtilities.invokeLater(this::createAndShowUI);
    }

    private void createAndShowUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception _) { }

        var frame = new JFrame("Mandelbrot Viewer · Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        var mandelbrotPanel = new MandelbrotPanel();

        var toolBar = createToolBar(mandelbrotPanel, frame);

        var statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
        statusBar.setBackground(new Color(45, 45, 45));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 80)));

        var coordLabel = styledLabel("Re: 0.0  Im: 0.0");
        var zoomLabel = styledLabel("Zoom: 1.0x");
        var iterLabel = styledLabel("Iteration: 0");

        statusBar.add(coordLabel);
        statusBar.add(zoomLabel);
        statusBar.add(iterLabel);

        mandelbrotPanel.setStatusLabels(coordLabel, zoomLabel, iterLabel);

        frame.setLayout(new BorderLayout());
        frame.add(toolBar, BorderLayout.NORTH);
        frame.add(mandelbrotPanel, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);

        frame.setVisible(true);
        mandelbrotPanel.startRender();
    }

    private JToolBar createToolBar(MandelbrotPanel panel, JFrame frame) {
        var toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(45, 45, 45));

        var resetButton = new JButton("Reset");
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(_ -> panel.resetView());
        toolBar.add(resetButton);
        toolBar.addSeparator();

        toolBar.add(styledLabel("Iterationen: "));
        var iterSpinner = new JSpinner(new SpinnerNumberModel(500, 50, 10000, 50));
        iterSpinner.setMaximumSize(new Dimension(100, 30));
        iterSpinner.addChangeListener(_ -> panel.setMaxIterations((int) iterSpinner.getValue()));
        toolBar.add(iterSpinner);
        toolBar.addSeparator();

        // Sequenced Collections (Java 21+): List<ColorScheme> mit Stream-Map auf Namen
        var schemes = ColorScheme.all();
        var schemeNames = schemes.stream()
                .map(ColorScheme::displayName)
                .toArray(String[]::new);

        toolBar.add(styledLabel("Farbschema: "));
        var colorCombo = new JComboBox<>(schemeNames);
        colorCombo.setMaximumSize(new Dimension(130, 30));
        colorCombo.addActionListener(_ -> panel.setColorScheme(schemes.get(colorCombo.getSelectedIndex())));
        toolBar.add(colorCombo);
        toolBar.addSeparator();

        var saveButton = new JButton("Bild speichern");
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(_ -> panel.saveImage(frame));
        toolBar.add(saveButton);

        return toolBar;
    }

    private static JLabel styledLabel(String text) {
        var label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        return label;
    }
}
