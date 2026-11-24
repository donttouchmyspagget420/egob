package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class ProfileFragment extends Fragment implements TabFragment {

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
        MainActivity activity = (MainActivity) requireActivity();

        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.profile_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());

        view.findViewById(R.id.row_devices).setOnClickListener(v ->
                Snackbar.make(view, R.string.profile_devices_manage, Snackbar.LENGTH_LONG).show());
        view.findViewById(R.id.row_language).setOnClickListener(v ->
                Snackbar.make(view, R.string.profile_language_dialog, Snackbar.LENGTH_LONG).show());
        view.findViewById(R.id.row_download_record).setOnClickListener(v ->
                Snackbar.make(view, R.string.profile_download_started, Snackbar.LENGTH_LONG).show());
        view.findViewById(R.id.row_correction).setOnClickListener(v ->
                Snackbar.make(view, R.string.profile_correction_started, Snackbar.LENGTH_LONG).show());

        view.findViewById(R.id.row_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.row_admin).setOnClickListener(v -> activity.openAdmin());

        view.findViewById(R.id.btn_sign_out).setOnClickListener(v -> activity.openLogin());
    }
}
