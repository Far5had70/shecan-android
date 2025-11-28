package ir.shecan.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.databinding.BottomItemBinding;
import ir.shecan.databinding.CustomBottomBarBinding;

public class CustomBottomBar extends LinearLayout {

    private CustomBottomBarBinding binding;
    private final List<Item> items = new ArrayList<>();
    private int selectedIndex = -1;

    public interface OnItemSelected {
        void onSelect(int index);
    }

    private OnItemSelected listener;

    public void setOnItemSelected(OnItemSelected listener) {
        this.listener = listener;
    }

    public CustomBottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = CustomBottomBarBinding.inflate(inflater, this, true);

        setOrientation(HORIZONTAL);
    }

    public void addItem(String title, int inactiveRes, int activeRes) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        BottomItemBinding itemBinding =
                BottomItemBinding.inflate(inflater, binding.getRoot(), false);

        Item item = new Item(itemBinding, inactiveRes, activeRes, title);
        items.add(item);

        itemBinding.label.setText(title);
        itemBinding.icon.setImageResource(inactiveRes);

        int index = items.size() - 1;

        itemBinding.getRoot().setOnClickListener(v -> {
            select(index);
            if (listener != null) listener.onSelect(index);
        });

        binding.getRoot().addView(itemBinding.getRoot());
    }

    public void select(int index) {
        selectedIndex = index;

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);

            if (i == index) {
                it.binding.icon.setImageResource(it.activeRes);

                // Scale 3X
                it.binding.icon.animate().scaleX(3f).scaleY(3f).setDuration(0).start();

                it.binding.label.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.bottomNavigationActiveTextColor)
                );
            } else {
                it.binding.icon.setImageResource(it.inactiveRes);

                // Scale back to normal
                it.binding.icon.animate().scaleX(1f).scaleY(1f).setDuration(0).start();

                it.binding.label.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.bottomNavigationInactiveTextColor)
                );
            }
        }
    }


    private static class Item {
        BottomItemBinding binding;
        int inactiveRes;
        int activeRes;
        String title;

        Item(BottomItemBinding binding, int inactiveRes, int activeRes, String title) {
            this.binding = binding;
            this.inactiveRes = inactiveRes;
            this.activeRes = activeRes;
            this.title = title;
        }
    }
}

