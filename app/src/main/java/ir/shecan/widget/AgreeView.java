package ir.shecan.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ir.shecan.databinding.ViewAgreeBinding;

public class AgreeView extends LinearLayout {

    private ViewAgreeBinding binding;

    public AgreeView(Context context) {
        super(context);
        init(context);
    }

    public AgreeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AgreeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        binding = ViewAgreeBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public void setupText(String url) {
        String fullText = "با ادامه، شما با شرایط خدمات و سیاست حفظ حریم خصوصی شکن موافقت می‌کنید.";
        String linkPart = "خدمات و سیاست حفظ حریم خصوصی";

        SpannableString ss = new SpannableString(fullText);

        int start = fullText.indexOf(linkPart);
        int end = start + linkPart.length();

        if (start == -1) {
            binding.tvAgree.setText(fullText);
            return;
        }

        // Clickable span for link
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                } catch (Exception ignored) {}
            }
        };

        // Set clickable + underline
        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.tvAgree.setText(ss);
        binding.tvAgree.setMovementMethod(LinkMovementMethod.getInstance());
        binding.tvAgree.setHighlightColor(getResources().getColor(android.R.color.transparent));
    }
}