package ir.shecan.constant;

public enum DurationType {
    SAL_E2(21, "ساله 2"),
    MAHE_6(19, "ماهه 6"),
    MAHE_9(51, "ماهه 9"),
    MAHE_3(52, "ماهه 3"),
    MAHIANE(50, "ماهیانه"),
    SALANE(17, "سالانه"),
    UNKNOWN(0, "نامشخص");

    private final int id;
    private final String title;

    DurationType(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    // تبدیل ID به Enum
    public static DurationType fromId(int id) {
        for (DurationType d : values()) {
            if (d.id == id) return d;
        }
        return UNKNOWN;
    }
}

