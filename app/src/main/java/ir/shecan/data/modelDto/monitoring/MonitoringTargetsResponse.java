package ir.shecan.data.modelDto.monitoring;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTargetsResponse {
    private int version;
    private Integer intervalSeconds;
    private List<MonitoringTarget> targets;

    public int getVersion() {
        return version;
    }

    public int getIntervalSeconds() {
        return intervalSeconds != null && intervalSeconds > 0 ? intervalSeconds : 300;
    }

    public List<MonitoringTarget> getTargets() {
        return targets != null ? targets : new ArrayList<>();
    }
}
