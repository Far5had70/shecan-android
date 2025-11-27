package ir.shecan.storage;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

public class AppStorage {

    private static final String Token_KEY = "TOKEN_MODEL";
    private static final String App_Config_KEY = "APP_CONFIG_MODEL";
    private static final String Issue_KEY = "Issue_MODEL";

    private final GenericPreferenceManager pref;

    public AppStorage(Context context) {
        this.pref = GenericPreferenceManager.getInstance(context);
    }

    // ---------------- User Model ----------------
    public <T> void saveToken(T token) {
        pref.saveModel(Token_KEY, token);
    }

    public <T> T getToken(Class<T> clazz) {
        return pref.getModel(Token_KEY, clazz);
    }

    public void removeToken() {
        pref.remove(Token_KEY);
    }

    // ---------------- App Config ----------------
    public <T> void saveAppConfig(T appConfig) {
        pref.saveModel(App_Config_KEY, appConfig);
    }

    public <T> T getAppConfig(Class<T> clazz) {
        return pref.getModel(App_Config_KEY, clazz);
    }

    public void removeAppConfig() {
        pref.remove(App_Config_KEY);
    }
    // -----------------------------------------------

    // ---------------- Configs ----------------
    public <T> void saveIssues(T issue) {
        pref.saveModel(Issue_KEY, issue);
    }

    public <T> T getIssue(Class<T> clazz) {
        return pref.getModel(Issue_KEY, clazz);
    }

    public void removeIssue() {
        pref.remove(Issue_KEY);
    }
    // -----------------------------------------------


    // پاک کردن همه چیز
    public void clearAll() {
        pref.clearAll();
    }
}

