package ir.shecan.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ir.shecan.databinding.SelectedConfigViewBinding;
import ir.shecan.data.modelDto.ServiceItem;
import saman.zamani.persiandate.PersianDate;
import saman.zamani.persiandate.PersianDateFormat;

public class ServiceStatusView extends ConstraintLayout {

    private SelectedConfigViewBinding binding;

    public ServiceStatusView(Context context) {
        super(context);
        init(context);
    }

    public ServiceStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ServiceStatusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        binding = SelectedConfigViewBinding.inflate(LayoutInflater.from(context), this, true);
    }

//    // -------------------------------------------
//    // Data Model
//    // -------------------------------------------
//    public static class ServiceStatus {
//        public String serviceType;
//        public boolean isPurchased;
//        public String orderCode;
//        public String expireDate;
//        public String updateLink;
//
//        // Purchased
//        public ServiceStatus(String serviceType, String orderCode, String expireDate, String updateLink) {
//            this.serviceType = serviceType;
//            this.orderCode = orderCode;
//            this.expireDate = expireDate;
//            this.updateLink = updateLink;
//            this.isPurchased = true;
//        }
//
//        // Free
//        public ServiceStatus(String serviceType) {
//            this.serviceType = serviceType;
//            this.isPurchased = false;
//        }
//    }

    // -------------------------------------------
    // Set status
    // -------------------------------------------
    public void setStatus(ServiceItem status) throws ParseException {

        binding.valueService.setText(status.getServiceType());

        if (status.getModel().getId() > 0) {
            binding.colOrder.setVisibility(View.VISIBLE);
            binding.colExpire.setVisibility(View.VISIBLE);

            binding.valueOrder.setText(status.getOrderCode() != null ? status.getOrderCode() : "-");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(status.getModel().getDueDate());

            PersianDate pDate = new PersianDate(date);
            PersianDateFormat pdFormat = new PersianDateFormat("Y/m/d");

            String shamsi = pdFormat.format(pDate);
            binding.valueExpire.setText(shamsi != null ? shamsi : "-");

        } else {
            binding.colOrder.setVisibility(View.GONE);
            binding.colExpire.setVisibility(View.GONE);
        }
    }

    public boolean isPurchased() {
        return binding.colOrder.getVisibility() == VISIBLE;
    }
}