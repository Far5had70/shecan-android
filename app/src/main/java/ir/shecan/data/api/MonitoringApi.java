package ir.shecan.data.api;

import android.content.Context;

import ir.shecan.data.modelDto.monitoring.MonitoringLogsRequest;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsResponse;
import ir.shecan.data.modelDto.monitoring.MonitoringTargetsResponse;

public class MonitoringApi {
    private static final String BASE_URL = "https://my.shecan.ir";

    private final ApiRepository repo;

    public MonitoringApi(Context context) {
        repo = new ApiRepository(context);
    }

    public void targets(ApiCallback<MonitoringTargetsResponse> callback) {
        repo.request(
                "monitoring_targets",
                null,
                BASE_URL + "/monitoring/targets",
                HttpMethod.GET,
                false,
                callback,
                MonitoringTargetsResponse.class
        );
    }

    public void sendLogs(MonitoringLogsRequest request, ApiCallback<MonitoringLogsResponse> callback) {
        repo.request(
                "monitoring_logs_" + System.currentTimeMillis(),
                request,
                BASE_URL + "/monitoring/logs",
                HttpMethod.POST,
                false,
                callback,
                MonitoringLogsResponse.class
        );
    }
}
//
//
//package ir.shecan.data.api;
//
//import android.content.Context;
//
//import ir.shecan.data.modelDto.monitoring.MonitoringLogsRequest;
//import ir.shecan.data.modelDto.monitoring.MonitoringLogsResponse;
//import ir.shecan.data.modelDto.monitoring.MonitoringTargetsResponse;
//
//public class MonitoringApi {
//    private static final String TARGETS_URL = "https://n8n.coolify.shcn.ir/webhook/monitoring/targets";
//    private static final String LOGS_URL = "https://n8n.coolify.shcn.ir/webhook/monitoring/logs";
//
//    private final ApiRepository repo;
//
//    public MonitoringApi(Context context) {
//        repo = new ApiRepository(context);
//    }
//
//    public void targets(ApiCallback<MonitoringTargetsResponse> callback) {
//        repo.request(
//                "monitoring_targets",
//                null,
//                TARGETS_URL,
//                HttpMethod.GET,
//                false,
//                callback,
//                MonitoringTargetsResponse.class
//        );
//    }
//
//    public void sendLogs(MonitoringLogsRequest request, ApiCallback<MonitoringLogsResponse> callback) {
//        repo.request(
//                "monitoring_logs_" + System.currentTimeMillis(),
//                request,
//                LOGS_URL,
//                HttpMethod.POST,
//                false,
//                callback,
//                MonitoringLogsResponse.class
//        );
//    }
//}
