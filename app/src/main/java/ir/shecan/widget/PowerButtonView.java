package ir.shecan.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import ir.shecan.databinding.ButtonLayoutBinding;

public class PowerButtonView extends ConstraintLayout {

    private ButtonLayoutBinding binding;

    public PowerButtonView(Context context) {
        super(context);
        init(context);
    }

    public PowerButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PowerButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        binding = ButtonLayoutBinding.inflate(LayoutInflater.from(context), this, true);

        setClickable(true);
        setFocusable(true);

        // تاچ روی circle و power
        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    binding.circle.setScaleX(0.92f);
                    binding.circle.setScaleY(0.92f);

                    binding.power.setScaleX(0.88f);
                    binding.power.setScaleY(0.88f);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    binding.circle.setScaleX(1f);
                    binding.circle.setScaleY(1f);

                    binding.power.setScaleX(1f);
                    binding.power.setScaleY(1f);
                    break;
            }
            return false;
        });

        // لودینگ در ابتدا مخفی
        binding.loadingCircle.setVisibility(View.GONE);
        binding.loadingCircle.setCircleSizePercent(0.85f);
    }

    /**
     * فعال/غیرفعال کردن حالت لودینگ
     */
    public void showLoading(boolean show) {
        binding.loadingCircle.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public boolean isLoading() {
        return binding.loadingCircle.getVisibility() == View.VISIBLE;
    }

    /**
     * فعال/غیرفعال کردن دکمه
     */
    public void setEnabledState(boolean enabled) {
        setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.5f);
    }
}
