package de.blafoo.mandelbrot.opengl;

import de.blafoo.mandelbrot.core.ColorScheme;
import de.blafoo.mandelbrot.core.RenderParams;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/// LWJGL/OpenGL 3.3 Mandelbrot-Viewer.
/// Die gesamte Berechnung läuft auf der GPU im Fragment-Shader –
/// dadurch sind Echtzeit-Zoom und -Pan auch bei hohen Iterationszahlen möglich.
public final class MandelbrotGLApp {

    private long window;
    private int program;
    private int vao;

    // View-State
    private double centerX = RenderParams.DEFAULT_CENTER_X, centerY = RenderParams.DEFAULT_CENTER_Y;
    private double zoom = RenderParams.DEFAULT_ZOOM;
    private int maxIter = 500;
    private int schemeIndex = 0;

    // Mauszustand
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private double dragStartCenterX, dragStartCenterY;
    private int fbWidth = 1000, fbHeight = 750;

    // Uniform-Locations
    private int locResolution, locCenter, locZoom, locMaxIter, locScheme;

    void main() {
        new MandelbrotGLApp().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(fbWidth, fbHeight, "Mandelbrot Viewer · OpenGL", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Fenster konnte nicht erstellt werden");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        installCallbacks();
        program = buildShaderProgram();
        vao = buildFullscreenQuad();

        locResolution = glGetUniformLocation(program, "uResolution");
        locCenter     = glGetUniformLocation(program, "uCenter");
        locZoom       = glGetUniformLocation(program, "uZoom");
        locMaxIter    = glGetUniformLocation(program, "uMaxIter");
        locScheme     = glGetUniformLocation(program, "uColorScheme");

        // Initiale Framebuffer-Größe
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0); fbHeight = h.get(0);
        }
        updateTitle();
    }

    private void installCallbacks() {
        glfwSetFramebufferSizeCallback(window, (_, w, h) -> {
            fbWidth = w; fbHeight = h;
            glViewport(0, 0, w, h);
        });

        glfwSetMouseButtonCallback(window, (_, button, action, _) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
                        var x = stack.mallocDouble(1);
                        var y = stack.mallocDouble(1);
                        glfwGetCursorPos(window, x, y);
                        dragStartX = x.get(0);
                        dragStartY = y.get(0);
                        dragStartCenterX = centerX;
                        dragStartCenterY = centerY;
                    }
                }
            }
        });

        glfwSetCursorPosCallback(window, (_, x, y) -> {
            if (dragging) {
                double scale = currentScale();
                centerX = dragStartCenterX - (x - dragStartX) * scale;
                centerY = dragStartCenterY + (y - dragStartY) * scale;
            }
        });

        glfwSetScrollCallback(window, (_, _, yoff) -> {
            try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
                var x = stack.mallocDouble(1);
                var y = stack.mallocDouble(1);
                glfwGetCursorPos(window, x, y);
                double mx = x.get(0), my = y.get(0);
                double factor = yoff > 0 ? 1.2 : 1 / 1.2;
                double scale = currentScale();
                double mouseRe = centerX + (mx - fbWidth / 2.0) * scale;
                double mouseIm = centerY - (my - fbHeight / 2.0) * scale;
                zoom *= factor;
                double newScale = currentScale();
                centerX = mouseRe - (mx - fbWidth / 2.0) * newScale;
                centerY = mouseIm + (my - fbHeight / 2.0) * newScale;
                updateTitle();
            }
        });

        glfwSetKeyCallback(window, (_, key, _, action, _) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_R -> { centerX = RenderParams.DEFAULT_CENTER_X; centerY = RenderParams.DEFAULT_CENTER_Y; zoom = RenderParams.DEFAULT_ZOOM; updateTitle(); }
                case GLFW_KEY_C -> {
                    schemeIndex = (schemeIndex + 1) % ColorScheme.all().size();
                    updateTitle();
                }
                case GLFW_KEY_KP_ADD, GLFW_KEY_EQUAL -> { maxIter = Math.min(10000, maxIter + 100); updateTitle(); }
                case GLFW_KEY_KP_SUBTRACT, GLFW_KEY_MINUS -> { maxIter = Math.max(50, maxIter - 100); updateTitle(); }
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            }
        });
    }

    private double currentScale() {
        double size = Math.min(fbWidth, fbHeight);
        return 3.0 / (size * zoom);
    }

    private void updateTitle() {
        var schemeName = ColorScheme.all().get(schemeIndex).displayName();
        glfwSetWindowTitle(window,
                "Mandelbrot (OpenGL) – Zoom %.2fx · Iter %d · Schema: %s · [R]eset [C]olor [+/-]Iter [Esc]"
                        .formatted(zoom, maxIter, schemeName));
    }

    private int buildShaderProgram() {
        int vs = compile(GL_VERTEX_SHADER,   loadResource("/shaders/mandelbrot.vert"));
        int fs = compile(GL_FRAGMENT_SHADER, loadResource("/shaders/mandelbrot.frag"));
        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Programm-Link fehlgeschlagen: " + glGetProgramInfoLog(prog));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
        return prog;
    }

    private int compile(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader-Compile fehlgeschlagen: " + glGetShaderInfoLog(s));
        }
        return s;
    }

    private int buildFullscreenQuad() {
        float[] verts = {
                -1f, -1f,  1f, -1f, -1f,  1f,
                 1f, -1f,  1f,  1f, -1f,  1f
        };
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glBindVertexArray(0);
        return vao;
    }

    private void loop() {
        glClearColor(0, 0, 0, 1);
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            glUseProgram(program);
            glUniform2f(locResolution, (float) fbWidth, (float) fbHeight);
            glUniform2f(locCenter,    (float) centerX, (float) centerY);
            glUniform1f(locZoom,      (float) zoom);
            glUniform1i(locMaxIter,   maxIter);
            glUniform1i(locScheme,    schemeIndex);

            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 6);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    private static String loadResource(String path) {
        try (var in = MandelbrotGLApp.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Ressource nicht gefunden: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

