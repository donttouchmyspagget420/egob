package deez.nuts.egob.ui;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.WriterException;

import java.util.Locale;

import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.security.QrGenerator;
import deez.nuts.egob.security.QrPayload;
import deez.nuts.egob.security.SecurityKeys;

/** Bottom-sheet presentation of the citizen's signed, rotating QR credential. */
public final class QrDialog {

    private static final long TOKEN_LIFETIME_MS = 120_000L;

    private QrDialog() {
    }

    public static void show(Fragment fragment) {
        BottomSheetDialog dialog = new BottomSheetDialog(fragment.requireContext());
        dialog.setContentView(R.layout.dialog_qr);

        View close = dialog.findViewById(R.id.btn_close_qr);
        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }

        ImageView image = dialog.findViewById(R.id.qr_image);
        TextView status = dialog.findViewById(R.id.qr_status);
        TextView nin = dialog.findViewById(R.id.qr_nin);

        AppRepository repository = AppRepository.get(fragment.requireContext());
        repository.currentSession(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!fragment.isAdded()) {
                    return;
                }
                Profile profile = value.profile;
                if (nin != null) {
                    nin.setText(fragment.getString(R.string.qr_nin_label, profile.dni));
                }
                render(dialog, fragment, image, status, profile);
            }

            @Override
            public void onError(String message) {
                if (fragment.isAdded() && status != null) {
                    status.setText(message);
                }
            }
        });

        dialog.show();
    }

    private static void render(BottomSheetDialog dialog, Fragment fragment, ImageView image,
                               TextView status, Profile profile) {
        Handler handler = new Handler(Looper.getMainLooper());
        String secret = SecurityKeys.qrSecret(fragment.requireContext());
        long[] issuedAt = {System.currentTimeMillis()};

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing() || !fragment.isAdded()) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - issuedAt[0] >= TOKEN_LIFETIME_MS) {
                    issuedAt[0] = now;
                    generate(fragment, image, profile, secret, issuedAt[0]);
                }
                long remaining = Math.max(0, TOKEN_LIFETIME_MS - (now - issuedAt[0]));
                if (status != null) {
                    String time = String.format(Locale.US, "%02d:%02d",
                            remaining / 60000, (remaining / 1000) % 60);
                    status.setText(fragment.getString(R.string.qr_verified) + " · "
                            + fragment.getString(R.string.qr_expires_count, time));
                }
                handler.postDelayed(this, 1000);
            }
        };

        generate(fragment, image, profile, secret, issuedAt[0]);
        dialog.setOnDismissListener(d -> handler.removeCallbacksAndMessages(null));
        handler.post(tick);
    }

    private static void generate(Fragment fragment, ImageView image, Profile profile,
                                 String secret, long issuedAt) {
        if (image == null) {
            return;
        }
        try {
            String payload = QrPayload.create(profile.dni, profile.fullName, issuedAt, secret);
            if (!QrPayload.verify(payload, secret)) {
                return;
            }
            Bitmap bitmap = QrGenerator.generate(payload, 640);
            image.setImageBitmap(bitmap);
        } catch (WriterException e) {
            image.setImageResource(R.drawable.ic_qr_art);
        }
    }
}
