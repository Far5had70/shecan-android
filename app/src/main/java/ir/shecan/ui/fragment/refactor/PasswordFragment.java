package ir.shecan.ui.fragment.refactor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentPasswordBinding;
import ir.shecan.data.modelDto.SendOtpApiViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

public class PasswordFragment extends Fragment {

    private final String identifier;

    public PasswordFragment(String identifier) {
        this.identifier = identifier;
    }

    private FragmentPasswordBinding binding;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingStoppedRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPasswordBinding.inflate(inflater, container, false);

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.iconBackImg.setAlpha(0f);
        binding.iconBackImg.animate()
                .alpha(1f)
                .setDuration(1000)
                .start();

        setupTypingAnimation();

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

        binding.btnContinue.setOnClickListener(view -> {

            showLoading(true);

            AuthApi auth = new AuthApi(requireContext());
            auth.login(
                    identifier,
                    binding.edtPassword.getText().toString(),
                    new ApiCallback<VerifyApiViewModel>() {
                        @Override
                        public void onSuccess(VerifyApiViewModel res, boolean fromCache) {
                            showLoading(false);
                            if (res != null) {
                                AppStorage storage = new AppStorage(getContext());
                                storage.saveToken(res);
                                getActivity().finish();
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

        binding.btnContinueWithOtpCode.setOnClickListener(view -> {
            AuthApi auth = new AuthApi(requireContext());
            auth.sendOtp(identifier, new ApiCallback<SendOtpApiViewModel>() {
                @Override
                public void onSuccess(SendOtpApiViewModel data, boolean fromCache) {

                }

                @Override
                public void onError(int statusCode, String message) {
//                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
            });

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new OtpFragment(identifier, true))
                    .addToBackStack(null)
                    .commit();
        });

        return binding.getRoot();
    }

    private void setupTypingAnimation() {

        typingStoppedRunnable = () -> {
            binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
        };

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (!hasFocus) {
                binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
            }
        };

        View.OnKeyListener typingListener = (v, keyCode, event) -> {
            binding.mascot.setImageResource(R.drawable.ic_shecan_hide_eye);
            typingHandler.removeCallbacks(typingStoppedRunnable);
            typingHandler.postDelayed(typingStoppedRunnable, 500);

            return false;
        };

        binding.edtPassword.setOnKeyListener(typingListener);
        binding.edtPassword.setOnFocusChangeListener(focusListener);
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnContinue.setEnabled(false);
            binding.btnContinue.setAlpha(0.5f);
            binding.progressVerify.setVisibility(View.VISIBLE);
            binding.btnContinue.setText("");
        } else {
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1f);
            binding.progressVerify.setVisibility(View.GONE);
            binding.btnContinue.setText(ContextCompat.getString(getContext(), R.string.login));
        }
    }
}
