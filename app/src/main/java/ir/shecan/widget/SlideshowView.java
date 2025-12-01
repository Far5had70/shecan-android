package ir.shecan.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.modelDto.BannerViewModel;

public class SlideshowView extends RelativeLayout {

    private ImageView imageView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> slides = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;

    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            showNext();
            handler.postDelayed(this, 5000); // هر 5 ثانیه تغییر کنه
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
        imageView = new ImageView(getContext());
        imageView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );
//        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(imageView);
    }

    // گرفتن لیست عکس‌ها
    public void setSlides(List<String> slideList) {
        slides.clear();
        slides.addAll(slideList);
        currentIndex = 0;
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    private void showSlide(String item) {
        if (item.startsWith("http")) {
            // URL mode
            Glide.with(getContext())
                    .load(item)
                    .into(imageView);
        } else {
            // Base64 mode
            byte[] bytes = Base64.decode(item, Base64.DEFAULT);

            Glide.with(getContext())
                    .load(bytes)
                    .into(imageView);
        }
    }

    private void showNext() {
        if (slides.isEmpty()) return;
        currentIndex = (currentIndex + 1) % slides.size();
        showSlide(slides.get(currentIndex));
    }

    // شروع اتوماتیک
    public void start() {
        if (!isRunning && slides.size() > 1) {
            isRunning = true;
            handler.postDelayed(slideRunnable, 5000);
        }
    }

    // توقف اتوماتیک
    public void stop() {
        isRunning = false;
        handler.removeCallbacks(slideRunnable);
    }

    public ImageView getImageView() {
        return imageView;
    }

    private final List<BannerViewModel> bannerList = new ArrayList<>();

    public void setBanners(List<BannerViewModel> banners) {
        bannerList.clear();
        bannerList.addAll(banners);

        slides.clear();
        for (BannerViewModel b : banners) {
            if (b.getType() == 1) slides.add(b.getImageURL());
            else slides.add(b.getImageBase64());
        }

        currentIndex = 0;
        if (!slides.isEmpty()) showSlide(slides.get(0));
    }

    public BannerViewModel getCurrentBanner() {
        if (bannerList.isEmpty()) return null;
        return bannerList.get(currentIndex);
    }
}
