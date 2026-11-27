package deez.nuts.egob.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;

import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Notification;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.RequestStatus;
import deez.nuts.egob.util.SessionManager;

public class HomeFragment extends Fragment implements TabFragment {

    private AppRepository repository;
    private Profile profile;
    private View root;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public int getTabId() {
        return R.id.nav_home;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());
        MainActivity activity = (MainActivity) requireActivity();

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.avatar).setOnClickListener(v -> activity.openProfile());

        View.OnClickListener qr = v -> QrDialog.show(this);
        view.findViewById(R.id.card_credential).setOnClickListener(qr);
        view.findViewById(R.id.btn_show_qr).setOnClickListener(qr);

        view.findViewById(R.id.btn_dismiss_bulletin).setOnClickListener(v -> {
            int userId = SessionManager.currentUserId(requireContext());
            SessionManager.dismissBulletin(requireContext(), userId);
            view.findViewById(R.id.bulletin_card).setVisibility(View.GONE);
        });

        view.findViewById(R.id.link_all_services).setOnClickListener(v -> activity.openServices());
        view.findViewById(R.id.link_filing).setOnClickListener(v ->
                openServiceByTitle(getString(R.string.home_pay_taxes), getString(R.string.home_pay_taxes_detail)));
        view.findViewById(R.id.quick_renew).setOnClickListener(v ->
                openServiceByTitle(getString(R.string.detail_service_name), getString(R.string.detail_description)));
        view.findViewById(R.id.quick_taxes).setOnClickListener(v ->
                openServiceByTitle(getString(R.string.home_pay_taxes), getString(R.string.home_pay_taxes_detail)));
        view.findViewById(R.id.quick_health).setOnClickListener(v ->
                openServiceByTitle(getString(R.string.home_health_card), getString(R.string.home_health_card_detail)));
        view.findViewById(R.id.quick_appointments).setOnClickListener(v ->
                openServiceByTitle(getString(R.string.home_appointments), getString(R.string.home_appointments_detail)));

        view.findViewById(R.id.link_track_history).setOnClickListener(v -> activity.openRequests());
        view.findViewById(R.id.card_active_request).setOnClickListener(v -> activity.openRequests());
        view.findViewById(R.id.link_view_steps).setOnClickListener(v -> activity.openRequests());

        view.findViewById(R.id.card_update_license).setOnClickListener(v -> activity.openDocuments());
        view.findViewById(R.id.card_update_voter).setOnClickListener(v -> activity.openNotifications());

        view.findViewById(R.id.card_emergency).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
            startActivity(intent);
        });

        loadData();
    }

    private void loadData() {
        repository.currentSession(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded()) {
                    return;
                }
                profile = value.profile;
                bindProfile();
                loadRequests();
                loadNotifications();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    ((MainActivity) requireActivity()).openLogin();
                }
            }
        });
    }

    private void bindProfile() {
        if (root == null || profile == null) {
            return;
        }
        String first = profile.fullName == null ? "Elena" : profile.fullName.trim().split("\\s+")[0];
        TextView greeting = root.findViewById(R.id.text_greeting);
        greeting.setText(HtmlCompat.fromHtml(getString(R.string.home_greeting_named, first),
                HtmlCompat.FROM_HTML_MODE_LEGACY));
        ((TextView) root.findViewById(R.id.text_credential_name)).setText(profile.fullName);
        ((TextView) root.findViewById(R.id.text_credential_nin)).setText(profile.dni);
        ((TextView) root.findViewById(R.id.avatar)).setText(profile.avatarInitials);
        ((TextView) root.findViewById(R.id.text_biometric_status)).setText(
                profile.biometricEnabled ? R.string.home_biometric_sync : R.string.home_biometric_pending);
        if (SessionManager.isBulletinDismissed(requireContext(), SessionManager.currentUserId(requireContext()))) {
            root.findViewById(R.id.bulletin_card).setVisibility(View.GONE);
        }
    }

    private void loadRequests() {
        int userId = SessionManager.currentUserId(requireContext());
        repository.getRequests(userId, new RepositoryCallback<List<RequestWithService>>() {
            @Override
            public void onSuccess(List<RequestWithService> value) {
                if (!isAdded() || root == null) {
                    return;
                }
                RequestWithService active = null;
                for (RequestWithService row : value) {
                    if (row.request != null && RequestStatus.isOpen(row.request.status)) {
                        active = row;
                        break;
                    }
                }
                if (active == null) {
                    root.findViewById(R.id.card_active_request).setVisibility(View.GONE);
                    return;
                }
                bindActiveRequest(active);
            }

            @Override
            public void onError(String message) {
                if (root != null) {
                    root.findViewById(R.id.card_active_request).setVisibility(View.GONE);
                }
            }
        });
    }

    private void bindActiveRequest(@NonNull RequestWithService row) {
        root.findViewById(R.id.card_active_request).setVisibility(View.VISIBLE);
        ((TextView) root.findViewById(R.id.text_active_request_title)).setText(row.serviceName);
        ((TextView) root.findViewById(R.id.text_active_request_ref)).setText(row.request.reference);
        Chip status = root.findViewById(R.id.chip_active_request_status);
        status.setText(RequestStatus.label(row.request.status));
        int progress = progressFor(row.request.status);
        ((TextView) root.findViewById(R.id.text_active_request_step)).setText(
                getString(R.string.home_active_request_step, stepFor(row.request.status),
                        stepLabel(row.request.status)));
        ((TextView) root.findViewById(R.id.text_active_request_percent)).setText(progress + "%");
        ((ProgressBar) root.findViewById(R.id.progress_active_request)).setProgress(progress);
        ((TextView) root.findViewById(R.id.text_active_request_estimated)).setText(
                getString(R.string.home_active_request_updated,
                        Formatters.relative(row.request.updatedAt)));
        styleStatusChip(status, row.request.status);
    }

    private void loadNotifications() {
        int userId = SessionManager.currentUserId(requireContext());
        repository.getNotifications(userId, new RepositoryCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> value) {
                if (!isAdded() || root == null) {
                    return;
                }
                bindUpdate(root.findViewById(R.id.text_update_license_title),
                        root.findViewById(R.id.text_update_license_time),
                        root.findViewById(R.id.text_update_license_body), value, 0);
                bindUpdate(root.findViewById(R.id.text_update_voter_title),
                        root.findViewById(R.id.text_update_voter_time),
                        root.findViewById(R.id.text_update_voter_body), value, 1);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void bindUpdate(TextView title, TextView time, TextView body,
                            List<Notification> notifications, int index) {
        if (notifications.size() <= index) {
            title.setText(R.string.home_recent_updates);
            time.setText("");
            body.setText(R.string.home_no_recent_updates);
            return;
        }
        Notification notification = notifications.get(index);
        title.setText(notification.title);
        time.setText(Formatters.relative(notification.createdAt));
        body.setText(notification.body);
    }

    private void openServiceByTitle(String title, String fallbackDescription) {
        repository.findServiceByTitle(title, new RepositoryCallback<Service>() {
            @Override
            public void onSuccess(Service value) {
                if (isAdded()) {
                    ((MainActivity) requireActivity()).openServiceDetail(
                            value.id, value.name, value.description);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    ((MainActivity) requireActivity()).openServiceDetail(title, fallbackDescription);
                }
            }
        });
    }

    private static int progressFor(String status) {
        if (RequestStatus.COMPLETED.equals(status) || RequestStatus.REJECTED.equals(status)) {
            return 100;
        }
        if (RequestStatus.ACTION.equals(status)) {
            return 50;
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return 75;
        }
        return 33;
    }

    private static int stepFor(String status) {
        if (RequestStatus.ACTION.equals(status)) {
            return 2;
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return 3;
        }
        if (RequestStatus.COMPLETED.equals(status)) {
            return 4;
        }
        return 1;
    }

    private String stepLabel(String status) {
        if (RequestStatus.ACTION.equals(status)) {
            return getString(R.string.requests_progress_action);
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return getString(R.string.requests_progress_in_progress);
        }
        if (RequestStatus.COMPLETED.equals(status)) {
            return getString(R.string.requests_progress_completed);
        }
        if (RequestStatus.REJECTED.equals(status)) {
            return getString(R.string.requests_progress_rejected);
        }
        return getString(R.string.requests_progress_pending);
    }

    private void styleStatusChip(Chip chip, String status) {
        int textColor;
        int background;
        if (RequestStatus.ACTION.equals(status)) {
            textColor = R.color.error;
            background = R.color.error_container;
        } else if (RequestStatus.COMPLETED.equals(status)) {
            textColor = R.color.success;
            background = R.color.tertiary_fixed;
        } else if (RequestStatus.REJECTED.equals(status)) {
            textColor = R.color.error;
            background = R.color.error_container;
        } else {
            textColor = R.color.secondary;
            background = R.color.surface_container_low;
        }
        chip.setTextColor(ContextCompat.getColor(requireContext(), textColor));
        chip.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), background)));
    }
}
