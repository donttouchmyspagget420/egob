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
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.SessionManager;

public class ServicesFragment extends Fragment implements TabFragment {

    private final List<ServiceItem> allServices = new ArrayList<>();
    private final List<ServiceItem> filteredServices = new ArrayList<>();

    private AppRepository repository;
    private ServicesAdapter adapter;
    private TextInputEditText searchInput;
    private ChipGroup categoryGroup;
    private View emptyState;
    private TextView countLabel;
    private String selectedCategory = "all";
    private int userId = -1;

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

        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.services_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openProfile());

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
                ((MainActivity) requireActivity()).openServiceDetail(item.id, item.title, item.description);
            }

            @Override
            public void onBookmarkClick(@NonNull ServiceItem item, int position) {
                toggleBookmark(item, position, view);
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
                ((MainActivity) requireActivity()).openAssistant());

        loadServices(view);
    }

    private void loadServices(@NonNull View view) {
        if (userId <= 0) {
            Snackbar.make(view, R.string.detail_requires_session, Snackbar.LENGTH_LONG).show();
            return;
        }
        repository.getServices(userId, new RepositoryCallback<List<Service>>() {
            @Override
            public void onSuccess(List<Service> value) {
                if (!isAdded()) {
                    return;
                }
                allServices.clear();
                for (Service service : value) {
                    ServiceItem item = new ServiceItem(iconFor(service), service.category,
                            safe(service.authority), safe(service.name), safe(service.description),
                            safe(service.time),
                            service.fee <= 0 ? getString(R.string.service_free)
                                    : Formatters.currency(service.fee),
                            getString(R.string.service_action_request), service.badge);
                    item.id = service.id;
                    item.bookmarked = service.bookmarked;
                    allServices.add(item);
                }
                applyFilters();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void toggleBookmark(@NonNull ServiceItem item, int position, @NonNull View view) {
        if (userId <= 0) {
            return;
        }
        boolean previous = item.bookmarked;
        item.bookmarked = !previous;
        adapter.notifyItemChanged(position);
        repository.toggleBookmark(userId, item.id, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                if (!isAdded()) {
                    return;
                }
                item.bookmarked = value;
                adapter.notifyItemChanged(position);
                Snackbar.make(view, value ? R.string.services_bookmarked
                        : R.string.services_unbookmarked, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                item.bookmarked = previous;
                adapter.notifyItemChanged(position);
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private int iconFor(Service service) {
        String name = safe(service.name).toLowerCase(Locale.getDefault());
        if (name.contains("pasaporte") || name.contains("viaje")) {
            return R.drawable.ic_travel;
        }
        switch (safe(service.category)) {
            case "transport":
                return R.drawable.ic_car;
            case "taxes":
                return R.drawable.ic_storefront;
            case "health":
                return R.drawable.ic_family;
            case "housing":
                return R.drawable.ic_home;
            case "civil":
            default:
                return R.drawable.ic_documents;
        }
    }

    private void applyFilters() {
        if (adapter == null || searchInput == null || emptyState == null || countLabel == null) {
            return;
        }
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
                : getString(R.string.services_all_filtered_total, filteredServices.size(), allServices.size()));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
