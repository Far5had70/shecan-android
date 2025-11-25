package ir.shecan.fragment.refactor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.api.ApiEndpoint;
import ir.shecan.api.ApiRepository;
import ir.shecan.api.HttpMethod;
import ir.shecan.databinding.FragmentPasswordBinding;
import ir.shecan.modelDio.LoginApiInput;
import ir.shecan.modelDto.VerifyApiViewModel;
import ir.shecan.storage.AppStorage;

public class PasswordFragment extends Fragment {

    private String identifier;

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
            ApiRepository repo = new ApiRepository(requireContext());
            LoginApiInput input = new LoginApiInput(identifier, binding.edtPassword.getText().toString());
            repo.<VerifyApiViewModel, LoginApiInput>request(
                    "login_" + input.getIdentifier(),
                    input,
                    ApiEndpoint.LOGIN.getPath(),
                    HttpMethod.POST,
                    false,
                    (response, fromCache) -> {
                        if (response != null){
                            AppStorage storage = new AppStorage(getContext());
                            storage.saveToken(response);
//                            VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
                            getActivity().finish();
                        }
                    },
                    VerifyApiViewModel.class
            );
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
}
