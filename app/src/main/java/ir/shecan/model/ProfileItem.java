package ir.shecan.model;

public class ProfileItem {
    private final int iconRes;
    private final String title;

    public ProfileItem(int iconRes, String title) {
        this.iconRes = iconRes;
        this.title = title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getTitle() {
        return title;
    }
}
