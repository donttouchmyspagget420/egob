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
import java.util.Calendar;
import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.SessionManager;

public class NotificationsFragment extends Fragment {

    private final List<Notification> allNotifications = new ArrayList<>();
    private final List<NotificationItem> allItems = new ArrayList<>();

    private NotificationsAdapter todayAdapter;
    private NotificationsAdapter yesterdayAdapter;
    private NotificationsAdapter earlierAdapter;

    private RecyclerView todayList;
    private RecyclerView yesterdayList;
    private RecyclerView earlierList;
    private View emptyState;
    private View markAllFooter;
    private TextView todayBadge;
    private Chip chipAll;
    private Chip chipUrgent;
    private Chip chipUpdates;
    private Chip chipLegal;
    private AppRepository repository;
    private View root;
    private String selectedFilter = "all";
    private int userId = -1;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

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

        chipAll = view.findViewById(R.id.chip_notifications_all);
        chipUrgent = view.findViewById(R.id.chip_notifications_urgent);
        chipUpdates = view.findViewById(R.id.chip_notifications_updates);
        chipLegal = view.findViewById(R.id.chip_notifications_legal);

        ChipGroup filters = view.findViewById(R.id.chip_group_notifications);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            selectedFilter = chip == null ? "all" : String.valueOf(chip.getTag());
            applyFilter();
        });

        view.findViewById(R.id.btn_mark_all_read).setOnClickListener(v -> markAllRead());
        markAllFooter.setOnClickListener(v -> markAllRead());

        if (userId <= 0) {
            ((MainActivity) requireActivity()).openLogin();
            return;
        }
        loadNotifications();
    }

    private NotificationsAdapter createAdapter() {
        return new NotificationsAdapter(new NotificationsAdapter.Listener() {
            @Override
            public void onAction(@NonNull NotificationItem item) {
                if (item.notificationId > 0) {
                    repository.markNotificationRead(item.notificationId);
                }
                MainActivity activity = (MainActivity) requireActivity();
                if ("request".equals(item.relatedType)) {
                    activity.openRequests();
                } else if ("document".equals(item.relatedType)) {
                    activity.openDocuments();
                } else if (item.action != null) {
                    Snackbar.make(root, item.action, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDismiss(@NonNull NotificationItem item) {
                repository.dismissNotification(userId, item.notificationId,
                        new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                if (!isAdded() || root == null) {
                                    return;
                                }
                                removeNotification(item.notificationId);
                                Snackbar.make(root, R.string.notifications_dismissed,
                                        Snackbar.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded() && root != null) {
                                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    private void loadNotifications() {
        repository.getNotifications(userId, new RepositoryCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> value) {
                if (!isAdded() || root == null) {
                    return;
                }
                allNotifications.clear();
                allNotifications.addAll(value);
                rebuildItems();
                applyFilter();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void rebuildItems() {
        allItems.clear();
        for (Notification notification : allNotifications) {
            NotificationItem item = toItem(notification);
            allItems.add(item);
        }
        int unread = 0;
        int urgent = 0;
        int updates = 0;
        int legal = 0;
        for (Notification notification : allNotifications) {
            if (!notification.read) {
                unread++;
            }
            if ("urgent".equals(notification.category)) {
                urgent++;
            } else if ("legal".equals(notification.category)) {
                legal++;
            } else {
                updates++;
            }
        }
        chipAll.setText(getString(R.string.notifications_all_count, allNotifications.size()));
        chipUrgent.setText(getString(R.string.notifications_urgent_count, urgent));
        chipUpdates.setText(getString(R.string.notifications_updates_count, updates));
        chipLegal.setText(getString(R.string.notifications_legal_count, legal));
        if (unread > 0) {
            todayBadge.setVisibility(View.VISIBLE);
            todayBadge.setText(getString(R.string.notifications_new_count, unread));
        } else {
            todayBadge.setVisibility(View.GONE);
        }
    }

    private NotificationItem toItem(@NonNull Notification notification) {
        String key;
        int icon;
        int iconBackground;
        int accent;
        int categoryLabel;
        if ("urgent".equals(notification.category)) {
            key = "urgent";
            icon = R.drawable.ic_campaign;
            iconBackground = R.drawable.bg_icon_error;
            accent = R.color.error;
            categoryLabel = R.string.notifications_category_urgent;
        } else if ("legal".equals(notification.category)) {
            key = "legal";
            icon = R.drawable.ic_documents;
            iconBackground = R.drawable.bg_icon_success;
            accent = R.color.on_tertiary_fixed_variant;
            categoryLabel = R.string.notifications_category_legal;
        } else {
            key = "updates";
            icon = R.drawable.ic_contactless;
            iconBackground = R.drawable.bg_icon_soft;
            accent = R.color.secondary;
            categoryLabel = R.string.notifications_category_updates;
        }

        NotificationItem item = new NotificationItem(key, icon, iconBackground, accent,
                getString(categoryLabel), Formatters.relative(notification.createdAt),
                safe(notification.title), safe(notification.body));
        item.timeGroup = groupFor(notification.createdAt);
        item.action = notification.actionLabel == null || notification.actionLabel.trim().isEmpty()
                ? null : notification.actionLabel;
        item.dismissible = true;
        item.notificationId = notification.id;
        item.relatedType = notification.relatedType;
        item.relatedId = notification.relatedId;
        item.read = notification.read;
        return item;
    }

    private void applyFilter() {
        List<NotificationItem> today = new ArrayList<>();
        List<NotificationItem> yesterday = new ArrayList<>();
        List<NotificationItem> earlier = new ArrayList<>();

        for (NotificationItem item : allItems) {
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

    private void markAllRead() {
        repository.markAllNotificationsRead(userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                if (!isAdded() || root == null) {
                    return;
                }
                for (Notification notification : allNotifications) {
                    notification.read = true;
                }
                rebuildItems();
                applyFilter();
                Snackbar.make(root, R.string.notifications_all_read, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void removeNotification(int notificationId) {
        for (int i = allNotifications.size() - 1; i >= 0; i--) {
            if (allNotifications.get(i).id == notificationId) {
                allNotifications.remove(i);
            }
        }
        rebuildItems();
        applyFilter();
    }

    private static String groupFor(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        if (sameDay(now, target)) {
            return "today";
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(yesterday, target)) {
            return "yesterday";
        }
        return "earlier";
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
