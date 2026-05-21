package ir.shecan.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.data.modelDto.BannerViewModel;

public class SlideshowView extends RelativeLayout {

    private static final long DEFAULT_SLIDE_DURATION_MS = 5000L;

    private ImageView imageView;
    private LinearLayout indicatorLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> slides = new ArrayList<>();
    private final List<BannerViewModel> bannerList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;
    private float downX;
    private float downY;
    private boolean swiping;
    private int touchSlop;
    private int minBannerHeight;
    private int defaultBannerHeight;
    private int measuredBannerHeight;
    private boolean roundLoadedImage = true;
    private boolean testSlidesEnabled = false;

    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            showNext();
            scheduleNextSlide();
        }
    };

    public SlideshowView(Context context) {
        super(context);
        init();
    }

    public SlideshowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClipToOutline(false);
        setMinimumHeight(dp(112));
        minBannerHeight = dp(112);
        defaultBannerHeight = dp(112);
        measuredBannerHeight = defaultBannerHeight;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        imageView = new ImageView(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        imageView.setAdjustViewBounds(false);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(lp);
        imageView.setBackground(makeRoundRect(Color.argb(28, 39, 69, 58), dp(28)));
        imageView.setOnTouchListener((v, event) -> handleTouch(event));
        addView(imageView);

        indicatorLayout = new LinearLayout(getContext());
        indicatorLayout.setGravity(Gravity.CENTER);
        indicatorLayout.setOrientation(LinearLayout.HORIZONTAL);
        indicatorLayout.setPadding(dp(8), dp(4), dp(8), dp(4));
        indicatorLayout.setBackground(makeRoundRect(Color.argb(90, 6, 31, 22), dp(12)));
        LayoutParams indicatorLp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        indicatorLp.addRule(ALIGN_PARENT_BOTTOM, TRUE);
        indicatorLp.addRule(CENTER_HORIZONTAL, TRUE);
        indicatorLp.setMargins(0, 0, 0, dp(8));
        addView(indicatorLayout, indicatorLp);

        setOnTouchListener((v, event) -> handleTouch(event));
    }

    // گرفتن لیست عکس‌ها
    public void setSlides(List<String> slideList) {
        slides.clear();
        if (slideList != null) {
            for (String slide : slideList) {
                if (slide != null && !slide.trim().isEmpty()) slides.add(slide);
            }
        }
        addTestSlidesIfNeeded();
        currentIndex = 0;
        updateIndicators();
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    private void showSlide(String item) {
        int radius = (int) (30 * getResources().getDisplayMetrics().density);

        RequestOptions options = new RequestOptions()
                .placeholder(makeRoundRect(Color.argb(28, 39, 69, 58), radius))
                .error(makeRoundRect(Color.argb(28, 39, 69, 58), radius));

        if (!roundLoadedImage) {
            options = options.transform(new FitCenter());
        } else {
            options = options.transform(new FitCenter(), new com.bumptech.glide.load.resource.bitmap.RoundedCorners(radius));
        }

        if (item.startsWith("http")) {
            Glide.with(getContext())
                    .load(item)
                    .listener(imageSizeListener())
                    .apply(options)
                    .into(imageView);
        } else {
            try {
                byte[] bytes = Base64.decode(item, Base64.DEFAULT);
                Glide.with(getContext())
                        .load(bytes)
                        .listener(imageSizeListener())
                        .apply(options)
                        .into(imageView);
            } catch (IllegalArgumentException ignored) {
                imageView.setImageDrawable(makeRoundRect(Color.argb(28, 39, 69, 58), radius));
            }
        }
    }

    private void showNext() {
        if (slides.isEmpty()) return;
        currentIndex = (currentIndex + 1) % slides.size();
        showCurrent();
    }

    private void showPrevious() {
        if (slides.isEmpty()) return;
        currentIndex = (currentIndex - 1 + slides.size()) % slides.size();
        showCurrent();
    }

    private void showCurrent() {
        showSlide(slides.get(currentIndex));
        updateIndicators();
    }

    // شروع اتوماتیک
    public void start() {
        if (!isRunning && slides.size() > 1) {
            isRunning = true;
            scheduleNextSlide();
        }
    }

    // توقف اتوماتیک
    public void stop() {
        isRunning = false;
        handler.removeCallbacks(slideRunnable);
    }

    private void scheduleNextSlide() {
        handler.removeCallbacks(slideRunnable);
        if (isRunning && slides.size() > 1) {
            handler.postDelayed(slideRunnable, getCurrentSlideDurationMs());
        }
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setRoundLoadedImage(boolean roundLoadedImage) {
        this.roundLoadedImage = roundLoadedImage;
        if (!slides.isEmpty()) showCurrent();
    }

    public void setTestSlidesEnabled(boolean testSlidesEnabled) {
        this.testSlidesEnabled = testSlidesEnabled;
        addTestSlidesIfNeeded();
        updateIndicators();
        stop();
        start();
    }

    public void setBanners(List<BannerViewModel> banners) {
        stop();
        bannerList.clear();
        slides.clear();

        if (banners != null) {
            for (BannerViewModel b : banners) {
                String slide = b.getType() == 1 ? b.getImageURL() : b.getImageBase64();
                if (slide == null || slide.trim().isEmpty()) continue;
                bannerList.add(b);
                slides.add(slide);
            }
        }
        addTestSlidesIfNeeded();

        currentIndex = 0;
        updateIndicators();
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    public BannerViewModel getCurrentBanner() {
        if (bannerList.isEmpty()) return null;
        return bannerList.get(currentIndex);
    }

    private long getCurrentSlideDurationMs() {
        BannerViewModel banner = getCurrentBanner();
        return banner != null ? banner.getDurationMs() : DEFAULT_SLIDE_DURATION_MS;
    }

    private void addTestSlidesIfNeeded() {
        if (!testSlidesEnabled || slides.size() != 1) return;
        String slide = slides.get(0);
        slides.add(slide);
        slides.add(slide);

        if (bannerList.size() == 1) {
            BannerViewModel banner = bannerList.get(0);
            bannerList.add(banner);
            bannerList.add(banner);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resolvedHeight = Math.max(minBannerHeight, measuredBannerHeight);

        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            resolvedHeight = Math.max(minBannerHeight, requestedHeight);
        } else if (mode == MeasureSpec.AT_MOST) {
            resolvedHeight = Math.min(Math.max(minBannerHeight, resolvedHeight), requestedHeight);
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(resolvedHeight, MeasureSpec.EXACTLY));
    }

    private RequestListener<Drawable> imageSizeListener() {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                updateHeightFrom(resource);
                return false;
            }
        };
    }

    private void updateHeightFrom(Drawable drawable) {
        if (drawable == null) return;
        int imageWidth = drawable.getIntrinsicWidth();
        int imageHeight = drawable.getIntrinsicHeight();
        int viewWidth = getWidth();
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0) return;

        int nextHeight = Math.max(minBannerHeight, Math.round(viewWidth * (imageHeight / (float) imageWidth)));
        if (nextHeight != measuredBannerHeight) {
            measuredBannerHeight = nextHeight;
            requestLayout();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean handleTouch(MotionEvent event) {
        if (slides.size() <= 1) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                swiping = false;
                stop();
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    swiping = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (swiping) {
                    float totalDx = event.getX() - downX;
                    if (Math.abs(totalDx) > dp(48)) {
                        if (totalDx < 0) showNext();
                        else showPrevious();
                    }
                    start();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                start();
                performClick();
                return false;
            default:
                return false;
        }
    }

    private void updateIndicators() {
        indicatorLayout.removeAllViews();
        indicatorLayout.setVisibility(slides.size() > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < slides.size(); i++) {
            View dot = new View(getContext());
            boolean active = i == currentIndex;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    active ? dp(18) : dp(7),
                    dp(7)
            );
            params.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(params);
            dot.setBackground(makeRoundRect(
                    active ? Color.argb(240, 255, 255, 255) : Color.argb(130, 255, 255, 255),
                    dp(4)
            ));
            final int index = i;
            dot.setOnClickListener(v -> {
                currentIndex = index;
                showCurrent();
                stop();
                start();
            });
            indicatorLayout.addView(dot);
        }
    }

    private GradientDrawable makeRoundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
