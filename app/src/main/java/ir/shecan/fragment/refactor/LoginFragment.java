package ir.shecan.fragment.refactor;

import android.app.ActivityOptions;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ir.shecan.R;
import ir.shecan.activity.AuthorizeActivity;
import ir.shecan.databinding.FragmentLoginBinding;

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

            OtpFragment otpFragment = new OtpFragment();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                otpFragment.setSharedElementEnterTransition(
                        TransitionInflater.from(getContext())
                                .inflateTransition(R.transition.change_bounds)
                );
                otpFragment.setSharedElementReturnTransition(
                        TransitionInflater.from(getContext())
                                .inflateTransition(R.transition.change_bounds)
                );
            }

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .addSharedElement(binding.iconToolbar, "toolbar_logo")
                    .replace(R.id.fragmentContainer, otpFragment)
                    .addToBackStack(null)
                    .commit();
        });


        return binding.getRoot();
    }
}
