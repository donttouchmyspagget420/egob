package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.database.entities.User;
import deez.nuts.egob.util.FileShare;
import deez.nuts.egob.util.SessionManager;

public class ProfileFragment extends Fragment implements TabFragment {

    private AppRepository repository;
    private View root;
    private Profile profile;
    private User user;
    private boolean bindingSwitches;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public int getTabId() {
        return R.id.nav_profile;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());

        MainActivity activity = (MainActivity) requireActivity();
        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.profile_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());

        view.findViewById(R.id.row_edit_contact).setOnClickListener(v -> editContact());
        view.findViewById(R.id.row_devices).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.profile_devices_title)
                        .setMessage(R.string.profile_devices_body)
                        .setPositiveButton(R.string.action_done, null)
                        .show());
        view.findViewById(R.id.row_download_record).setOnClickListener(v -> downloadRecord());
        view.findViewById(R.id.row_correction).setOnClickListener(v -> requestCorrection());
        view.findViewById(R.id.row_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.row_admin).setOnClickListener(v -> {
            if (user != null && "admin".equals(user.role)) {
                activity.openAdmin();
            } else {
                Snackbar.make(view, R.string.profile_admin_restricted, Snackbar.LENGTH_LONG).show();
            }
        });

        view.findViewById(R.id.btn_sign_out).setOnClickListener(v -> confirmSignOut());

        int userId = SessionManager.currentUserId(requireContext());
        if (userId <= 0) {
            activity.openLogin();
            return;
        }
        loadSession();
    }

    private void loadSession() {
        repository.currentSession(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded() || root == null) {
                    return;
                }
                user = value.user;
                profile = value.profile;
                bindProfile();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindProfile() {
        if (profile == null || root == null) {
            return;
        }
        ((TextView) root.findViewById(R.id.text_profile_initials)).setText(profile.avatarInitials);
        ((TextView) root.findViewById(R.id.text_profile_name)).setText(profile.fullName);
        ((TextView) root.findViewById(R.id.text_profile_nin)).setText(profile.dni);
        ((TextView) root.findViewById(R.id.text_registry_since)).setText(
                getString(R.string.profile_registry_since_value, profile.memberSince,
                        profile.district));
        ((TextView) root.findViewById(R.id.text_civ_id)).setText(
                getString(R.string.profile_civ_id_value, profile.memberSince));
        ((TextView) root.findViewById(R.id.chip_profile_tier)).setText(
                getString(R.string.profile_tier_value, profile.tier));
        ((TextView) root.findViewById(R.id.text_profile_district)).setText(profile.district);
        ((TextView) root.findViewById(R.id.text_profile_dob)).setText(profile.dateOfBirth);
        ((TextView) root.findViewById(R.id.text_profile_marital)).setText(profile.maritalStatus);
        ((TextView) root.findViewById(R.id.text_profile_address)).setText(profile.adress);
        ((TextView) root.findViewById(R.id.text_profile_city)).setText(
                profile.city + " · " + profile.code);
        ((TextView) root.findViewById(R.id.text_profile_polling)).setText(profile.pollingPlace);

        MaterialSwitch emergency = root.findViewById(R.id.switch_emergency);
        MaterialSwitch tax = root.findViewById(R.id.switch_tax);
        MaterialSwitch health = root.findViewById(R.id.switch_health);
        bindingSwitches = true;
        emergency.setChecked(profile.emergencySms);
        tax.setChecked(profile.taxPush);
        health.setChecked(profile.healthAlerts);
        bindingSwitches = false;

        emergency.setOnCheckedChangeListener((button, checked) ->
                savePreferences(checked, tax.isChecked(), health.isChecked()));
        tax.setOnCheckedChangeListener((button, checked) ->
                savePreferences(emergency.isChecked(), checked, health.isChecked()));
        health.setOnCheckedChangeListener((button, checked) ->
                savePreferences(emergency.isChecked(), tax.isChecked(), checked));

        root.findViewById(R.id.row_admin).setVisibility(
                user != null && "admin".equals(user.role) ? View.VISIBLE : View.GONE);
    }

    private void savePreferences(boolean emergency, boolean tax, boolean health) {
        if (bindingSwitches || profile == null) {
            return;
        }
        repository.updateNotificationPreferences(user.id, emergency, tax, health,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        if (profile == null) {
                            return;
                        }
                        profile.emergencySms = emergency;
                        profile.taxPush = tax;
                        profile.healthAlerts = health;
                        if (isAdded() && root != null) {
                            Snackbar.make(root, R.string.profile_prefs_saved, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || root == null) {
                            return;
                        }
                        bindingSwitches = true;
                        ((MaterialSwitch) root.findViewById(R.id.switch_emergency))
                                .setChecked(profile.emergencySms);
                        ((MaterialSwitch) root.findViewById(R.id.switch_tax))
                                .setChecked(profile.taxPush);
                        ((MaterialSwitch) root.findViewById(R.id.switch_health))
                                .setChecked(profile.healthAlerts);
                        bindingSwitches = false;
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void editContact() {
        if (profile == null) {
            return;
        }
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_contact, null, false);
        TextInputEditText phone = content.findViewById(R.id.input_phone);
        TextInputEditText address = content.findViewById(R.id.input_address);
        phone.setText(profile.phone);
        address.setText(profile.adress);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_edit_title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    String phoneValue = phone.getText() == null ? "" : phone.getText().toString().trim();
                    String addressValue = address.getText() == null ? "" : address.getText().toString().trim();
                    if (phoneValue.isEmpty() || addressValue.isEmpty()) {
                        Snackbar.make(root, R.string.profile_contact_required, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    profile.phone = phoneValue;
                    profile.adress = addressValue;
                    repository.updateProfile(user.id, profile, new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            if (!isAdded() || root == null) {
                                return;
                            }
                            bindProfile();
                            Snackbar.make(root, R.string.profile_saved, Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded() && root != null) {
                                Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .show();
    }

    private void downloadRecord() {
        repository.downloadProfilePdf(user.id, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File value) {
                if (!isAdded() || root == null) {
                    return;
                }
                try {
                    File saved = FileShare.saveToDownloads(requireContext(), value, "egob_registro_identidad.pdf");
                    Snackbar.make(root, getString(R.string.profile_download_saved, saved.getAbsolutePath()),
                            Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Snackbar.make(root, R.string.profile_download_error, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void requestCorrection() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_correction, null, false);
        TextInputEditText note = content.findViewById(R.id.input_note);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_correction_title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    String value = note.getText() == null ? "" : note.getText().toString().trim();
                    if (value.isEmpty()) {
                        Snackbar.make(root, R.string.profile_correction_hint, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    repository.requestCorrection(user.id, value, new RepositoryCallback<>() {
                        @Override
                        public void onSuccess(deez.nuts.egob.database.entities.Request result) {
                            if (!isAdded() || root == null) {
                                return;
                            }
                            Snackbar.make(root, R.string.profile_correction_sent, Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded() && root != null) {
                                Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .show();
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_sign_out_title)
                .setMessage(R.string.profile_sign_out_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.profile_sign_out, (dialog, which) -> {
                    repository.logout();
                    if (isAdded()) {
                        ((MainActivity) requireActivity()).openLogin();
                    }
                })
                .show();
    }
}
