package ir.shecan.core.monitoring;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.MonitoringApi;
import ir.shecan.data.modelDto.monitoring.MonitoringLog;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsRequest;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsResponse;
import ir.shecan.data.modelDto.monitoring.MonitoringTarget;
import ir.shecan.data.modelDto.monitoring.MonitoringTargetsResponse;

public class MonitoringManager {
    private static final String TAG = "MonitoringManager";
    private static final int DEFAULT_INTERVAL_SECONDS = 300;

    private final Context context;
    private final MonitoringApi api;
    private final MonitoringConnectivity connectivity;
    private final MonitoringIdentity identity;
    private final MonitoringChecks checks;
    private final AtomicBoolean runningChecks = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;
    private volatile List<MonitoringTarget> targets = new ArrayList<>();
    private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private volatile boolean active;

    public MonitoringManager(Context context) {
        this.context = context.getApplicationContext();
        this.api = new MonitoringApi(this.context);
        this.connectivity = new MonitoringConnectivity(this.context);
        this.identity = new MonitoringIdentity(this.context);
        this.checks = new MonitoringChecks(connectivity, identity);
    }

    public synchronized void start() {
        active = true;
        fetchTargets();
    }

    public synchronized void stop() {
        active = false;
        runningChecks.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void fetchTargets() {
        if (!connectivity.isOnline()) return;

        api.targets(new ApiCallback<MonitoringTargetsResponse>() {
            @Override
            public void onSuccess(MonitoringTargetsResponse response, boolean fromCache) {
                if (!active || response == null) return;
                targets = response.getTargets();
                intervalSeconds = Math.max(60, response.getIntervalSeconds());
                scheduleLoop();
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.d(TAG, "Monitoring targets unavailable: " + statusCode + " " + message);
            }
        });
    }

    private synchronized void scheduleLoop() {
        if (!active || targets.isEmpty()) return;

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(
                this::runOnceSafely,
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    private void runOnceSafely() {
        if (!active || !connectivity.isOnline()) return;
        if (!runningChecks.compareAndSet(false, true)) return;

        try {
            List<MonitoringLog> logs = new ArrayList<>();
            for (MonitoringTarget target : targets) {
                if (!active) return;
                if (target == null || target.getType() == null) continue;
                logs.add(checks.run(target));
            }
            if (!logs.isEmpty()) sendBatch(logs);
        } catch (Exception e) {
            Log.e(TAG, "Monitoring run failed", e);
        } finally {
            runningChecks.set(false);
        }
    }

    private void sendBatch(List<MonitoringLog> logs) {
        MonitoringLogsRequest request = new MonitoringLogsRequest(
                identity.hashedDeviceId(),
                identity.appVersion(),
                "android",
                connectivity.networkInfo(),
                logs
        );

        api.sendLogs(request, new ApiCallback<MonitoringLogsResponse>() {
            @Override
            public void onSuccess(MonitoringLogsResponse response, boolean fromCache) {
                Log.d(TAG, "Monitoring logs sent");
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.d(TAG, "Monitoring logs rejected: " + statusCode + " " + message);
            }
        });
    }
}
