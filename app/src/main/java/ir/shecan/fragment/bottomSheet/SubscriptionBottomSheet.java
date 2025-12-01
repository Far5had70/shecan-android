package ir.shecan.fragment.bottomSheet;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import ir.shecan.R;
import ir.shecan.constant.Constant;
import ir.shecan.constant.DurationType;
import ir.shecan.constant.RequestStatus;
import ir.shecan.databinding.BottomSheetSubscriptionBinding;
import ir.shecan.modelDto.ServiceItem;
import ir.shecan.util.AppUtils;
import saman.zamani.persiandate.PersianDate;
import saman.zamani.persiandate.PersianDateFormat;

public class SubscriptionBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetSubscriptionBinding binding;
    private ServiceItem item;

    public SubscriptionBottomSheet(ServiceItem item) {
        this.item = item;
    }

    public static SubscriptionBottomSheet newInstance(ServiceItem item) {
        return new SubscriptionBottomSheet(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSubscriptionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.AppTheme;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null)
            item = (ServiceItem) getArguments().getSerializable("item");

        if (item == null) return;

        bindUi();
    }

    private void bindUi() {
        // تاریخ شمسی
        String startShamsi = convertToShamsi(item.startDate);
        String endShamsi = convertToShamsi(item.dueDate);

        // محاسبات
        long[] totalLeft = calculateTotalAndLeftDays(item.startDate, item.dueDate);
        long totalDays = totalLeft[0];
        long daysLeft = totalLeft[1];

        int percentUsed = calculateUsedPercent(totalDays, daysLeft, item.doneRatio);
        int percentRemaining = Math.max(0, 100 - percentUsed);

        // ست کردن UI از مدل
        binding.tvOrderCodeBody.setText(item.getOrderCode());
        binding.tvServiceTypeBody.setText(item.getServiceType());
        binding.tvCompanyNameBody.setText(item.projectName != null ? item.projectName : "-");

        // تبدیل دوره با DurationType
        binding.tvPayCycleBody.setText(
                item.cfDuration != null ?
                        DurationType.fromId(item.cfDuration).getTitle() : "نامشخص"
        );

        binding.tvStartDateBody.setText(startShamsi);
        binding.tvEndDateBody.setText(endShamsi);

        // وضعیت سرویس
        binding.txtStatus.setText(item.getStatusText() != null ? item.getStatusText() : "");
        try {
            binding.txtStatus.setTextColor(item.getStatusColor());
        } catch (Exception ignored) {
        }
        try {
            binding.statusBoxIcon.setImageResource(item.getStatusIcon());
        } catch (Exception ignored) {
        }

        // متن باقی‌مانده
        String pretty = buildRemainingMessage(totalDays, daysLeft, item.cfDuration);
        binding.tvRemainingDesc.setText(pretty + "\n" + "• " + percentUsed + "% استفاده شده");

        // زمان باقی‌مانده دقیق
        binding.remainingTimeBody.setText(formatTimeLeft(item.dueDate));

        // Progressbar
        binding.customProgress.setProgressAnimated(percentRemaining);

        binding.btnRenew.setOnClickListener(v -> {
            if (item.getUpdateLink() != null && !item.getUpdateLink().isEmpty()) {
                // TODO: open link
            }
        });

        binding.ivChevron.setOnClickListener(v -> dismiss());

        binding.btnSupport.setOnClickListener(view -> {
            AppUtils.openUrl(Constant.TicketUrl, getActivity());
        });

        binding.btnRenew.setOnClickListener(view -> {
            AppUtils.openUrl(Constant.PlanUrl, getActivity());
        });

        binding.btnBuyService.setOnClickListener(view -> {
            AppUtils.openUrl(Constant.PlanUrl, getActivity());
        });

        binding.btnCheckAgain.setOnClickListener(view -> {
            AppUtils.openUrl(Constant.PlanUrl, getActivity());
        });

        if (item.statusId == RequestStatus.SUPPORT_FINISHED.getValue()) {
            binding.cardBuyService.setVisibility(VISIBLE);
            binding.cardWaitingForActivation.setVisibility(GONE);
            binding.cardRemaining.setVisibility(GONE);
        } else if (item.statusId == RequestStatus.WAITING_FOR_ACTIVATION.getValue()) {
            binding.cardBuyService.setVisibility(GONE);
            binding.cardWaitingForActivation.setVisibility(VISIBLE);
            binding.cardRemaining.setVisibility(VISIBLE);
        } else {
            binding.cardBuyService.setVisibility(GONE);
            binding.cardWaitingForActivation.setVisibility(GONE);
            binding.cardRemaining.setVisibility(VISIBLE);
        }
    }

    // تبدیل میلادی به شمسی
    private String convertToShamsi(String gregorian) {
        try {
            if (gregorian == null || gregorian.isEmpty()) return "-";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(gregorian);
            if (date == null) return "-";

            PersianDate pDate = new PersianDate(date);
            PersianDateFormat format = new PersianDateFormat("Y/m/d");
            return format.format(pDate);

        } catch (Exception e) {
            return "-";
        }
    }

    // محاسبه totalDays و daysLeft
    private long[] calculateTotalAndLeftDays(String startGregorian, String endGregorian) {
        try {
            if (startGregorian == null || endGregorian == null)
                return new long[]{-1, -1};

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date start = sdf.parse(startGregorian);
            Date end = sdf.parse(endGregorian);

            if (start == null || end == null)
                return new long[]{-1, -1};

            long total = end.getTime() - start.getTime();
            long left = end.getTime() - System.currentTimeMillis();

            long totalDays = Math.max(0, TimeUnit.MILLISECONDS.toDays(total));
            long daysLeft = Math.max(0, TimeUnit.MILLISECONDS.toDays(left));

            return new long[]{totalDays, daysLeft};

        } catch (Exception e) {
            return new long[]{-1, -1};
        }
    }

    // درصد مصرف‌شده
    private int calculateUsedPercent(long totalDays, long daysLeft, int ratioFallback) {
        if (totalDays > 0 && daysLeft >= 0) {
            long used = totalDays - daysLeft;
            int percent = (int) ((used * 100f) / totalDays);
            return Math.min(100, Math.max(0, percent));
        }
        return Math.min(100, Math.max(0, ratioFallback));
    }

    // متن کامل باقی‌مانده
    private String buildRemainingMessage(long totalDays, long daysLeft, Integer duration) {

        if (daysLeft <= 0)
            return "سرویس شما منقضی شده! برای جلوگیری از قطعی، همین حالا تمدید کنید.";

        if (daysLeft <= 3)
            return daysLeft + " روز از سرویست مونده. همین حالا تمدیدش کن.";

        return daysLeft + " روز از سرویست مونده. می‌تونی برای مدت بیشتر رزرو کنی.";
    }

    // نمایش: "X روز Y ساعت Z دقیقه"
    private String formatTimeLeft(String endGregorian) {
        try {
            if (endGregorian == null || endGregorian.isEmpty()) return "-";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date end = sdf.parse(endGregorian);
            if (end == null) return "-";

            long diff = end.getTime() - System.currentTimeMillis();
            if (diff <= 0) return "۰ روز";

            long days = TimeUnit.MILLISECONDS.toDays(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff) - TimeUnit.DAYS.toHours(days);
            long mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                    - TimeUnit.DAYS.toMinutes(days)
                    - TimeUnit.HOURS.toMinutes(hours);

            StringBuilder sb = new StringBuilder();
            if (days > 0) sb.append(days).append(" روز ");
            if (hours > 0) sb.append(hours).append(" ساعت ");
            if (mins > 0) sb.append(mins).append(" دقیقه ");
            if (sb.length() == 0) sb.append("کمتر از یک دقیقه");

            return sb.toString().trim();

        } catch (Exception e) {
            return "-";
        }
    }
}
