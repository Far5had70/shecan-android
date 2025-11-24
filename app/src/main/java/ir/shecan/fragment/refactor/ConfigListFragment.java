package ir.shecan.fragment.refactor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.ServiceItem;
import ir.shecan.adapter.ServiceAdapter;
import ir.shecan.bottomSheet.SubscriptionBottomSheet;
import ir.shecan.databinding.FragmentConfigListBinding;
import ir.shecan.fragment.ToolbarFragment;

public class ConfigListFragment extends ToolbarFragment {

    private FragmentConfigListBinding binding;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigListBinding.inflate(inflater, container, false);

        List<ServiceItem> serviceItems = new ArrayList<>();
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.bronze), getString(R.string.newConnection), R.drawable.ic_new, ContextCompat.getColor(getContext(), R.color.serviceRowBodyColor)));
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.bronze), getString(R.string.expiring), R.drawable.ic_alert, ContextCompat.getColor(getContext(), R.color.colorAccent)));
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.bronze), getString(R.string.waitingForActivate), R.drawable.ic_watch, ContextCompat.getColor(getContext(), R.color.serviceRowBodyColor)));
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.bronze), getString(R.string.active), R.drawable.ic_connect, ContextCompat.getColor(getContext(), R.color.connectionIsActiveColor)));
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.gold), getString(R.string.readyToConnect), R.drawable.ic_done, ContextCompat.getColor(getContext(), R.color.connectionIsReadyColor)));
        serviceItems.add(new ServiceItem("۲۹۳۲۸۶۴", getString(R.string.free), getString(R.string.readyToConnect), R.drawable.ic_done, ContextCompat.getColor(getContext(), R.color.connectionIsReadyColor)));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(new ServiceAdapter(serviceItems, item -> {
            SubscriptionBottomSheet bottomSheet = SubscriptionBottomSheet.newInstance();
            bottomSheet.show(getParentFragmentManager(), "subscription_sheet");
        }));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void checkStatus() {
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
