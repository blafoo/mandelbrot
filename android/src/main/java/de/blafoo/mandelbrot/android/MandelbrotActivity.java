package de.blafoo.mandelbrot.android;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jspecify.annotations.Nullable;

import java.util.List;

import de.blafoo.mandelbrot.core.ColorScheme;

/// Android-Hauptaktivität für den Mandelbrot Viewer.
///
/// Nutzt den plattformunabhängigen Core (Records, Sealed Types, Sequenced Collections).
public class MandelbrotActivity extends AppCompatActivity {

    private MandelbrotView mandelbrotView;
    private TextView statusText;
    private int currentSchemeIndex = 0;
    private final List<ColorScheme> schemes = ColorScheme.all();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mandelbrot Viewer");
        }

        mandelbrotView = findViewById(R.id.mandelbrot_view);
        statusText = findViewById(R.id.status_text);

        mandelbrotView.setOnStatusUpdateListener((re, im, zoom, iterations) -> {
            String iterText = iterations < 0 ? "∞ (in Menge)" : String.valueOf(iterations);
            statusText.setText(String.format(
                    "Re: %.8f  Im: %.8f | Zoom: %.1fx | Iter: %s",
                    re, im, zoom, iterText
            ));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menü-Resource laden – sorgt für korrekte showAsAction-Behandlung
        // und garantiert das Overflow-Icon (3-Punkte) auf modernen Themes.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.action_reset) {
            mandelbrotView.resetView();
            return true;
        } else if (id == R.id.action_cycle_scheme) {
            currentSchemeIndex = (currentSchemeIndex + 1) % schemes.size();
            var scheme = schemes.get(currentSchemeIndex);
            mandelbrotView.setColorScheme(scheme);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(scheme.displayName());
            }
            return true;
        } else if (id == R.id.action_increase_iterations) {
            mandelbrotView.adjustIterations(100);
            return true;
        } else if (id == R.id.action_decrease_iterations) {
            mandelbrotView.adjustIterations(-100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
