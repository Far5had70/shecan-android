package ir.shecan.api;

import ir.shecan.modelDto.ExistApiViewModel;

public class Listeners {

    public interface ApiListener<T> {
        void onReceived(T response, boolean fromCache);
    }
}
