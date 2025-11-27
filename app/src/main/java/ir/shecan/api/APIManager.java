package ir.shecan.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIManager {

    private static APIManager instance;
    private final RequestQueue requestQueue;

    private final Map<String, Object> cache = new HashMap<>();
    private final Map<String, String> optionalHeaders = new HashMap<>();

    private APIManager(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized APIManager getInstance(Context context) {
        if (instance == null) instance = new APIManager(context);
        return instance;
    }

    // -------------------------------------
    //     مدیریت هدرهای اختیاری
    // -------------------------------------

    public void setOptionalHeader(String key, String value) {
        optionalHeaders.put(key, value);
    }

    public void setOptionalHeaders(Map<String, String> headers) {
        optionalHeaders.putAll(headers);
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();

        // هدر اختصاصی شما
        headers.put("x-redmine-api-key", optionalHeaders.getOrDefault("x-redmine-api-key", ""));

        // سایر هدرهای دلخواه
        for (String key : optionalHeaders.keySet()) {
            if (!key.equals("x-redmine-api-key")) {
                headers.put(key, optionalHeaders.get(key));
            }
        }
        return headers;
    }

    // -------------------------------------
    //      Single Object Request
    // -------------------------------------

    public <T, P> void requestObject(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            Listeners.ApiListener<T> listener,
            Class<T> clazz
    ) {
        try {
            if (useCache && cache.containsKey(cacheKey)) {
                listener.onReceived((T) cache.get(cacheKey), true);
            }

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            JSONObject payload = payloadModel != null ?
                    new JSONObject(gson.toJson(payloadModel)) : null;

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {
                        JSONObject dataObject = response.optJSONObject("data");
                        if (dataObject == null) dataObject = response;

                        T model = gson.fromJson(dataObject.toString(), clazz);
                        cache.put(cacheKey, model);

                        listener.onReceived(model, false);
                    },
                    error -> {
                        try {
                            if (error.networkResponse != null) {
                                String body = new String(
                                        error.networkResponse.data,
                                        StandardCharsets.UTF_8
                                );
                                JSONObject obj = new JSONObject(body);
                                Log.e("API_ERROR", obj.optString("error"));
                            }
                        } catch (Exception ignored) {}
                        listener.onReceived(null, false);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return buildHeaders();
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            listener.onReceived(null, false);
        }
    }

    // -------------------------------------
    //      List Request
    // -------------------------------------

    public <T, P> void requestList(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            Listeners.ApiListener<List<T>> listener,
            Mapper<JSONArray, List<T>> mapper
    ) {
        try {
            if (useCache && cache.containsKey(cacheKey)) {
                listener.onReceived((List<T>) cache.get(cacheKey), true);
            }

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            JSONObject payload = payloadModel != null ?
                    new JSONObject(gson.toJson(payloadModel)) : null;

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {
                        JSONArray dataArray = response.optJSONArray("data");
                        if (dataArray == null) dataArray = new JSONArray();

                        List<T> list = mapper.map(dataArray);
                        cache.put(cacheKey, list);

                        listener.onReceived(list, false);
                    },
                    error -> listener.onReceived(null, false)
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return buildHeaders();
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            listener.onReceived(null, false);
        }
    }

    private int convertMethod(HttpMethod method) {
        switch (method) {
            case POST: return Request.Method.POST;
            case PUT: return Request.Method.PUT;
            case DELETE: return Request.Method.DELETE;
            default: return Request.Method.GET;
        }
    }
}
