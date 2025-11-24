package ir.shecan.fragment.refactor;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.databinding.FragmentOtpBinding;

public class OtpFragment extends Fragment {

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
                shakeError("کد ناقص است");
                return;
            }
            code.append(otpField.getText());
        }

        shakeError("کد اشتباه است");
    }

    private void shakeError(String msg) {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        binding.otpLayout.otpContainer.startAnimation(shake);

        binding.otpLayout.tvErrorOtp.setText(msg);
        binding.otpLayout.tvErrorOtp.setVisibility(View.VISIBLE);
    }
}
