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
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import ir.shecan.R;
import ir.shecan.api.ApiEndpoint;
import ir.shecan.api.ApiRepository;
import ir.shecan.api.HttpMethod;
import ir.shecan.databinding.FragmentLoginBinding;
import ir.shecan.modelDio.ExistApiInput;
import ir.shecan.modelDto.ExistApiViewModel;

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

            JSONObject payload = new JSONObject();
            try {
                payload.put("phone", binding.edtPhoneNumber.getText().toString());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            ApiRepository repo = new ApiRepository(requireContext());

            ExistApiInput input = new ExistApiInput(binding.edtPhoneNumber.getText().toString());
            repo.<ExistApiViewModel, ExistApiInput>request(
                    "otp_" + input.getIdentifier(),
                    input,
                    ApiEndpoint.OTP_EXISTS.getPath(),
                    HttpMethod.POST,
                    false,
                    (response, fromCache) -> {
                        if (response != null && response.getExists()) {
                            goToLoginWithPasswordFragment();
                        } else {
                            goToLoginWithOtpFragment();
                        }
                    },
                    ExistApiViewModel.class
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
        OtpFragment otpFragment = new OtpFragment();
        route(otpFragment);
    }

    private void goToLoginWithPasswordFragment() {
        OtpFragment otpFragment = new OtpFragment();
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
}
