package ir.shecan.data.api;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

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
        String apiKey = optionalHeaders.containsKey("x-redmine-api-key")
                ? optionalHeaders.get("x-redmine-api-key")
                : "";

        headers.put("x-redmine-api-key", apiKey);

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
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        try {

            // === Cache Check ===
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

                            // --------------- Handling null responses safely ----------------------

                            if (response == null || response.toString().equals("null")) {

                                // اگر مدل EmptyResponse بود → بدون JSON استفاده شود
                                if (clazz.equals(EmptyResponse.class)) {
                                    T model = clazz.getDeclaredConstructor().newInstance();
                                    callback.onSuccess(model, false);
                                    return;
                                }

                                // در غیر اینصورت یک JSONObject خالی
                                response = new JSONObject();
                            }

                            // ---------------- Parse "data" or fallback to root ------------------

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) data = response;

                            T model = gson.fromJson(data.toString(), clazz);

                            cache.put(cacheKey, model);
                            callback.onSuccess(model, false);

                        } catch (Exception ex) {
                            callback.onError(-2, ex.getMessage());
                        }
                    },
                    error -> {
                        int code = 0;
                        String message = "Unknown error";

                        try {
                            if (error.networkResponse != null) {
                                code = error.networkResponse.statusCode;

                                String body = new String(
                                        error.networkResponse.data,
                                        StandardCharsets.UTF_8
                                ).trim();

                                if (body.isEmpty()) {
                                    message = "Empty error response";
                                } else {
                                    try {
                                        JSONObject obj = new JSONObject(body);

                                        if (obj.has("error")) {
                                            message = obj.getString("error");
                                        } else if (obj.has("message")) {
                                            message = obj.getString("message");
                                        } else {
                                            message = body;
                                        }

                                    } catch (JSONException je) {
                                        message = body;
                                    }
                                }
                            }
                        } catch (Exception e2) {
                            message = e2.getMessage();
                        }

                        callback.onError(code, message);
                    }
            ) {

                // ---------------- Force accept empty/null server responses ------------------
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    try {
                        String jsonString = new String(
                                response.data,
                                HttpHeaderParser.parseCharset(response.headers, "utf-8")
                        ).trim();

                        // Handle null / empty / 204
                        if (jsonString.isEmpty() ||
                                jsonString.equals("null") ||
                                response.statusCode == 204) {

                            return Response.success(
                                    new JSONObject(), // return empty object
                                    HttpHeaderParser.parseCacheHeaders(response)
                            );
                        }

                        return super.parseNetworkResponse(response);

                    } catch (Exception e) {
                        return Response.error(new ParseError(e));
                    }
                }

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


    // -------------------------------------
    //      List Request
    // -------------------------------------

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
        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((List<T>) cache.get(cacheKey), true);
                return;
            }

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();

            if(isPublicApi){
                gson = new GsonBuilder().create();
            }

            JSONObject payload = payloadModel != null ? new JSONObject(gson.toJson(payloadModel)) : null;

            Gson finalGson = gson;
            CustomJsonArrayRequest request = new CustomJsonArrayRequest(
                    convertMethod(method),
                    url,
                    payload,
                    buildHeaders(),
                    jsonArray -> {

                        List<T> list = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.optJSONObject(i);
                            list.add(finalGson.fromJson(item.toString(), clazz));
                        }

                        cache.put(cacheKey, list);
                        callback.onSuccess(list, false);
                    },
                    error -> {

                        int code = 0;
                        String msg = "Unknown error";

                        if (error.networkResponse != null) {
                            code = error.networkResponse.statusCode;
                            msg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        }

                        callback.onError(code, msg);
                    }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
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
}
