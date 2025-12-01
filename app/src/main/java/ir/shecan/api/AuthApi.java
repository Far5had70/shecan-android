package ir.shecan.api;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.shecan.modelDio.ExistApiInput;
import ir.shecan.modelDio.LoginApiInput;
import ir.shecan.modelDio.SendOtpApiInput;
import ir.shecan.modelDio.VerifyApiInput;
import ir.shecan.modelDto.AccountViewModel;
import ir.shecan.modelDto.BannerViewModel;
import ir.shecan.modelDto.EmptyResponse;
import ir.shecan.modelDto.ExistApiViewModel;
import ir.shecan.modelDto.IssuesViewModel;
import ir.shecan.modelDto.SendOtpApiViewModel;
import ir.shecan.modelDto.VerifyApiViewModel;

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
    public void verifyOtp(String identifier, String code, ApiCallback<VerifyApiViewModel> callback) {

        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.requestList(
                "otp_verify_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
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
            ApiCallback<EmptyResponse> callback
    ) {

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("firstname", firstname);
        payload.put("lastname", lastname);

        if (companyName != null) payload.put("company_name", companyName);

        payload.put("mail", mail);

        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

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

        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

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

        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

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
}
