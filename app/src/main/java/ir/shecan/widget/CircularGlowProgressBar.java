package ir.shecan.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class CircularGlowProgressBar extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF arcRect;
    private int progress = 60;   // مقدار پیشفرض

    private int[] gradientColors = {
            0xFFFF6A00, // نارنجی
            0xFF00C853, // سبز
            0xFF00E5FF, // آبی
            0xFFFF6A00  // 🔥 تکرار رنگ اول برای حذف خط مرزی
    };

    public CircularGlowProgressBar(Context context) {
        super(context);
        init();
    }

    public CircularGlowProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        setLayerType(LAYER_TYPE_SOFTWARE, null); // مهم برای Glow

        // حلقه اصلی
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(22f);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        // گلو داخلی دور رنگ
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(15f);
        glowPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));

        // هاله بیرونی خیلی محو
        outerGlowPaint.setStyle(Paint.Style.STROKE);
        outerGlowPaint.setStrokeWidth(20f);
        outerGlowPaint.setMaskFilter(new BlurMaskFilter(40, BlurMaskFilter.Blur.NORMAL));
        outerGlowPaint.setAlpha(20);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        float pad = 60f;
        arcRect = new RectF(pad, pad, w - pad, h - pad);

        Shader shader = new SweepGradient(
                w / 2f, h / 2f,
                gradientColors,
                null
        );
        ringPaint.setShader(shader);
        glowPaint.setShader(shader);
        outerGlowPaint.setShader(shader);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float sweepAngle = (progress / 100f) * 360f;

        // هاله‌ خیلی محو
        canvas.drawArc(arcRect, -90, sweepAngle, false, outerGlowPaint);

        // گلو ضخیم
        canvas.drawArc(arcRect, -90, sweepAngle, false, glowPaint);

        // حلقه اصلی
        canvas.drawArc(arcRect, -90, sweepAngle, false, ringPaint);
    }

    // -------------- Public API ------------------------

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(100, value));
        invalidate();
    }

    public void setProgressAnimated(int to) {
        ValueAnimator anim = ValueAnimator.ofInt(progress, to);
        anim.setDuration(900);
        anim.addUpdateListener(v -> {
            progress = (int) v.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void setGradientColors(int... colors) {
        this.gradientColors = colors;
        invalidate();
    }
}
