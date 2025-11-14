package ir.shecan.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import ir.shecan.databinding.SelectedConfigViewBinding;

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

    // -------------------------------------------
    // Data Model
    // -------------------------------------------
    public static class ServiceStatus {
        public String serviceType;
        public boolean isPurchased;
        public String orderCode;
        public String expireDate;

        // Purchased
        public ServiceStatus(String serviceType, String orderCode, String expireDate) {
            this.serviceType = serviceType;
            this.orderCode = orderCode;
            this.expireDate = expireDate;
            this.isPurchased = true;
        }

        // Free
        public ServiceStatus(String serviceType) {
            this.serviceType = serviceType;
            this.isPurchased = false;
        }
    }

    // -------------------------------------------
    // Set status
    // -------------------------------------------
    public void setStatus(ServiceStatus status) {

        binding.valueService.setText(status.serviceType);

        if (status.isPurchased) {
            binding.colOrder.setVisibility(View.VISIBLE);
            binding.colExpire.setVisibility(View.VISIBLE);

            binding.valueOrder.setText(status.orderCode != null ? status.orderCode : "-");
            binding.valueExpire.setText(status.expireDate != null ? status.expireDate : "-");

        } else {
            binding.colOrder.setVisibility(View.GONE);
            binding.colExpire.setVisibility(View.GONE);
        }
    }

    public boolean isPurchased() {
        return binding.colOrder.getVisibility() == VISIBLE;
    }
}