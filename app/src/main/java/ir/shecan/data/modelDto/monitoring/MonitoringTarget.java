package ir.shecan.data.modelDto.monitoring;

public class MonitoringTarget {
    private String id;
    private String type;
    private String ip;
    private Integer port;
    private String domain;
    private String url;
    private Integer timeoutMs;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public String getDomain() {
        return domain;
    }

    public String getUrl() {
        return url;
    }

    public int getTimeoutMs(int fallback) {
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : fallback;
    }
}
