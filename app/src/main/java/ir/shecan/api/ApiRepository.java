
package ir.shecan.api;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.function.Function;

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

    ///
    /// repository.requestList(
    ///     "users",
    ///     null,
    ///     url,
    ///     HttpMethod.GET,
    ///     true,
    ///     listener,
    ///     jsonArray -> {
    ///         List<User> list = new ArrayList<>();
    ///         // Convert JSON here...
    ///         return list;
    ///     }
    /// );
    ///
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