package ir.shecan.storage;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

public class AppStorage {

    private static final String Token_KEY = "TOKEN_MODEL";

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

    // پاک کردن همه چیز
    public void clearAll() {
        pref.clearAll();
    }
}

