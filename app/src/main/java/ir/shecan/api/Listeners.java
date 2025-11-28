package ir.shecan.api;

public class Listeners {

    public interface ApiListener<T> {
        void onReceived(T response, boolean fromCache);
    }
}
