package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class NotificationsFragment extends Fragment {

    private final List<NotificationItem> allNotifications = new ArrayList<>();

    private NotificationsAdapter todayAdapter;
    private NotificationsAdapter yesterdayAdapter;
    private NotificationsAdapter earlierAdapter;

    private RecyclerView todayList;
    private RecyclerView yesterdayList;
    private RecyclerView earlierList;
    private View emptyState;
    private View markAllFooter;
    private TextView todayBadge;
    private String selectedFilter = "all";

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        buildNotifications();

        todayList = view.findViewById(R.id.list_today);
        yesterdayList = view.findViewById(R.id.list_yesterday);
        earlierList = view.findViewById(R.id.list_earlier);
        emptyState = view.findViewById(R.id.empty_notifications);
        markAllFooter = view.findViewById(R.id.mark_all_footer);
        todayBadge = view.findViewById(R.id.today_badge);

        todayAdapter = createAdapter();
        yesterdayAdapter = createAdapter();
        earlierAdapter = createAdapter();

        todayList.setLayoutManager(new LinearLayoutManager(requireContext()));
        yesterdayList.setLayoutManager(new LinearLayoutManager(requireContext()));
        earlierList.setLayoutManager(new LinearLayoutManager(requireContext()));
        todayList.setAdapter(todayAdapter);
        yesterdayList.setAdapter(yesterdayAdapter);
        earlierList.setAdapter(earlierAdapter);

        ChipGroup filters = view.findViewById(R.id.chip_group_notifications);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            selectedFilter = chip == null ? "all" : String.valueOf(chip.getTag());
            applyFilter();
        });

        view.findViewById(R.id.btn_mark_all_read).setOnClickListener(v -> {
            markAllFooter.setVisibility(View.VISIBLE);
            todayBadge.setVisibility(View.GONE);
            Snackbar.make(view, R.string.notifications_all_read, Snackbar.LENGTH_SHORT).show();
        });

        applyFilter();
    }

    private NotificationsAdapter createAdapter() {
        return new NotificationsAdapter(new NotificationsAdapter.Listener() {
            @Override
            public void onAction(@NonNull NotificationItem item) {
                MainActivity activity = (MainActivity) requireActivity();
                switch (item.categoryKey) {
                    case "passport":
                        activity.openRequests();
                        break;
                    case "license":
                    case "tax_receipt":
                        activity.openDocuments();
                        break;
                    case "vehicle":
                        activity.openServices();
                        break;
                    case "tax":
                        activity.openRequests();
                        break;
                    default:
                        View root = getView();
                        if (root != null && item.action != null) {
                            Snackbar.make(root, item.action, Snackbar.LENGTH_SHORT).show();
                        }
                        break;
                }
            }

            @Override
            public void onDismiss(@NonNull NotificationItem item) {
                allNotifications.remove(item);
                applyFilter();
                View root = getView();
                if (root != null) {
                    Snackbar.make(root, R.string.notifications_dismissed, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilter() {
        List<NotificationItem> today = new ArrayList<>();
        List<NotificationItem> yesterday = new ArrayList<>();
        List<NotificationItem> earlier = new ArrayList<>();

        for (NotificationItem item : allNotifications) {
            if (!"all".equals(selectedFilter) && !selectedFilter.equals(item.categoryKey)) {
                continue;
            }
            if ("today".equals(item.timeGroup)) {
                today.add(item);
            } else if ("yesterday".equals(item.timeGroup)) {
                yesterday.add(item);
            } else {
                earlier.add(item);
            }
        }

        todayAdapter.submitList(today);
        yesterdayAdapter.submitList(yesterday);
        earlierAdapter.submitList(earlier);

        boolean empty = today.isEmpty() && yesterday.isEmpty() && earlier.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        todayList.setVisibility(today.isEmpty() ? View.GONE : View.VISIBLE);
        yesterdayList.setVisibility(yesterday.isEmpty() ? View.GONE : View.VISIBLE);
        earlierList.setVisibility(earlier.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void buildNotifications() {
        allNotifications.clear();

        NotificationItem tax = new NotificationItem("urgent", R.drawable.ic_campaign,
                R.drawable.bg_icon_error, R.color.error,
                getString(R.string.notifications_urgent_action),
                getString(R.string.notifications_ten_min),
                getString(R.string.notifications_tax_deadline),
                getString(R.string.notifications_tax_body));
        tax.timeGroup = "today";
        tax.action = getString(R.string.notifications_review_file);
        tax.dismissible = true;
        allNotifications.add(tax);

        NotificationItem passport = new NotificationItem("updates", R.drawable.ic_contactless,
                R.drawable.bg_icon_soft, R.color.secondary,
                getString(R.string.notifications_document_track),
                getString(R.string.notifications_two_hours),
                getString(R.string.notifications_passport_title),
                getString(R.string.notifications_passport_body));
        passport.timeGroup = "today";
        passport.chipPrimary = getString(R.string.notifications_security_cleared);
        passport.chipSecondary = getString(R.string.notifications_step_3);
        passport.action = getString(R.string.notifications_track_step);
        allNotifications.add(passport);

        NotificationItem license = new NotificationItem("legal", R.drawable.ic_documents,
                R.drawable.bg_icon_success, R.color.on_tertiary_fixed_variant,
                getString(R.string.notifications_vault_sync),
                getString(R.string.notifications_time_415),
                getString(R.string.notifications_license_approved),
                getString(R.string.notifications_license_body));
        license.timeGroup = "yesterday";
        license.chipPrimary = getString(R.string.notifications_ready);
        license.action = getString(R.string.notifications_view_wallet);
        allNotifications.add(license);

        NotificationItem login = new NotificationItem("legal", R.drawable.ic_security,
                R.drawable.bg_icon_soft, R.color.on_surface_variant,
                getString(R.string.notifications_security_protocol),
                getString(R.string.notifications_time_1102),
                getString(R.string.notifications_login),
                getString(R.string.notifications_login_body));
        login.timeGroup = "yesterday";
        login.action = getString(R.string.notifications_verify_device);
        login.footer = getString(R.string.notifications_ip);
        allNotifications.add(login);

        NotificationItem water = new NotificationItem("updates", R.drawable.ic_water,
                R.drawable.bg_icon_secondary_soft, R.color.secondary,
                getString(R.string.notifications_public_works),
                getString(R.string.notifications_nov_11),
                getString(R.string.notifications_water),
                getString(R.string.notifications_water_body));
        water.timeGroup = "earlier";
        allNotifications.add(water);

        NotificationItem voter = new NotificationItem("updates", R.drawable.ic_how_to_vote,
                R.drawable.bg_icon_soft, R.color.secondary,
                getString(R.string.notifications_electoral_board),
                getString(R.string.notifications_nov_09),
                getString(R.string.notifications_voter),
                getString(R.string.notifications_voter_body));
        voter.timeGroup = "earlier";
        allNotifications.add(voter);

        NotificationItem receipt = new NotificationItem("updates", R.drawable.ic_payments,
                R.drawable.bg_icon_soft, R.color.primary,
                getString(R.string.notifications_category_tax),
                getString(R.string.notifications_tax_receipt_time),
                getString(R.string.notifications_tax_receipt_title),
                getString(R.string.notifications_tax_receipt_body));
        receipt.timeGroup = "earlier";
        receipt.action = getString(R.string.notifications_tax_receipt_action);
        allNotifications.add(receipt);

        NotificationItem vehicle = new NotificationItem("urgent", R.drawable.ic_car,
                R.drawable.bg_icon_error, R.color.error,
                getString(R.string.notifications_category_transport),
                getString(R.string.notifications_vehicle_time),
                getString(R.string.notifications_vehicle_title),
                getString(R.string.notifications_vehicle_body));
        vehicle.timeGroup = "earlier";
        vehicle.action = getString(R.string.notifications_vehicle_action);
        vehicle.dismissible = true;
        allNotifications.add(vehicle);
    }
}
