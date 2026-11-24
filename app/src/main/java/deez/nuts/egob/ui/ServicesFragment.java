package deez.nuts.egob.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class ServicesFragment extends Fragment implements TabFragment {

    private final List<ServiceItem> allServices = new ArrayList<>();
    private final List<ServiceItem> filteredServices = new ArrayList<>();

    private ServicesAdapter adapter;
    private TextInputEditText searchInput;
    private ChipGroup categoryGroup;
    private View emptyState;
    private TextView countLabel;
    private String selectedCategory = "all";

    public ServicesFragment() {
        super(R.layout.fragment_services);
    }

    @Override
    public int getTabId() {
        return R.id.nav_services;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.services_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openProfile());

        buildServices();

        searchInput = view.findViewById(R.id.input_search);
        categoryGroup = view.findViewById(R.id.chip_group_categories);
        emptyState = view.findViewById(R.id.empty_state);
        countLabel = view.findViewById(R.id.text_service_count);

        RecyclerView list = view.findViewById(R.id.list_services);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setHasFixedSize(false);
        adapter = new ServicesAdapter(new ServicesAdapter.Listener() {
            @Override
            public void onServiceClick(@NonNull ServiceItem item) {
                ((MainActivity) requireActivity()).openServiceDetail(item.title, item.description);
            }

            @Override
            public void onBookmarkClick(@NonNull ServiceItem item, int position) {
                item.bookmarked = !item.bookmarked;
                adapter.notifyItemChanged(position);
                Snackbar.make(view, item.bookmarked
                        ? R.string.services_bookmarked
                        : R.string.services_unbookmarked, Snackbar.LENGTH_SHORT).show();
            }
        });
        list.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        categoryGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) {
                selectedCategory = String.valueOf(chip.getTag());
                applyFilters();
            }
        });

        view.findViewById(R.id.btn_clear_filters).setOnClickListener(v -> {
            selectedCategory = "all";
            categoryGroup.check(R.id.chip_all);
            searchInput.setText("");
            applyFilters();
        });

        view.findViewById(R.id.btn_support).setOnClickListener(v ->
                Snackbar.make(view, R.string.services_support_detail, Snackbar.LENGTH_LONG).show());

        applyFilters();
    }

    private void buildServices() {
        allServices.clear();
        allServices.add(new ServiceItem(R.drawable.ic_travel, "civil",
                getString(R.string.service_passport_authority),
                getString(R.string.service_passport_title),
                getString(R.string.service_passport_desc),
                getString(R.string.service_days_2_5),
                getString(R.string.service_price_45),
                getString(R.string.service_action_request), null));

        allServices.add(new ServiceItem(R.drawable.ic_car, "transport",
                getString(R.string.service_driver_authority),
                getString(R.string.service_driver_title),
                getString(R.string.service_driver_desc),
                getString(R.string.service_driver_time),
                getString(R.string.service_price_3250),
                getString(R.string.service_action_request), null));

        allServices.add(new ServiceItem(R.drawable.ic_documents, "civil",
                getString(R.string.service_civil_authority),
                getString(R.string.service_civil_title),
                getString(R.string.service_civil_desc),
                getString(R.string.service_civil_time),
                getString(R.string.service_free),
                getString(R.string.service_action_request),
                getString(R.string.service_civil_badge)));

        allServices.add(new ServiceItem(R.drawable.ic_storefront, "taxes",
                getString(R.string.service_commerce_authority),
                getString(R.string.service_commerce_title),
                getString(R.string.service_commerce_desc),
                getString(R.string.service_days_3_7),
                getString(R.string.service_price_80),
                getString(R.string.service_action_register), null));

        allServices.add(new ServiceItem(R.drawable.ic_family, "health",
                getString(R.string.service_social_authority),
                getString(R.string.service_social_title),
                getString(R.string.service_social_desc),
                getString(R.string.service_days_5_10),
                getString(R.string.service_free),
                getString(R.string.service_action_access),
                getString(R.string.service_social_badge)));
    }

    private void applyFilters() {
        String query = searchInput.getText() == null
                ? ""
                : searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());

        filteredServices.clear();
        for (ServiceItem item : allServices) {
            boolean matchesCategory = "all".equals(selectedCategory)
                    || item.category.equals(selectedCategory);
            boolean matchesQuery = query.isEmpty()
                    || item.title.toLowerCase(Locale.getDefault()).contains(query)
                    || item.description.toLowerCase(Locale.getDefault()).contains(query)
                    || item.authority.toLowerCase(Locale.getDefault()).contains(query);
            if (matchesCategory && matchesQuery) {
                filteredServices.add(item);
            }
        }

        adapter.submitList(filteredServices);
        boolean empty = filteredServices.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        countLabel.setText(empty
                ? getString(R.string.services_no_results)
                : getString(R.string.services_all_filtered, filteredServices.size()));
    }
}
