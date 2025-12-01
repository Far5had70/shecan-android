package ir.shecan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.R;
import ir.shecan.modelDto.ServiceItem;
import ir.shecan.databinding.LayoutItemServiceBinding;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private final Context context;
    private final List<ServiceItem> items;
    private final OnMoreClickListener listener;

    private int selectedPosition = -1;

    public interface OnMoreClickListener {
        void onBackgroundClicked(ServiceItem item);
        void onOptionClicked(ServiceItem item);
    }

    public ServiceAdapter(Context context, List<ServiceItem> items, OnMoreClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        LayoutItemServiceBinding binding;

        public ViewHolder(LayoutItemServiceBinding binding) {
            super(binding.root);
            this.binding = binding;
        }

        public void bind(Context context, ServiceItem item, boolean isSelected, OnMoreClickListener listener) {

            binding.txtOrderCode.setText(item.getOrderCode().equals("0") ? "-" : item.getOrderCode());
            binding.txtServiceType.setText(item.getServiceType());
            binding.txtStatus.setText(item.getOrderCode().equals("0") ? context.getString(R.string.readyToConnect) : item.getStatusText());
            binding.txtStatus.setTextColor(item.getStatusColor());
            binding.statusBoxIcon.setImageResource(item.getStatusIcon());

            if (isSelected) {
                binding.greenHalfOval.setVisibility(View.VISIBLE);
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.lightBack));
            } else {
                binding.greenHalfOval.setVisibility(View.INVISIBLE);
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            }

            binding.btnOptions.setOnClickListener(v -> listener.onBackgroundClicked(item));

            binding.root.setOnClickListener(v -> {
                listener.onBackgroundClicked(item);
            });
            binding.btnOptions.setOnClickListener(v -> {
                listener.onOptionClicked(item);
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

        holder.bind(context, items.get(position), isSelected, new OnMoreClickListener() {
            @Override
            public void onBackgroundClicked(ServiceItem item) {
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
                listener.onBackgroundClicked(item);
            }

            @Override
            public void onOptionClicked(ServiceItem item) {
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
                listener.onOptionClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setSelectedPosition(int pos) {
        this.selectedPosition = pos;
        notifyDataSetChanged();
    }
}
