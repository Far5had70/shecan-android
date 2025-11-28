package ir.shecan.modelDto;

import android.content.Context;

import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.constant.DurationType;
import ir.shecan.constant.RequestStatus;
import ir.shecan.constant.ServiceType;
import ir.shecan.modelDto.IssuesViewModel;

public class ServiceItemMapper {

    public static ServiceItem map(Context context, IssuesViewModel.IssuesDTO dto) {

        String orderCode = String.valueOf(dto.getId());

        // استخراج نوع سرویس از Custom Fields → id = 58
        int serviceTypeId = getCustomFieldInt(dto, 58);
        ServiceType serviceType = ServiceType.fromId(serviceTypeId);

        // استخراج مدت سرویس از Custom Fields → id = 21
        int durationId = getCustomFieldInt(dto, 21);
        DurationType durationType = DurationType.fromId(durationId);

        // استخراج وضعیت
        RequestStatus status = RequestStatus.fromValue(dto.getStatus().getId());
        if (status == null){
            status = RequestStatus.READY_TO_CONNECT;
        }

        // تبدیل وضعیت به متن + آیکون + رنگ
        String statusText = getStatusTitle(context, status);
        int statusIcon = getStatusIcon(status);
        int statusColor = getStatusColor(context, status);

        return new ServiceItem(
                orderCode,
                serviceType.getTitle(),
                statusText,
                statusIcon,
                statusColor,
                dto
        );
    }

    private static int getCustomFieldInt(IssuesViewModel.IssuesDTO dto, int id) {
        if (dto.getCustomFields() == null) return 0;
        return dto.getCustomFields().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValue());
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    private static String getStatusTitle(Context context, RequestStatus status) {
        switch (status) {
            case READY_TO_CONNECT: return context.getString(R.string.readyToConnect);
            case ACTIVE: return context.getString(R.string.active);
            case EXPIRING: return context.getString(R.string.expiring);
            case WAITING_FOR_ACTIVATION: return context.getString(R.string.waitingForActivate);
            case NEW: return context.getString(R.string.newConnection);
            default: return "نامشخص";
        }
    }

    private static int getStatusIcon(RequestStatus status) {
        switch (status) {
            case ACTIVE: return R.drawable.ic_connect;
            case EXPIRING: return R.drawable.ic_alert;
            case WAITING_FOR_ACTIVATION: return R.drawable.ic_watch;
            case NEW: return R.drawable.ic_new;
            default: return R.drawable.ic_done;
        }
    }

    private static int getStatusColor(Context context, RequestStatus status) {
        switch (status) {
            case ACTIVE: return ContextCompat.getColor(context, R.color.connectionIsActiveColor);
            case EXPIRING: return ContextCompat.getColor(context, R.color.colorAccent);
            case WAITING_FOR_ACTIVATION: return ContextCompat.getColor(context, R.color.primaryTextColor);
            case NEW: return ContextCompat.getColor(context, R.color.primaryTextColor);
            default: return ContextCompat.getColor(context, R.color.primaryTextColor);
        }
    }
}