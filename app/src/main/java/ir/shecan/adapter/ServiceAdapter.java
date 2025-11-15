package ir.shecan.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.ServiceItem;
import ir.shecan.databinding.LayoutItemServiceBinding;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private final List<ServiceItem> items;
    private final OnMoreClickListener listener;

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

        public void bind(ServiceItem item, OnMoreClickListener listener) {

            binding.txtOrderCode.setText(item.getOrderCode());
            binding.txtServiceType.setText(item.getServiceType());

            binding.txtStatus.setText(item.getStatusText());
            binding.txtStatus.setTextColor(item.getStatusColor());

            binding.statusBoxIcon.setImageResource(item.getStatusIcon());

            binding.btnOptions.setOnClickListener(v -> listener.onMoreClicked(item));
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
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
