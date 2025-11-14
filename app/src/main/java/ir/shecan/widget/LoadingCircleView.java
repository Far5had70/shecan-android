package ir.shecan.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LoadingCircleView extends View {

    private Paint paint;
    private RectF rectF;
    private float sweepAngle = 90f; // طول پروگرس
    private float startAngle = 225f; // شروع از 225 درجه
    private float rotation = 0f; // زاویه چرخش
    private int strokeWidth = 8; // ضخامت خط
    private float circleSizePercent = 1.0f; // 1.0 یعنی کل سایز ویو، 0.8 یعنی 80% از ویو

    public LoadingCircleView(Context context) {
        super(context);
        init();
    }

    public LoadingCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFFFFFFFF); // سفید
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);

        rectF = new RectF();

        // انیمیشن چرخش
        post(rotationRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = strokeWidth / 2f;
        float width = getWidth() * circleSizePercent;
        float height = getHeight() * circleSizePercent;

        float left = (getWidth() - width) / 2f + padding;
        float top = (getHeight() - height) / 2f + padding;
        float right = left + width - padding*2;
        float bottom = top + height - padding*2;

        rectF.set(left, top, right, bottom);

        canvas.drawArc(rectF, startAngle + rotation, sweepAngle, false, paint);
    }


    private Runnable rotationRunnable = new Runnable() {
        @Override
        public void run() {
            rotation += 5; // سرعت چرخش
            if (rotation >= 360) rotation -= 360;
            invalidate();
            postDelayed(this, 16); // تقریبا 60fps
        }
    };

    public void setCircleSizePercent(float percent) {
        circleSizePercent = percent; // مقدار بین 0.0 و 1.0
        invalidate();
    }
}