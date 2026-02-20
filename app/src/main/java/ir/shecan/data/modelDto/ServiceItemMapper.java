package ir.shecan.data.modelDto;

import android.content.Context;
import androidx.core.content.ContextCompat;
import ir.shecan.R;
import ir.shecan.core.constant.DurationType;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.core.constant.ServiceType;

public class ServiceItemMapper {

    public static ServiceItem map(Context context, IssuesViewModel.IssuesDTO dto) {

        String orderCode = String.valueOf(dto.getId());

        int serviceTypeId = getCustomFieldInt(dto, 58);
        ServiceType serviceType = ServiceType.fromId(serviceTypeId);

        int durationId = getCustomFieldInt(dto, 21);
        DurationType durationType = DurationType.fromId(durationId);

        String updateLink = getCustomFieldString(dto, 95);

        RequestStatus status = RequestStatus.fromValue(dto.getStatus().getId());
        if (status == null) status = RequestStatus.READY_TO_CONNECT;

        String statusText = getStatusTitle(context, status);
        int statusIcon = getStatusIcon(status);
        int statusColor = getStatusColor(context, status);

        // ساخت ServiceItem
        ServiceItem item = new ServiceItem(
                orderCode,
                serviceType != null ? serviceType.getTitle() : "",
                statusText,
                updateLink,
                statusIcon,
                statusColor,
                dto
        );

        // 🎉‌ ست کردن تمام فیلدهای جدید
        item.id = dto.getId();

        item.projectId = dto.getProject().getId();
        item.projectName = dto.getProject().getName();

        item.trackerId = dto.getTracker().getId();
        item.trackerName = dto.getTracker().getName();

        item.statusId = dto.getStatus().getId();
        item.statusName = dto.getStatus().getName();

        item.priorityId = dto.getPriority().getId();
        item.priorityName = dto.getPriority().getName();

        item.authorId = dto.getAuthor().getId();
        item.authorName = dto.getAuthor().getName();

        item.subject = dto.getSubject();
        item.description = dto.getDescription();
        item.startDate = dto.getStartDate();
        item.dueDate = dto.getDueDate();
        item.doneRatio = dto.getDoneRatio();
        item.isPrivate = dto.isIsPrivate();
        item.createdOn = dto.getCreatedOn();
        item.updatedOn = dto.getUpdatedOn();
        item.closedOn = dto.getClosedOn();

        // custom fields
        item.cfDuration = getCustomFieldInt(dto, 21);
        item.cfNameFa = getCustomFieldString(dto, 5);
        item.cfFamilyFa = getCustomFieldString(dto, 25);
        item.cfMobile = getCustomFieldString(dto, 18);
        item.cfEmail = getCustomFieldString(dto, 4);
        item.cfWebsite = getCustomFieldString(dto, 36);
        item.cfServiceType = getCustomFieldInt(dto, 58);
        item.cfUpdateLink = getCustomFieldString(dto, 95);

        return item;
    }

    private static int getCustomFieldInt(IssuesViewModel.IssuesDTO dto, int id) {
        if (dto.getCustomFields() == null) return 0;
        for (IssuesViewModel.IssuesDTO.CustomFieldsDTO c : dto.getCustomFields()) {
            if (c.getId() == id) {
                try { return Integer.parseInt(c.getValue()); }
                catch (Exception e) { return 0; }
            }
        }
        return 0;
    }

    private static String getCustomFieldString(IssuesViewModel.IssuesDTO dto, int id) {
        if (dto.getCustomFields() == null) return "";
        for (IssuesViewModel.IssuesDTO.CustomFieldsDTO c : dto.getCustomFields()) {
            if (c.getId() == id) {
                return c.getValue();
            }
        }
        return "";
    }

    private static String getStatusTitle(Context context, RequestStatus status) {
        if (context == null) return "نامشخص";

        try {
            switch (status) {
                case READY_TO_CONNECT: return context.getString(R.string.readyToConnect);
                case ACTIVE: return context.getString(R.string.active);
                case EXPIRING: return context.getString(R.string.expiring);
                case WAITING_FOR_ACTIVATION: return context.getString(R.string.waitingForActivate);
                case NEW: return context.getString(R.string.newConnection);
                default: return "نامشخص";
            }
        } catch (Exception e) {
            return "نامشخص";
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