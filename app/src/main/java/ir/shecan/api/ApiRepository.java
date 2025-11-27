package ir.shecan.api;

import android.content.Context;
import org.json.JSONArray;
import java.util.List;

public class ApiRepository {

    private final APIManager apiManager;

    public ApiRepository(Context context) {
        apiManager = APIManager.getInstance(context);
    }

    public <T, P> void request(
            String cacheKey,
            P payload,
            String url,
            HttpMethod method,
            boolean useCache,
            Listeners.ApiListener<T> listener,
            Class<T> clazz
    ) {
        apiManager.requestObject(cacheKey, payload, url, method, useCache, listener, clazz);
    }

    public <T, P> void requestList(
            String cacheKey,
            P payload,
            String url,
            HttpMethod method,
            boolean useCache,
            Listeners.ApiListener<List<T>> listener,
            Mapper<JSONArray, List<T>> mapper
    ) {
        apiManager.requestList(cacheKey, payload, url, method, useCache, listener, mapper);
    }
}
