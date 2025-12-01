package ir.shecan.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.api.ApiCallback;
import ir.shecan.api.AuthApi;
import ir.shecan.constant.Constant;
import ir.shecan.databinding.ActivityUpdatePrrofileBinding;
import ir.shecan.modelDto.AccountViewModel;
import ir.shecan.modelDto.EmptyResponse;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;
import ir.shecan.util.AppUtils;

public class UpdateProfileActivity extends AppCompatActivity {

    private ActivityUpdatePrrofileBinding binding;
    private AppStorage storage;
    private int currentPosition = 1;
    private VerifyApiViewModel token;
    private AuthApi authApi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUpdatePrrofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initDependencies();
        initViews();
        initListeners();
    }

    private void initDependencies() {
        storage = new AppStorage(getApplicationContext());
        token = storage.getToken(VerifyApiViewModel.class);
        authApi = new AuthApi(getApplicationContext());
    }

    private void initViews() {
        binding.toolbar.back.setVisibility(VISIBLE);
        binding.edtPersianName.setText(token.getFirstname());
        binding.edtPersianFamilyName.setText(token.getLastname());
        binding.edtEmail.setText(token.getMail());
        handleTabChange(1);
    }

    private void initListeners() {

        binding.toolbar.back.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.btnCancel.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.btnSave.setOnClickListener(v -> {
            if (currentPosition == 1) {
                updateProfile();
            } else {
                updatePassword();
            }
        });

        binding.profileTabBar.setOnProfileTabSelected(index -> {
            currentPosition = index;
            handleTabChange(index);
        });

        binding.toolbar.vip.setOnClickListener(view -> AppUtils.openUrl(Constant.PlanUrl, this));

    }

    private void handleTabChange(int index) {
        boolean isProfileTab = index == 1;

        binding.profileTab.setVisibility(isProfileTab ? VISIBLE : GONE);
        binding.passwordTab.setVisibility(isProfileTab ? GONE : VISIBLE);
    }

    private void updatePassword() {
        String newPass = binding.edtPasswordNew.getText().toString();
        String repeat = binding.edtPasswordNewRepeat.getText().toString();

        if (!newPass.equals(repeat)) {
            Toast.makeText(this, R.string.passworsNotSame, Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);

        authApi.updatePassword(
                token.getApiKey(),
                newPass,
                new ApiCallback<EmptyResponse>() {
                    @Override
                    public void onSuccess(EmptyResponse data, boolean fromCache) {
                        showLoading(false);
                        updateUserInformation();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showLoading(false);
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void updateProfile() {

        showLoading(true);

        authApi.updateProfile(
                token.getApiKey(),
                binding.edtPersianName.getText().toString(),
                binding.edtPersianFamilyName.getText().toString(),
                null,
                binding.edtEmail.getText().toString(),
                new ApiCallback<EmptyResponse>() {
                    @Override
                    public void onSuccess(EmptyResponse data, boolean fromCache) {
                        showLoading(false);
                        updateUserInformation();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showLoading(false);
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void updateUserInformation() {

        showLoading(true);

        authApi.me(
                token.getApiKey(),
                new ApiCallback<AccountViewModel>() {
                    @Override
                    public void onSuccess(AccountViewModel res, boolean fromCache) {
                        showLoading(false);

                        if (res != null && res.getUser() != null) {

                            // update token fields
                            token.setFirstname(res.getUser().getFirstname());
                            token.setLastname(res.getUser().getLastname());
                            token.setMail(res.getUser().getMail());

                            // save both
                            storage.saveToken(token);

                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showLoading(false);
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void showLoading(boolean loading) {
        binding.btnSave.setEnabled(!loading);
        binding.btnSave.setAlpha(loading ? 0.5f : 1f);
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        binding.btnSave.setText(
                loading ? "" : ContextCompat.getString(this, R.string.saveChange)
        );
    }
}
