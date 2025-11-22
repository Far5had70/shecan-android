package ir.shecan.api;

import android.content.Context;

public class CoreRepository {

    private static CoreRepository instance;
    private String coreApiResult = null;
    private boolean isLoading = false;

    private CoreRepository() {}

    public static synchronized CoreRepository getInstance() {
        if (instance == null) {
            instance = new CoreRepository();
        }
        return instance;
    }

    public String getCachedResult() {
        return coreApiResult;
    }

    public boolean hasCache() {
        return coreApiResult != null;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void fetchCoreAPI(Context context, CoreApiResponseListener listener) {

        if (hasCache()) {
            listener.onSuccess(coreApiResult);
            return;
        }

        isLoading = true;

        CoreApiManager.getInstance(context).callCoreAPI(new CoreApiResponseListener() {
            @Override public void onLoading() { listener.onLoading(); }

            @Override public void onSuccess(String result) {
                isLoading = false;
                coreApiResult = result;
                listener.onSuccess(result);
            }

            @Override public void onInvalid() {
                isLoading = false;
                listener.onInvalid();
            }

            @Override public void onInTheRange() {
                isLoading = false;
                listener.onInTheRange();
            }

            @Override public void onOutOfRange() {
                isLoading = false;
                listener.onOutOfRange();
            }

            @Override public void onError(String error) {
                isLoading = false;
                listener.onError(error);
            }
        });
    }
}
