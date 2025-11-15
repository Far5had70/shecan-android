package ir.shecan;

public class ServiceItem {
    private final String orderCode;
    private final String serviceType;
    private final String statusText;
    private final int statusIcon;
    private final int statusColor;

    public ServiceItem(String orderCode, String serviceType, String statusText, int statusIcon, int statusColor) {
        this.orderCode = orderCode;
        this.serviceType = serviceType;
        this.statusText = statusText;
        this.statusIcon = statusIcon;
        this.statusColor = statusColor;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getStatusText() {
        return statusText;
    }

    public int getStatusIcon() {
        return statusIcon;
    }

    public int getStatusColor() {
        return statusColor;
    }
}
