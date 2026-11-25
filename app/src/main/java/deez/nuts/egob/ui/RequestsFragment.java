package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class RequestsFragment extends Fragment implements TabFragment {

    public RequestsFragment() {
        super(R.layout.fragment_requests);
    }

    @Override
    public int getTabId() {
        return R.id.nav_requests;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) requireActivity();

        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.requests_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v -> activity.openProfile());

        ChipGroup filters = view.findViewById(R.id.chip_group_requests);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            String filter = chip == null ? "all" : String.valueOf(chip.getTag());
            applyFilter(view, filter);
        });

        view.findViewById(R.id.btn_upload_missing).setOnClickListener(v -> showUploadDialog());
        view.findViewById(R.id.btn_download_copy).setOnClickListener(v ->
                Snackbar.make(view, R.string.requests_pdf_saved, Snackbar.LENGTH_LONG).show());

        TextView passportToggle = view.findViewById(R.id.btn_toggle_passport_details);
        View passportDetails = view.findViewById(R.id.details_passport);
        passportToggle.setOnClickListener(v -> toggleDetails(passportToggle, passportDetails));

        TextView taxToggle = view.findViewById(R.id.btn_toggle_tax_details);
        View taxDetails = view.findViewById(R.id.details_tax);
        taxToggle.setOnClickListener(v -> toggleDetails(taxToggle, taxDetails));
    }

    private void applyFilter(@NonNull View root, @NonNull String filter) {
        int[] cards = {R.id.card_request_passport, R.id.card_request_parking,
                R.id.card_request_vital, R.id.card_request_tax};
        for (int id : cards) {
            View card = root.findViewById(id);
            String tag = String.valueOf(card.getTag());
            card.setVisibility("all".equals(filter) || filter.equals(tag)
                    ? View.VISIBLE
                    : View.GONE);
        }
    }

    private void toggleDetails(@NonNull TextView toggle, @NonNull View details) {
        boolean show = details.getVisibility() != View.VISIBLE;
        details.setVisibility(show ? View.VISIBLE : View.GONE);
        toggle.setText(show ? R.string.requests_collapse : R.string.requests_expand);
    }

    private void showUploadDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(R.layout.dialog_upload);

        dialog.findViewById(R.id.upload_drop_zone).setOnClickListener(v ->
                Snackbar.make(v, R.string.detail_file_picker, Snackbar.LENGTH_SHORT).show());
        dialog.findViewById(R.id.btn_cancel_upload).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_confirm_upload).setOnClickListener(v -> {
            dialog.dismiss();
            View root = getView();
            if (root != null) {
                Snackbar.make(root, R.string.requests_upload_success, Snackbar.LENGTH_LONG).show();
            }
        });
        dialog.show();
    }
}
