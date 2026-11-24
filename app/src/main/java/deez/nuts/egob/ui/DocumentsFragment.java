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

public class DocumentsFragment extends Fragment implements TabFragment {

    public DocumentsFragment() {
        super(R.layout.fragment_documents);
    }

    @Override
    public int getTabId() {
        return R.id.nav_documents;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) requireActivity();

        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.documents_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v -> activity.openProfile());

        View.OnClickListener inspect = v -> {
            QrDialog.show(this);
            Snackbar.make(view, R.string.documents_credential_opened, Snackbar.LENGTH_SHORT).show();
        };
        view.findViewById(R.id.card_nid).setOnClickListener(inspect);
        view.findViewById(R.id.card_license).setOnClickListener(inspect);
        view.findViewById(R.id.card_medicare).setOnClickListener(inspect);
        view.findViewById(R.id.card_show_qr).setOnClickListener(v -> QrDialog.show(this));

        view.findViewById(R.id.btn_add_document).setOnClickListener(v -> {
            Snackbar.make(view, R.string.documents_add_scanning, Snackbar.LENGTH_LONG).show();
            view.postDelayed(() -> Snackbar.make(view, R.string.documents_saved,
                    Snackbar.LENGTH_SHORT).show(), 1600);
        });

        view.findViewById(R.id.row_download_pdf).setOnClickListener(v ->
                Snackbar.make(view, R.string.documents_pdf_ready, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.row_share).setOnClickListener(v ->
                Snackbar.make(view, R.string.documents_share_ready, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.row_audit).setOnClickListener(v ->
                Snackbar.make(view, R.string.documents_audit_detail, Snackbar.LENGTH_LONG).show());
    }
}
