package ir.shecan.fragment.refactor;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.activity.LoginActivity;
import ir.shecan.activity.MainActivityNew;
import ir.shecan.adapter.ProfileAdapter;
import ir.shecan.databinding.FragmentProfileBinding;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.model.ProfileItem;

public class ProfileFragment extends ToolbarFragment {

    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        setupRecycler();

        return binding.getRoot();
    }

    private void setupRecycler() {
        List<ProfileItem> list = new ArrayList<>();
        list.add(new ProfileItem(R.drawable.ic_info, "حساب کاربری"));
        list.add(new ProfileItem(R.drawable.ic_info, "ظاهر برنامه"));
        list.add(new ProfileItem(R.drawable.ic_info, "تراکنش‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "پشتیبانی دامنه‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "تیکت‌ها"));
        list.add(new ProfileItem(R.drawable.ic_info, "درباره"));

        ProfileAdapter adapter = new ProfileAdapter(list, (position, item) -> {
            switch (position) {
                case 0:
//                    openFragment(AccountFragment.class);
                    break;
                case 1:
                    openFragment(ThemeFragment.class);
                    break;
                case 2:
//                    openFragment(TransactionFragment.class);
                    getActivity().startActivity(new Intent(getActivity(), LoginActivity.class));
                    break;
                case 3:
//                    openFragment(DomainSupportFragment.class);
                    break;
                case 4:
//                    openFragment(TicketFragment.class);
                    break;
                case 5:
//                    openFragment(AboutFragment.class);
                    break;
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void openFragment(Class fragmentClass) {
        if (getActivity() instanceof MainActivityNew) {
            ((MainActivityNew) getActivity()).switchFragment(fragmentClass, false, true);
        }
    }

    @Override
    public void checkStatus() {

    }
}

