package ir.shecan.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import ir.shecan.R;

public class CustomButton extends AppCompatButton {

    public enum Type { PRIMARY, SECONDARY }

    private int primaryBg;
    private int primaryText;
    private int secondaryBg;
    private int secondaryBorder;
    private int secondaryText;
    private float radius;
    private int defaultPadding;

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttrs(context, attrs);
    }

    private void loadAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomButton);

        int typeValue = a.getInt(R.styleable.CustomButton_type, 0);
        Type type = typeValue == 0 ? Type.PRIMARY : Type.SECONDARY;

        primaryBg = a.getColor(R.styleable.CustomButton_cb_primaryBackground, 0xFF0BA36A);
        primaryText = a.getColor(R.styleable.CustomButton_cb_primaryTextColor, 0xFFFFFFFF);

        secondaryBg = a.getColor(R.styleable.CustomButton_cb_secondaryBackground, 0x00000000);
        secondaryBorder = a.getColor(R.styleable.CustomButton_cb_secondaryBorderColor, 0xFF0BA36A);
        secondaryText = a.getColor(R.styleable.CustomButton_cb_secondaryTextColor, 0xFF0BA36A);

        radius = a.getDimension(R.styleable.CustomButton_cb_cornerRadius, 40f);
        defaultPadding = (int) a.getDimension(R.styleable.CustomButton_cb_defaultPadding, 30);

        a.recycle();

        init(type);
    }

    private GradientDrawable createDrawable(Type type) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);

        if (type == Type.PRIMARY) {
            drawable.setColor(primaryBg);
            drawable.setStroke(0, 0);
        } else {
            drawable.setColor(secondaryBg);
            drawable.setStroke(3, secondaryBorder);
        }
        return drawable;
    }

    private void init(Type type) {
        GradientDrawable shape = createDrawable(type);

        // ripple
        int rippleColor = 0x22000000;
        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                shape,
                null
        );

        setBackground(ripple);

        setAllCaps(false);
        setPadding(defaultPadding, defaultPadding / 2, defaultPadding, defaultPadding / 2);

        if (type == Type.PRIMARY) {
            setTextColor(primaryText);
        } else {
            setTextColor(secondaryText);
        }
    }
}
