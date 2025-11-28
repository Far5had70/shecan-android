package ir.shecan.fragment.refactor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.api.ApiCallback;
import ir.shecan.api.AuthApi;
import ir.shecan.databinding.FragmentSignUpBinding;
import ir.shecan.modelDto.EmptyResponse;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;

public class SignUpFragment extends Fragment {

    private FragmentSignUpBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSignUpBinding.inflate(inflater, container, false);

        AppStorage storage = new AppStorage(getContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        binding.edtPersianName.setText(token.getFirstname());
        binding.edtPersianFamilyName.setText(token.getLastname());
        binding.edtEmail.setText(token.getMail());

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.isCompanyCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            binding.edtCompanyName.setVisibility(isChecked ? VISIBLE : GONE);
        });

        binding.btnPassword.setOnClickListener(view -> {
            showLoading(true);
            AuthApi auth = new AuthApi(requireContext());
            auth.updateProfile(
                    token.getApiKey(),
                    binding.edtPersianName.getText().toString(),
                    binding.edtPersianFamilyName.getText().toString(),
                    binding.isCompanyCB.isChecked() ? binding.edtCompanyName.getText().toString() : null,
                    binding.edtEmail.getText().toString(),
                    new ApiCallback<EmptyResponse>() {
                        @Override
                        public void onSuccess(EmptyResponse data, boolean fromCache) {
                            showLoading(false);
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, new ChangePasswordFragment())
                                    .addToBackStack(null)
                                    .commit();
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            showLoading(false);
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        binding.btnLogin.setOnClickListener(view -> {
            showLoading(true);
            AuthApi auth = new AuthApi(requireContext());
            auth.updateProfile(
                    token.getApiKey(),
                    binding.edtPersianName.getText().toString(),
                    binding.edtPersianFamilyName.getText().toString(),
                    binding.isCompanyCB.isChecked() ? binding.edtCompanyName.getText().toString() : null,
                    binding.edtEmail.getText().toString(),
                    new ApiCallback<EmptyResponse>() {
                        @Override
                        public void onSuccess(EmptyResponse data, boolean fromCache) {
                            showLoading(false);
                            getActivity().finish();
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            showLoading(false);
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {

            if (!isAdded() || getContext() == null) return;

            int screenHeight = binding.getRoot().getHeight();

            int fiftyDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    170,
                    getResources().getDisplayMetrics()
            );

            int finalHeight = screenHeight - fiftyDp;

            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.bottomFrameLayout.getLayoutParams();

            params.height = finalHeight;
            binding.bottomFrameLayout.setLayoutParams(params);
        });

        return binding.getRoot();
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnLogin.setEnabled(false);
            binding.btnLogin.setAlpha(0.5f);
            binding.progress.setVisibility(VISIBLE);
            binding.btnLogin.setText("");
        } else {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setAlpha(1f);
            binding.progress.setVisibility(GONE);
            binding.btnLogin.setText(ContextCompat.getString(getContext(),R.string.login));
        }
    }
}
