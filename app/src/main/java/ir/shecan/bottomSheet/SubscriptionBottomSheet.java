package ir.shecan.bottomSheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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