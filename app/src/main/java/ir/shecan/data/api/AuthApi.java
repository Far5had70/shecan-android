package ir.shecan.data.api;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ir.shecan.data.modelDio.ExistApiInput;
import ir.shecan.data.modelDio.LoginApiInput;
import ir.shecan.data.modelDio.SendOtpApiInput;
import ir.shecan.data.modelDio.VerifyApiInput;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.ExistApiViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.SendOtpApiViewModel;
import ir.shecan.data.modelDto.UserRating;
import ir.shecan.data.modelDto.VerifyApiViewModel;

public class AuthApi {

    private final ApiRepository repo;

    public AuthApi(Context context) {
        repo = new ApiRepository(context);
    }

    // ---------------------------------------------------
    // 1) exists
    // ---------------------------------------------------
    public void exists(String identifier, ApiCallback<ExistApiViewModel> callback) {

        ExistApiInput input = new ExistApiInput(identifier);

        repo.request(
                "otp_exists_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/exists",
                HttpMethod.POST,
                false,
                callback,
                ExistApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 2) login
    // ---------------------------------------------------
    public void login(String identifier, String password, ApiCallback<VerifyApiViewModel> callback) {

        LoginApiInput input = new LoginApiInput(identifier, password);

        repo.request(
                "login_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/login",
                HttpMethod.POST,
                false,
                callback,
                VerifyApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 3) send OTP
    // ---------------------------------------------------
    public void sendOtp(String identifier, ApiCallback<SendOtpApiViewModel> callback) {

        SendOtpApiInput input = new SendOtpApiInput(identifier);

        repo.request(
                "otp_send_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/send-otp",
                HttpMethod.POST,
                false,
                callback,
                SendOtpApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 4) verify OTP
    // ---------------------------------------------------

    public void verifyOtpObject(String identifier, String code, ApiCallback<VerifyApiViewModel> callback) {

        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.request(
                "otp_verify_Object_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
                false,
                callback,
                VerifyApiViewModel.class
        );
    }

    public void verifyOtp(String identifier, String code, ApiCallback<VerifyApiViewModel> callback) {

        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.requestList(
                "otp_verify_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
                false,
                false,
                new ApiCallback<List<VerifyApiViewModel>>() {
                    @Override
                    public void onSuccess(List<VerifyApiViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(null, false);
                            return;
                        }

                        callback.onSuccess(list.get(0), false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                VerifyApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 5) update password
    // ---------------------------------------------------
    public void updatePassword(String apiKey, String password, ApiCallback<EmptyResponse> callback) {

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("password", password);

        repo.request(
                "update_pass",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                callback,
                EmptyResponse.class
        );
    }

    // ---------------------------------------------------
    // 6) update profile
    // ---------------------------------------------------
    public void updateProfile(
            String apiKey,
            String firstname,
            String lastname,
            String companyName,
            String mail,
            String phoneNumber,
            ApiCallback<EmptyResponse> callback
    ) {

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("firstname", firstname);
        payload.put("lastname", lastname);
        payload.put("phoneNumber", phoneNumber);

        if (companyName != null) payload.put("company_name", companyName);

        payload.put("mail", mail);

        repo.apiManager.setApiKey(apiKey);

        repo.request(
                "update_profile",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                callback,
                EmptyResponse.class
        );
    }

    // ---------------------------------------------------
    // 7) me (account info)
    // ---------------------------------------------------
    public void me(String apiKey, ApiCallback<AccountViewModel> callback) {

        repo.apiManager.setApiKey(apiKey);

        repo.request(
                "my_account",
                null,
                "https://my.shecan.ir/my/account.json?key=" + apiKey,
                HttpMethod.GET,
                false,
                callback,
                AccountViewModel.class
        );
    }

    // ---------------------------------------------------
    // 8) issues
    // ---------------------------------------------------
    public void issues(String apiKey, int offset, int limit, ApiCallback<IssuesViewModel> callback) {

        repo.apiManager.setApiKey(apiKey);

        String url =
                "https://my.shecan.ir/issues.json?offset=" + offset +
                        "&limit=" + limit +
                        "&key=" + apiKey;

        repo.request(
                "issues",
                null,
                url,
                HttpMethod.GET,
                false,
                callback,
                IssuesViewModel.class
        );
    }


    // ---------------------------------------------------
    // 9) banner
    // ---------------------------------------------------
    public void banner(ApiCallback<BannerViewModel> callback) {

        repo.requestList(
                "banner",
                null,
                "https://n8n.coolify.shcn.ir/webhook/banner?type=1",
                HttpMethod.GET,
                false,
                true,
                new ApiCallback<List<BannerViewModel>>() {
                    @Override
                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(null, false);
                            return;
                        }
                        // کوچکترین اوردر برداشته شود
                        int minIndex = 0;
                        for (int i = 1; i < list.size(); i++) {
                            if (list.get(i).getOrder() < list.get(minIndex).getOrder()) {
                                minIndex = i;
                            }
                        }
                        callback.onSuccess(list.get(minIndex), false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                BannerViewModel.class
        );
    }

    public void bannerList(String url, ApiCallback<List<BannerViewModel>> callback) {

        repo.requestList(
                "banner",
                null,
                url,
                HttpMethod.GET,
                false,
                true,
                new ApiCallback<List<BannerViewModel>>() {
                    @Override
                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(new ArrayList<>(), false);
                            return;
                        }

                        // کل لیست را بده UI — بدون انتخاب کوچک‌ترین order
                        callback.onSuccess(list, false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                BannerViewModel.class
        );
    }

    // ---------------------------------------------------
    // 10) homePage
    // ---------------------------------------------------
    public void homePageApi(ApiCallback<HomePage> callback) {

        repo.request(
                "homePage",
                null,
                "https://shecan.ir/app/home-page/",
                HttpMethod.GET,
                false,
                callback,
                HomePage.class
        );
    }

    // ---------------------------------------------------
    // 11) SendRating
    // ---------------------------------------------------
    public void sendRatingApi(
            String url,
            String mobile,
            long weeklyConnectionTime,
            String appVersion,
            String storeName,
            int rating,
            boolean isProUser,
            ApiCallback<Void> callback) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        String createdDate = sdf.format(new Date());
//
//        Map<String, String> payload = new HashMap<>();
//        payload.put("mobile_number", mobile);
//        payload.put("weekly_connection_time", String.valueOf(weeklyConnectionTime));
//        payload.put("app_version", appVersion);
//        payload.put("store_name", storeName);
//        payload.put("rating", String.valueOf(rating));
//        payload.put("is_pro_user", String.valueOf(isProUser));
//        payload.put("create_date", createdDate);

        UserRating payload = new UserRating(
                mobile,
                weeklyConnectionTime,
                appVersion,
                storeName,
                rating,
                isProUser,
                createdDate
        );

        repo.apiManager.setSecretKey("ksdasdjcu*@ndshW@1503SdD");

        repo.requestList(
                "appStoreRate",
                payload,
                url,
                HttpMethod.POST,
                false,
                false,
                new ApiCallback<List<UserRating>>() {
                    @Override
                    public void onSuccess(List<UserRating> list, boolean fromCache) {
                        callback.onSuccess(null, false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                UserRating.class
        );
    }

}
