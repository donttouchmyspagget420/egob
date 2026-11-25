package deez.nuts.egob.ui;

import android.view.View;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import deez.nuts.egob.R;

/** Bottom-sheet style presentation of the citizen's dynamic QR credential. */
public final class QrDialog {

    private QrDialog() {
    }

    public static void show(Fragment fragment) {
        BottomSheetDialog dialog = new BottomSheetDialog(fragment.requireContext());
        dialog.setContentView(R.layout.dialog_qr);

        View close = dialog.findViewById(R.id.btn_close_qr);
        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }
}
