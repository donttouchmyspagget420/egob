package deez.nuts.egob.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class HomeFragment extends Fragment implements TabFragment {

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
        MainActivity activity = (MainActivity) requireActivity();

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.avatar).setOnClickListener(v -> activity.openProfile());

        View.OnClickListener qr = v -> QrDialog.show(this);
        view.findViewById(R.id.card_credential).setOnClickListener(qr);
        view.findViewById(R.id.btn_show_qr).setOnClickListener(qr);

        view.findViewById(R.id.btn_dismiss_bulletin).setOnClickListener(v ->
                view.findViewById(R.id.bulletin_card).setVisibility(View.GONE));

        view.findViewById(R.id.link_all_services).setOnClickListener(v -> activity.openServices());
        view.findViewById(R.id.link_filing).setOnClickListener(v -> activity.openServiceDetail(
                getString(R.string.home_pay_taxes), getString(R.string.home_pay_taxes_detail)));
        view.findViewById(R.id.quick_renew).setOnClickListener(v -> activity.openServiceDetail(
                getString(R.string.detail_service_name), getString(R.string.detail_description)));
        view.findViewById(R.id.quick_taxes).setOnClickListener(v -> activity.openServiceDetail(
                getString(R.string.home_pay_taxes), getString(R.string.home_pay_taxes_detail)));
        view.findViewById(R.id.quick_health).setOnClickListener(v -> activity.openServiceDetail(
                getString(R.string.home_health_card), getString(R.string.home_health_card_detail)));
        view.findViewById(R.id.quick_appointments).setOnClickListener(v -> activity.openServiceDetail(
                getString(R.string.home_appointments), getString(R.string.home_appointments_detail)));

        view.findViewById(R.id.link_track_history).setOnClickListener(v -> activity.openRequests());
        view.findViewById(R.id.card_active_request).setOnClickListener(v -> activity.openRequests());
        view.findViewById(R.id.link_view_steps).setOnClickListener(v -> activity.openRequests());

        view.findViewById(R.id.card_update_license).setOnClickListener(v -> activity.openDocuments());
        view.findViewById(R.id.card_update_voter).setOnClickListener(v -> activity.openNotifications());

        view.findViewById(R.id.card_emergency).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
            startActivity(intent);
        });
    }
}
