#version 330 core
in vec2 vUv;
out vec4 fragColor;

uniform vec2  uResolution;   // Pixel-Maße
uniform vec2  uCenter;       // Komplexer Mittelpunkt (re, im)
uniform float uZoom;         // Zoom-Faktor
uniform int   uMaxIter;      // Maximale Iterationen
uniform int   uColorScheme;  // 0..4 (Classic, Fire, Ocean, Neon, Grayscale)

const float PI = 3.14159265358979;

vec3 colorFor(float t) {
    if (uColorScheme == 0) {
        // Classic
        float a = t * 5.0 * PI;
        return vec3(0.5 + 0.5*sin(a),
                    0.5 + 0.5*sin(a + 2.094),
                    0.5 + 0.5*sin(a + 4.189));
    } else if (uColorScheme == 1) {
        // Fire
        if (t < 0.33)        return vec3(t/0.33, 0.0, 0.0);
        else if (t < 0.66)   return vec3(1.0, (t-0.33)/0.33, 0.0);
        else                 return vec3(1.0, 1.0, (t-0.66)/0.34);
    } else if (uColorScheme == 2) {
        // Ocean
        float a = t * 4.0 * PI;
        float r = 0.314 * (1.0 + sin(a + 4.0));
        float g = 0.392 + 0.608 * pow(sin(a*0.5), 2.0);
        float b = 0.588 + 0.412 * sin(a);
        return clamp(vec3(r,g,b), 0.0, 1.0);
    } else if (uColorScheme == 3) {
        // Neon
        float a = t * 6.0 * PI;
        return clamp(vec3(0.5 + 0.5*sin(a),
                          0.5 + 0.5*sin(a + 2.5),
                          0.784 + 0.216*sin(a + 5.0)), 0.0, 1.0);
    } else {
        // Grayscale
        float v = sqrt(t);
        return vec3(v, v, v);
    }
}

void main() {
    // Pixel → komplexe Ebene (gleiche Logik wie RenderParams.pixelToComplex)
    float size = min(uResolution.x, uResolution.y);
    float scale = 3.0 / (size * uZoom);
    vec2 pixel = vUv * uResolution;
    vec2 c = uCenter + (pixel - uResolution * 0.5) * vec2(scale, scale);

    vec2 z = vec2(0.0);
    int iter = 0;
    // Bailout 256 für Smooth Coloring
    for (int i = 0; i < 100000; i++) {
        if (i >= uMaxIter || dot(z, z) > 256.0) break;
        z = vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
        iter++;
    }

    if (iter == uMaxIter) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        // Smooth: iter + 1 - log2(log(|z|))
        float logZn = log(dot(z, z)) * 0.5;        // = log(|z|)
        float smoothIter = float(iter) + 1.0 - log(logZn) / log(2.0);
        float t = clamp(smoothIter / float(uMaxIter), 0.0, 1.0);
        fragColor = vec4(colorFor(t), 1.0);
    }
}

