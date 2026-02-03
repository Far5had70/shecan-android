package ir.shecan.data.api;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.shecan.data.modelDto.EmptyResponse;

public class APIManager {

    private static APIManager instance;
    private final RequestQueue requestQueue;

    private final Map<String, Object> cache = new HashMap<>();

    private String cookie = "";
    private String authToken = "";
    private String apiKey = "";

    private APIManager(Context context) {
        requestQueue = Volley.newRequestQueue(
                context,
                new HurlStack() {
                    @Override
                    protected HttpURLConnection createConnection(URL url) throws IOException {
                        HttpURLConnection conn = super.createConnection(url);
                        conn.setInstanceFollowRedirects(false); // 👈 KEY LINE
                        return conn;
                    }
                }
        );
    }

    public static synchronized APIManager getInstance(Context context) {
        if (instance == null) instance = new APIManager(context);
        return instance;
    }

    // -------------------------------------
    // Headers
    // -------------------------------------

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();

        if (!authToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + authToken);
        }

        if (!cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }

        if (!apiKey.isEmpty()) {
            headers.put("x-redmine-api-key", apiKey);
        }

        return headers;
    }

    // -------------------------------------
    // Object Request
    // -------------------------------------

    public <T, P> void requestObject(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {

        requestObjectInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                callback,
                clazz,
                false // 👈 retry نشده
        );
    }

    public <T, P> void requestList(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz
    ) {

        requestListInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                isPublicApi,
                callback,
                clazz,
                false // 👈 هنوز retry نشده
        );
    }

    private <T, P> void requestObjectInternal(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz,
            boolean retried
    ) {

        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((T) cache.get(cacheKey), true);
                return;
            }

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            JSONObject payload = payloadModel != null
                    ? new JSONObject(gson.toJson(payloadModel))
                    : null;

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {
                        try {

                            if (response == null || response.toString().equals("null")) {
                                if (clazz.equals(EmptyResponse.class)) {
                                    T model = clazz.getDeclaredConstructor().newInstance();
                                    callback.onSuccess(model, false);
                                    return;
                                }
                                response = new JSONObject();
                            }

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) data = response;

                            T model = gson.fromJson(data.toString(), clazz);
                            cache.put(cacheKey, model);
                            callback.onSuccess(model, false);

                        } catch (Exception e) {
                            callback.onError(-2, e.getMessage());
                        }
                    },
                    error -> {

                        NetworkResponse nr = error.networkResponse;

                        // 🔥 HANDLE 307 HERE
                        if (nr != null && nr.statusCode == 307 && !retried) {

                            if (nr.headers != null && nr.headers.containsKey("Set-Cookie")) {
                                cookie = mergeCookies(cookie, nr.headers.get("Set-Cookie"));
                            }

                            // 🔁 retry once
                            requestObjectInternal(
                                    cacheKey,
                                    payloadModel,
                                    url,
                                    method,
                                    useCache,
                                    callback,
                                    clazz,
                                    true
                            );
                            return;
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseVolleyError(nr, error)
                        );
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return buildHeaders();
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    private <T, P> void requestListInternal(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz,
            boolean retried
    ) {

        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((List<T>) cache.get(cacheKey), true);
                return;
            }

            Gson gson = isPublicApi
                    ? new GsonBuilder().create()
                    : new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            JSONObject payload = payloadModel != null
                    ? new JSONObject(gson.toJson(payloadModel))
                    : null;

            CustomJsonArrayRequest request = new CustomJsonArrayRequest(
                    convertMethod(method),
                    url,
                    payload,
                    buildHeaders(),
                    jsonArray -> {

                        List<T> list = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.optJSONObject(i);
                            if (item != null) {
                                list.add(gson.fromJson(item.toString(), clazz));
                            }
                        }

                        cache.put(cacheKey, list);
                        callback.onSuccess(list, false);
                    },
                    error -> {

                        NetworkResponse nr = error.networkResponse;

                        // 🔥 HANDLE 307 HERE
                        if (nr != null && nr.statusCode == 307 && !retried) {

                            if (nr.headers != null && nr.headers.containsKey("Set-Cookie")) {
                                cookie = mergeCookies(cookie, nr.headers.get("Set-Cookie"));
                            }

                            // 🔁 retry once with new cookie
                            requestListInternal(
                                    cacheKey,
                                    payloadModel,
                                    url,
                                    method,
                                    useCache,
                                    isPublicApi,
                                    callback,
                                    clazz,
                                    true
                            );
                            return;
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseVolleyError(nr, error)
                        );
                    }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }


    // -------------------------------------
    // Utils
    // -------------------------------------

    private String mergeCookies(String oldCookie, String setCookieHeader) {

        StringBuilder result = new StringBuilder(oldCookie == null ? "" : oldCookie);

        String[] cookies = setCookieHeader.split(",");
        for (String cookie : cookies) {
            String clean = cookie.split(";", 2)[0].trim();
            if (result.length() > 0) result.append("; ");
            result.append(clean);
        }

        return result.toString();
    }

    private int convertMethod(HttpMethod method) {
        switch (method) {
            case POST:
                return Request.Method.POST;
            case PUT:
                return Request.Method.PUT;
            case DELETE:
                return Request.Method.DELETE;
            default:
                return Request.Method.GET;
        }
    }

    private String parseVolleyError(NetworkResponse response, Throwable error) {

        if (response != null && response.data != null) {

            try {
                String json = new String(response.data, StandardCharsets.UTF_8);

                JSONObject obj = new JSONObject(json);

                if (obj.has("message"))
                    return obj.getString("message");

                if (obj.has("error"))
                    return obj.getString("error");

                if (obj.has("detail"))
                    return obj.getString("detail");

                return json;

            } catch (Exception ignored) {
            }
        }

        // Network errors
        if (error instanceof AuthFailureError)
            return "خطا در احراز هویت";

        if (error instanceof ParseError)
            return "خطا در پردازش پاسخ سرور";

        if (error instanceof com.android.volley.TimeoutError)
            return "ارتباط با سرور برقرار نشد";

        return "خطایی رخ داده است";
    }

}
