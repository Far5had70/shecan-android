package ir.shecan.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.ServiceItem;
import ir.shecan.databinding.LayoutItemServiceBinding;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private final List<ServiceItem> items;
    private final OnMoreClickListener listener;

    private int selectedPosition = -1;

    public interface OnMoreClickListener {
        void onMoreClicked(ServiceItem item);
    }

    public ServiceAdapter(List<ServiceItem> items, OnMoreClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        LayoutItemServiceBinding binding;

        public ViewHolder(LayoutItemServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ServiceItem item, boolean isSelected, OnMoreClickListener listener) {

            binding.txtOrderCode.setText(item.getOrderCode());
            binding.txtServiceType.setText(item.getServiceType());
            binding.txtStatus.setText(item.getStatusText());
            binding.txtStatus.setTextColor(item.getStatusColor());
            binding.statusBoxIcon.setImageResource(item.getStatusIcon());

            if (isSelected) {
                binding.greenHalfOval.setVisibility(View.VISIBLE);
                binding.getRoot().setBackgroundColor(Color.parseColor("#E3F7F2"));
            } else {
                binding.greenHalfOval.setVisibility(View.INVISIBLE);
                binding.getRoot().setBackgroundColor(Color.WHITE);
            }

            binding.btnOptions.setOnClickListener(v -> listener.onMoreClicked(item));

            binding.getRoot().setOnClickListener(v -> {
                listener.onMoreClicked(item);
            });
        }
    }

    @NonNull
    @Override
    public ServiceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutItemServiceBinding binding = LayoutItemServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceAdapter.ViewHolder holder, int position) {

        boolean isSelected = position == selectedPosition;

        holder.bind(items.get(position), isSelected, item -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();

            listener.onMoreClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
