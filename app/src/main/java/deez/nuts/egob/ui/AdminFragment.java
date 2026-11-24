package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;

public class AdminFragment extends Fragment {

    public AdminFragment() {
        super(R.layout.fragment_admin);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        List<AdminRequest> requests = new ArrayList<>();
        requests.add(new AdminRequest("#REQ-90142", getString(R.string.citizen_name),
                getString(R.string.admin_initials_ev),
                getString(R.string.admin_service_parking),
                getString(R.string.admin_submitted_nov01),
                getString(R.string.admin_agent_412),
                getString(R.string.admin_status_action),
                AdminRequest.STATUS_ERROR,
                getString(R.string.admin_review_dossier)));
        requests.add(new AdminRequest("#REQ-88319", getString(R.string.admin_citizen_marcus),
                getString(R.string.admin_initials_mb),
                getString(R.string.admin_service_passport),
                getString(R.string.admin_submitted_oct29),
                getString(R.string.admin_agent_108),
                getString(R.string.admin_status_printing),
                AdminRequest.STATUS_INFO,
                getString(R.string.admin_track_printing)));
        requests.add(new AdminRequest("#REQ-90211", getString(R.string.admin_citizen_aisha),
                getString(R.string.admin_initials_ap),
                getString(R.string.admin_service_business),
                getString(R.string.admin_submitted_nov02),
                getString(R.string.admin_agent_229),
                getString(R.string.admin_status_manual),
                AdminRequest.STATUS_WARNING,
                getString(R.string.admin_escalate)));
        requests.add(new AdminRequest("#REQ-90334", getString(R.string.admin_citizen_david),
                getString(R.string.admin_initials_dk),
                getString(R.string.admin_service_vital),
                getString(R.string.admin_submitted_nov03),
                getString(R.string.admin_agent_412),
                getString(R.string.admin_status_approved),
                AdminRequest.STATUS_SUCCESS,
                getString(R.string.admin_view_record)));

        RecyclerView list = view.findViewById(R.id.list_admin_requests);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        AdminRequestsAdapter adapter = new AdminRequestsAdapter(request ->
                Snackbar.make(view, request.action, Snackbar.LENGTH_SHORT).show());
        list.setAdapter(adapter);
        adapter.submitList(requests);

        view.findViewById(R.id.btn_daily_audit).setOnClickListener(v ->
                Snackbar.make(view, R.string.admin_audit_started, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_manage_queues).setOnClickListener(v ->
                Snackbar.make(view, R.string.admin_queues_opened, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_broadcast).setOnClickListener(v ->
                Snackbar.make(view, R.string.admin_broadcast_started, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_audit_queue).setOnClickListener(v ->
                Snackbar.make(view, R.string.admin_audit_queue_open, Snackbar.LENGTH_SHORT).show());
    }
}
