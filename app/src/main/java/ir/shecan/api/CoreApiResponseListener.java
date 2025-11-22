package ir.shecan.api;

public interface CoreApiResponseListener {
    void onLoading();
    void onSuccess(String result);
    void onInvalid();
    void onInTheRange();
    void onOutOfRange();
    void onError(String error);
}
