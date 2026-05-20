package ir.shecan.core.billing;

import android.content.Context;
import android.content.SharedPreferences;

public class BillingPaymentReturnState {

    private static final String PREFS_NAME = "billing_payment_return";
    private static final String KEY_WAITING_FOR_BROWSER = "waiting_for_browser";
    private static final String KEY_LEFT_FOR_BROWSER = "left_for_browser";

    private final SharedPreferences preferences;

    public BillingPaymentReturnState(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void markBrowserOpening() {
        preferences.edit()
                .putBoolean(KEY_WAITING_FOR_BROWSER, true)
                .putBoolean(KEY_LEFT_FOR_BROWSER, false)
                .apply();
    }

    public void markBrowserLeft() {
        if (!isWaitingForBrowser()) return;

        preferences.edit()
                .putBoolean(KEY_LEFT_FOR_BROWSER, true)
                .apply();
    }

    public boolean shouldRestartAfterReturn() {
        return preferences.getBoolean(KEY_WAITING_FOR_BROWSER, false)
                && preferences.getBoolean(KEY_LEFT_FOR_BROWSER, false);
    }

    public boolean isWaitingForBrowser() {
        return preferences.getBoolean(KEY_WAITING_FOR_BROWSER, false);
    }

    public void clear() {
        preferences.edit()
                .remove(KEY_WAITING_FOR_BROWSER)
                .remove(KEY_LEFT_FOR_BROWSER)
                .apply();
    }
}
