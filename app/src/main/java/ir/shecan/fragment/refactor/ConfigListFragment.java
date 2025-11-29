package ir.shecan.fragment.refactor;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import ir.shecan.adapter.ServiceAdapter;
import ir.shecan.databinding.FragmentConfigListBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.fragment.bottomSheet.SubscriptionBottomSheet;
import ir.shecan.modelDto.IssuesViewModel;
import ir.shecan.modelDto.ServiceItem;
import ir.shecan.modelDto.ServiceItemMapper;
import ir.shecan.storage.AppStorage;

public class ConfigListFragment extends ToolbarFragment {

    private FragmentConfigListBinding binding;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfigListBinding.inflate(inflater, container, false);

        AppStorage appStorage = new AppStorage(getContext());
        List<ServiceItem> serviceItems = buildServiceItems(appStorage);

        setupRecyclerView(serviceItems, appStorage);

        return binding.getRoot();
    }

    private List<ServiceItem> buildServiceItems(AppStorage appStorage) {
        IssuesViewModel viewModel = appStorage.getIssue(IssuesViewModel.class);

        List<IssuesViewModel.IssuesDTO> issues = (viewModel != null && viewModel.getIssues() != null)
                ? new ArrayList<>(viewModel.getIssues())
                : new ArrayList<>();

        // آیتم پیش‌فرض free
        issues.add(IssuesViewModel.IssuesDTO.createDefault());

        List<ServiceItem> items = new ArrayList<>();
        for (IssuesViewModel.IssuesDTO issue : issues) {
            items.add(ServiceItemMapper.map(getContext(), issue));
        }

        return items;
    }

    private void setupRecyclerView(List<ServiceItem> items, AppStorage appStorage) {
        ServiceItem savedItem = appStorage.getServiceStatus(ServiceItem.class);

        int defaultSelected = -1;
        if (savedItem != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getOrderCode().equals(savedItem.getOrderCode())) {
                    defaultSelected = i;
                    break;
                }
            }
        }

        ServiceAdapter adapter = new ServiceAdapter(
                getContext(),
                items,
                new ServiceAdapter.OnMoreClickListener() {
                    @Override
                    public void onBackgroundClicked(ServiceItem item) {
                        appStorage.saveServiceStatus(item);
                    }

                    @Override
                    public void onOptionClicked(ServiceItem item) {
                        SubscriptionBottomSheet bottomSheet = SubscriptionBottomSheet.newInstance(item);
                        bottomSheet.show(getParentFragmentManager(), "subscription_sheet");
                    }
                }
        );

        adapter.setSelectedPosition(defaultSelected);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void checkStatus() {}

    @Override
    public void onResume() {
        super.onResume();
    }
}
