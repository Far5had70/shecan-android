package ir.shecan.fragment.refactor;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionInflater;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.api.ApiEndpoint;
import ir.shecan.api.ApiRepository;
import ir.shecan.api.AuthApi;
import ir.shecan.api.HttpMethod;
import ir.shecan.databinding.FragmentOtpBinding;
import ir.shecan.modelDio.LoginApiInput;
import ir.shecan.modelDio.SendOtpApiInput;
import ir.shecan.modelDio.VerifyApiInput;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;

public class OtpFragment extends Fragment {

    private String identifier;
    private boolean isExist;

    public OtpFragment(String identifier, boolean isExist) {
        this.identifier = identifier;
        this.isExist = isExist;
    }

    private FragmentOtpBinding binding;
    EditText[] otpFields = new EditText[6];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentOtpBinding.inflate(inflater, container, false);

        binding.agreementView.setupText("https://shecan.ir/");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setSharedElementEnterTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
            getActivity().getWindow().setSharedElementReturnTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
        }

        otpFields[0] = binding.otpLayout.otp1;
        otpFields[1] = binding.otpLayout.otp2;
        otpFields[2] = binding.otpLayout.otp3;
        otpFields[3] = binding.otpLayout.otp4;
        otpFields[4] = binding.otpLayout.otp5;
        otpFields[5] = binding.otpLayout.otp6;

        setupOtpFields();
        startTimer();

        binding.otpLayout.btnVerify.setOnClickListener(v -> validateOtp());
        binding.otpLayout.tvResend.setOnClickListener(v -> startTimer());

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.iconBackImg.setAlpha(0f);
        binding.iconBackImg.animate()
                .alpha(1f)
                .setDuration(1000)
                .start();


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

    private void setupOtpFields() {

        for (int i = 0; i < otpFields.length; i++) {
            int index = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {

                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }

                    if (s.length() > 1 && s.length() == 6) {
                        for (int i = 0; i < 6; i++) {
                            otpFields[i].setText(String.valueOf(s.charAt(i)));
                        }
                    }
                }
            });

            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == 67 && otpFields[index].getText().length() == 0) {
                    if (index > 0) otpFields[index - 1].requestFocus();
                }
                return false;
            });
        }
    }

    private void startTimer() {

        AuthApi auth = new AuthApi(requireContext());
        auth.sendOtp(identifier, (response, fromCache1) -> {

        });

        binding.otpLayout.tvResend.setVisibility(TextView.GONE);
        binding.otpLayout.tvTimer.setVisibility(TextView.VISIBLE);

        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.otpLayout.tvTimer.setText("ارسال مجدد کد در " + millisUntilFinished / 1000 + " ثانیه");
            }

            public void onFinish() {
                binding.otpLayout.tvTimer.setVisibility(TextView.GONE);
                binding.otpLayout.tvResend.setVisibility(TextView.VISIBLE);
            }
        }.start();
    }

    private void validateOtp() {

        StringBuilder code = new StringBuilder();

        for (EditText otpField : otpFields) {
            if (otpField.getText().length() == 0) {
                shakeError(getString(R.string.codeIsUnCompleted));
                return;
            }
            code.append(otpField.getText());
        }

        showLoading(true);

        AuthApi auth = new AuthApi(requireContext());
        auth.verifyOtp(
                identifier,
                code.toString(),
                (res, fromCache) -> {

                    showLoading(false);

                    if (res != null) {
                        AppStorage storage = new AppStorage(getContext());
                        storage.saveToken(res);
                        if (isExist) {
                            getActivity().finish();
                        } else {
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, new SignUpFragment())
                                    .addToBackStack(null)
                                    .commit();
                        }
                    } else {
                        shakeError(getString(R.string.codeIsWrong));
                    }
                }
        );
    }

    private void shakeError(String msg) {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        binding.otpLayout.otpContainer.startAnimation(shake);

        binding.otpLayout.tvErrorOtp.setText(msg);
        binding.otpLayout.tvErrorOtp.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.otpLayout.btnVerify.setEnabled(false);
            binding.otpLayout.btnVerify.setAlpha(0.5f);
            binding.progressVerify.setVisibility(View.VISIBLE);
            binding.otpLayout.btnVerify.setText("");
        } else {
            binding.otpLayout.btnVerify.setEnabled(true);
            binding.otpLayout.btnVerify.setAlpha(1f);
            binding.progressVerify.setVisibility(View.GONE);
            binding.otpLayout.btnVerify.setText("تایید");
        }
    }
}
