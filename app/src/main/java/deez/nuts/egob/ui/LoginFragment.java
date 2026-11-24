package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class LoginFragment extends Fragment {

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton tabNational = view.findViewById(R.id.tab_national);
        MaterialButton tabPasskey = view.findViewById(R.id.tab_passkey);
        tabNational.setOnClickListener(v -> selectTab(tabNational, tabPasskey));
        tabPasskey.setOnClickListener(v -> {
            selectTab(tabPasskey, tabNational);
            Snackbar.make(view, R.string.login_biometric_request, Snackbar.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_authenticate).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openHome());
        view.findViewById(R.id.btn_register).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRegister());
        view.findViewById(R.id.card_biometric).setOnClickListener(v ->
                Snackbar.make(view, R.string.login_biometric_request, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.link_lost_access).setOnClickListener(v ->
                Snackbar.make(view, R.string.login_lost_access, Snackbar.LENGTH_SHORT).show());
    }

    private void selectTab(@NonNull MaterialButton selected, @NonNull MaterialButton unselected) {
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        int muted = ContextCompat.getColor(requireContext(), R.color.on_surface_variant);
        int white = ContextCompat.getColor(requireContext(), R.color.surface_container_lowest);

        selected.setBackgroundTintList(ColorStateList.valueOf(white));
        selected.setTextColor(primary);
        selected.setIconTint(ColorStateList.valueOf(primary));
        selected.setElevation(dp(1));

        unselected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)));
        unselected.setTextColor(muted);
        unselected.setIconTint(ColorStateList.valueOf(muted));
        unselected.setElevation(0);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
