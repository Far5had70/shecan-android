package ir.shecan.activity;

import android.app.ActivityOptions;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Window;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.databinding.ActivityAuthorizeBinding;
import ir.shecan.fragment.refactor.LoginFragment;

public class AuthorizeActivity extends AppCompatActivity {

    private ActivityAuthorizeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.authorizeBackgroundColor));
        }

        binding = ActivityAuthorizeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getApplicationContext().setTheme(R.style.AppTheme);

        // Load first fragment
        loadFragment(new LoginFragment(), false);

        // Dynamic height (shared logic)
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

    // ---------------- Fragment Loader ----------------
    public void loadFragment(Fragment fragment, boolean addToBackstack) {
        if (addToBackstack) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
