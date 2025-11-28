package ir.shecan.fragment.refactor;

import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionInflater;
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

import org.json.JSONException;
import org.json.JSONObject;

import ir.shecan.R;
import ir.shecan.api.ApiCallback;
import ir.shecan.api.ApiEndpoint;
import ir.shecan.api.ApiRepository;
import ir.shecan.api.AuthApi;
import ir.shecan.api.HttpMethod;
import ir.shecan.api.Listeners;
import ir.shecan.databinding.FragmentLoginBinding;
import ir.shecan.modelDio.ExistApiInput;
import ir.shecan.modelDto.ExistApiViewModel;
import ir.shecan.modelDto.SendOtpApiViewModel;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);

        binding.agreementView.setupText("https://shecan.ir/");

        binding.btnContinue.setOnClickListener(v -> {

            showLoading(true);

            String identifier = binding.edtPhoneNumber.getText().toString();
            AuthApi auth = new AuthApi(requireContext());
            auth.exists(
                    identifier,
                    new ApiCallback<ExistApiViewModel>() {
                        @Override
                        public void onSuccess(ExistApiViewModel res, boolean fromCache) {
                            if (res != null) {

                                if (res.getExists()) {
                                    showLoading(false);
                                    goToLoginWithPasswordFragment();
                                } else {
                                    showLoading(false);
                                    goToLoginWithOtpFragment();
                                }
                            }
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

    private void goToLoginWithOtpFragment() {
        OtpFragment otpFragment = new OtpFragment(binding.edtPhoneNumber.getText().toString(), false);
        route(otpFragment);
    }

    private void goToLoginWithPasswordFragment() {
        PasswordFragment otpFragment = new PasswordFragment(binding.edtPhoneNumber.getText().toString());
        route(otpFragment);
    }

    private void route(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.setSharedElementEnterTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
            fragment.setSharedElementReturnTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(binding.iconToolbar, "toolbar_logo")
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }


    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnContinue.setEnabled(false);
            binding.btnContinue.setAlpha(0.5f);
            binding.progress.setVisibility(View.VISIBLE);
            binding.btnContinue.setText("");
        } else {
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1f);
            binding.progress.setVisibility(View.GONE);
            binding.btnContinue.setText(ContextCompat.getString(getContext(), R.string.continuee));
        }
    }
}
