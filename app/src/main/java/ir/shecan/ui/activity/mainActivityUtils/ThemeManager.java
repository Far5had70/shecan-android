package ir.shecan.ui.activity.mainActivityUtils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import ir.shecan.data.modelDto.AppConfig;
import ir.shecan.data.storage.AppStorage;

/**
 * Helper class to manage App theme based on AppConfig.
 */
public class ThemeManager {

    private final Context context;
    private final AppStorage appStorage;
    private int currentMode = AppCompatDelegate.MODE_NIGHT_NO;

    public ThemeManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.appStorage = new AppStorage(this.context);
        loadTheme();
    }

    private void loadTheme() {
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if (appConfig != null) {
            currentMode = appConfig.getMode();
            AppCompatDelegate.setDefaultNightMode(currentMode);
        } else {
            appStorage.saveAppConfig(new AppConfig(currentMode));
        }
    }

    /**
     * Apply theme (used on recreate or startup)
     */
    public void applyTheme() {
        loadTheme();
    }

    /**
     * Call this in Activity's onResume to check if theme has changed.
     * Returns true if recreate is needed
     */
    public boolean handleOnResume() {
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if (appConfig != null) {
            int mode = appConfig.getMode();
            if (currentMode != mode) {
                currentMode = mode;
                return true; // needs recreate
            }
        }
        return false;
    }
}
