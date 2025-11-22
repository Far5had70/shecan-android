package ir.shecan.activity;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import ir.shecan.R;
import ir.shecan.databinding.FragmentLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private FragmentLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(0);
        window.setStatusBarColor(getColor(R.color.authorizeBackgroundColor));

        binding = FragmentLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getApplicationContext().setTheme(R.style.AppTheme);

        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {

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

    }
}
