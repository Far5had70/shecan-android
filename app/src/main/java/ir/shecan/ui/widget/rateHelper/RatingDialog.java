package ir.shecan.ui.widget.rateHelper;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.activity.MainActivityNew;

public class RatingDialog extends Dialog {

    private RatingBar ratingBar;
    private Button btnSubmit;
    private Button btnLater;
    private TextView txtDescription;
    private RatingManager ratingManager;
    private String url;

    private boolean userRated = false;

    public RatingDialog(Context context, String url) {
        super(context);
        ratingManager = new RatingManager(context);
        this.url = url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_rating);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ratingBar = findViewById(R.id.ratingBar);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnLater = findViewById(R.id.btnLater);
        txtDescription = findViewById(R.id.txtDescription);

        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        findViewById(android.R.id.content).startAnimation(fadeIn);

        setOnDismissListener(dialog -> {
            if (!userRated) {
                ratingManager.onDialogDismissed();
            }
        });

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {

            if (rating <= 2) {
                txtDescription.setText("چه چیزی اذیتت کرد؟ بهمون بگو 🙏");
            } else if (rating == 3) {
                txtDescription.setText("مرسی ❤️ چطور می‌تونیم بهترش کنیم؟");
            } else {
                txtDescription.setText("عالیه! خوشحالیم که راضی بودی 😍");
            }
        });

        btnLater.setOnClickListener(v ->{
            if (!userRated) {
                ratingManager.onDialogDismissed();
            }
            dismiss();
        });

        btnSubmit.setOnClickListener(v -> {

            float rating = ratingBar.getRating();

            if (rating == 0) {
                dismiss();
                return;
            }

            userRated = true;

            AppStorage storage = new AppStorage(getContext());
            VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

            if (token != null) {

                long weeklyTime = Shecan.getPrefs()
                        .getLong("weekly_connection_time", 0L);

                ratingManager.onUserRated(
                        url,
                        (int) rating,
                        ShecanVpnService.isProMode(),
                        token.getLogin(),
                        weeklyTime,
                        BuildConfig.VERSION_NAME,
                        Constant.Store
                );
            }

            dismiss();
        });
    }
}