//package ir.shecan.api;
//
//import android.content.Context;
//
//import ir.shecan.api.ApiRepository;
//import ir.shecan.api.HttpMethod;
//import ir.shecan.api.Listeners;
//
//public class DomainApi {
//
//    private final ApiRepository repo;
//
//    public DomainApi(Context context) {
//        repo = new ApiRepository(context);
//    }
//
//    public void check(
//            String domain,
//            String password,
//            Listeners.ApiListener<DomainCheckViewModel> listener
//    ) {
//        String url = "https://shecan.ir/wp-json/shecan/v1/domain-check?domain="
//                + domain +
//                "&password=" + password;
//
//        repo.request(
//                "domain_check_" + domain,
//                null,
//                url,
//                HttpMethod.GET,
//                false,
//                listener,
//                DomainCheckViewModel.class
//        );
//    }
//}