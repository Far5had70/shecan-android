package ir.shecan.api;

import android.content.Context;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.shecan.api.ApiRepository;
import ir.shecan.api.HttpMethod;
import ir.shecan.api.Listeners;
import ir.shecan.modelDio.ExistApiInput;
import ir.shecan.modelDio.LoginApiInput;
import ir.shecan.modelDio.SendOtpApiInput;
import ir.shecan.modelDio.VerifyApiInput;
import ir.shecan.modelDto.AccountViewModel;
import ir.shecan.modelDto.ExistApiViewModel;
import ir.shecan.modelDto.IssuesViewModel;
import ir.shecan.modelDto.SendOtpApiViewModel;
import ir.shecan.modelDto.VerifyApiViewModel;

public class AuthApi {

    private final ApiRepository repo;

    public AuthApi(Context context) {
        repo = new ApiRepository(context);
    }

    // ------------------------
    // 1) exists
    // ------------------------
    public void exists(
            String identifier,
            Listeners.ApiListener<ExistApiViewModel> listener
    ) {
        ExistApiInput input = new ExistApiInput(identifier);

        repo.request(
                "otp_exists_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/exists",
                HttpMethod.POST,
                false,
                listener,
                ExistApiViewModel.class
        );
    }

    // ------------------------
    // 2) login
    // ------------------------
    public void login(
            String identifier,
            String password,
            Listeners.ApiListener<VerifyApiViewModel> listener
    ) {
        LoginApiInput input = new LoginApiInput(identifier, password);

        repo.request(
                "login_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/login",
                HttpMethod.POST,
                false,
                listener,
                VerifyApiViewModel.class
        );
    }

    // ------------------------
    // 3) send otp
    // ------------------------
    public void sendOtp(
            String identifier,
            Listeners.ApiListener<SendOtpApiViewModel> listener
    ) {
        SendOtpApiInput input = new SendOtpApiInput(identifier);

        repo.request(
                "otp_send_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/send-otp",
                HttpMethod.POST,
                false,
                listener,
                SendOtpApiViewModel.class
        );
    }

    // ------------------------
    // 4) verify otp
    // ------------------------
    public void verifyOtp(
            String identifier,
            String code,
            Listeners.ApiListener<VerifyApiViewModel> listener
    ) {
        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.requestList(
                "otp_verify_" + identifier,
                input,
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
                false,
                (response, fromCache) -> {
                    if (response == null || response.isEmpty()) {
                        listener.onReceived(null, false);
                    } else {
                        VerifyApiViewModel model = response.get(0);
                        listener.onReceived(model, false);
                    }
                },
                VerifyApiViewModel.class
        );

    }

    // ------------------------
    // 5) update password
    // ------------------------
    public void updatePassword(
            String apiKey,
            String password,
            Listeners.ApiListener<Void> listener
    ) {
        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("password", password);

        repo.request(
                "update_pass",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                listener,
                Void.class
        );
    }

    // ------------------------
    // 6) update profile
    // ------------------------
    public void updateProfile(
            String apiKey,
            String firstname,
            String lastname,
            String companyName,
            String mail,
            Listeners.ApiListener<Void> listener
    ) {
        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("firstname", firstname);
        payload.put("lastname", lastname);

        if (companyName != null){
            payload.put("company_name", companyName);
        }


        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

        payload.put("mail", mail);

        repo.request(
                "update_profile",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                listener,
                Void.class
        );
    }

    // ------------------------
    // 7) get account info
    // ------------------------
    public void me(
            String apiKey,
            Listeners.ApiListener<AccountViewModel> listener
    ) {

        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

        repo.request(
                "my_account",
                null,
                "https://my.shecan.ir/my/account.json?key=" + apiKey,
                HttpMethod.GET,
                false,
                listener,
                AccountViewModel.class
        );
    }

    // ------------------------
    // 8) get issues
    // ------------------------
    public void issues(
            String apiKey,
            int offset,
            int limit,
            Listeners.ApiListener<IssuesViewModel> listener
    ) {
        repo.apiManager.setOptionalHeader("x-redmine-api-key", apiKey);

        String url = "https://my.shecan.ir/issues.json?offset=" + offset +
                "&limit=" + limit +
                "&key=" + apiKey;

        repo.request(
                "issues",
                null,
                url,
                HttpMethod.GET,
                false,
                listener,
                IssuesViewModel.class
        );
    }
}