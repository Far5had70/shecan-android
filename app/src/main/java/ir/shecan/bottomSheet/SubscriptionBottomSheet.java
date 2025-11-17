package ir.shecan.bottomSheet;

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

import java.util.Objects;

import ir.shecan.R;
import ir.shecan.databinding.BottomSheetSubscriptionBinding;

public class SubscriptionBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetSubscriptionBinding binding;

    public static SubscriptionBottomSheet newInstance() {
        return new SubscriptionBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSubscriptionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.parseColor("#80000000"))); // 50% مشکی
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.AppTheme;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // نمونه مقداردهی — شما اینها را از ViewModel یا آرگومان‌ها بفرستید
        binding.tvOrderCode.setText("۲۹۳۲۸۶۴");
        binding.tvServiceType.setText("برنزی");
        binding.tvPayCycleBody.setText("ماهانه");
        binding.tvStartDateBody.setText("1404/05/28");
        binding.tvEndDateBody.setText("1404/06/28");
        binding.tvRemainingDesc.setText("۲ روز از سرویس‌ت مونده. همین حالا تمدیدش کن.");

        binding.customProgress.setProgressAnimated(85);

        // دکمه تمدید
        binding.btnRenew.setOnClickListener(v -> {
            // ارسال رویداد به Activity یا ViewModel
            // dismiss(); // در صورت نیاز
        });

        // آیکون شِوِر کلیک — می‌توانید برای بستن شیت استفاده کنید
        binding.ivChevron.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}