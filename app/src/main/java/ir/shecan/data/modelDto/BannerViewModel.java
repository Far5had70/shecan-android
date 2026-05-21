package ir.shecan.data.modelDto;

import com.google.gson.annotations.SerializedName;

public class BannerViewModel {

    private static final long DEFAULT_DURATION_MS = 5000L;

    private int type;
    private int order;
    private String url;
    private String imageURL;
    private String imageBase64;
    @SerializedName(value = "duration", alternate = {
            "duration_seconds",
            "durationSeconds",
            "display_duration",
            "displayDuration",
            "display_seconds",
            "displaySeconds",
            "slide_duration",
            "slideDuration",
            "interval",
            "interval_seconds",
            "intervalSeconds"
    })
    private Long duration;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public long getDurationMs() {
        if (duration == null || duration <= 0L) return DEFAULT_DURATION_MS;
        return duration <= 120L ? duration * 1000L : duration;
    }
}
