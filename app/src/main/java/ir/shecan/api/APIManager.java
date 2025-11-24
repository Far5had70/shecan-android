// نسخه‌ی بهینه‌شده APIManager و ApiRepository
// شامل مدیریت کش، مدیریت صحیح JSON، پشتیبانی از Single و List، مدیریت خطا، و Mapper انعطاف‌پذیر

package ir.shecan.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class APIManager {

    private static APIManager instance;
    private final RequestQueue requestQueue;
    private final Map<String, Object> cache = new HashMap<>();
    private final Gson gson = new Gson();

    private APIManager(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized APIManager getInstance(Context context) {
        if (instance == null) instance = new APIManager(context);
        return instance;
    }

    // ------------------------------
    //   Single Object API Request
    // ------------------------------

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

            JSONObject payload = payloadModel != null ? new JSONObject(gson.toJson(payloadModel)) : null;

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {

                        // اگر API خروجی را در "data" بدهد → آن را بخوان
                        JSONObject dataObject = response.optJSONObject("data");
                        if (dataObject == null) dataObject = response;

                        // خودکار تبدیل به مدل
                        T model = gson.fromJson(dataObject.toString(), clazz);

                        cache.put(cacheKey, model);
                        listener.onReceived(model, false);
                    },
                    error -> {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String body = new String(error.networkResponse.data, "UTF-8");
                                JSONObject obj = new JSONObject(body);
                                String errorMessage = obj.optString("error", "خطای ناشناخته");
                                Log.e("API_ERROR", "Message: " + errorMessage);
//                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.onReceived(null, false);
                    }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            listener.onReceived(null, false);
        }
    }

    // ------------------------------
    //   List API Request
    // ------------------------------

    public <T, P> void requestList(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            Listeners.ApiListener<List<T>> listener,
            Function<JSONArray, List<T>> mapper
    ) {
        try {
            if (useCache && cache.containsKey(cacheKey)) {
                listener.onReceived((List<T>) cache.get(cacheKey), true);
            }

            JSONObject payload = payloadModel != null ? new JSONObject(gson.toJson(payloadModel)) : null;

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {
                        JSONArray dataArray = response.optJSONArray("data");
                        if (dataArray == null) dataArray = new JSONArray();

                        List<T> model = mapper.apply(dataArray);
                        cache.put(cacheKey, model);
                        listener.onReceived(model, false);
                    },
                    error -> listener.onReceived(null, false)
            );

            request.setRetryPolicy(new DefaultRetryPolicy(8000, 1, 1f));

            requestQueue.add(request);

        } catch (Exception ex) {
            listener.onReceived(null, false);
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
