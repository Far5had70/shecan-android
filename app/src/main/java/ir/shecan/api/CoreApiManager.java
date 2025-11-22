package ir.shecan.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import ir.shecan.Shecan;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.service.VolleyHelper;

public class CoreApiManager {

    private static CoreApiManager instance;
    private final Context context;

    private CoreApiManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CoreApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new CoreApiManager(context);
        }
        return instance;
    }

    public void callCoreAPI(CoreApiResponseListener listener) {

        if (listener != null) listener.onLoading();

        String apiUrl = ShecanVpnService.getUpdaterLink();
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);

        StringRequest request = new StringRequest(
                Request.Method.GET,
                apiUrl,
                result -> handleResponse(result, listener),
                error -> {
                    if (listener != null)
                        listener.onError(error.toString());
                }
        );

        request.setShouldCache(false);
        request.setTag("CoreApiRequest");

        requestQueue.add(request);
    }

    private void handleResponse(String response, CoreApiResponseListener listener) {
        if (listener == null) return;

        String result = response != null ? response.trim() : "";

        switch (result) {
            case "invalid":
                listener.onInvalid();
                break;
            case "in the range":
                listener.onInTheRange();
                break;
            case "out of the range":
                listener.onOutOfRange();
                break;
            default:
                listener.onSuccess(result);
                Shecan.setDynamicIP(result.trim());
                break;
        }
    }
}
