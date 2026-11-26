package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;

public class LoginFragment extends Fragment {

    private AppRepository repository;
    private MaterialButton authenticateButton;
    private TextInputEditText ninInput;
    private TextInputEditText passcodeInput;
    private MaterialCheckBox trustDevice;
    private View root;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());

        MaterialButton tabNational = view.findViewById(R.id.tab_national);
        MaterialButton tabPasskey = view.findViewById(R.id.tab_passkey);
        tabNational.setOnClickListener(v -> selectTab(tabNational, tabPasskey));
        tabPasskey.setOnClickListener(v -> {
            selectTab(tabPasskey, tabNational);
            loginWithDevice();
        });

        ninInput = view.findViewById(R.id.input_nin);
        passcodeInput = view.findViewById(R.id.input_passcode);
        trustDevice = view.findViewById(R.id.check_trust_device);
        authenticateButton = view.findViewById(R.id.btn_authenticate);
        authenticateButton.setOnClickListener(v -> authenticate());

        view.findViewById(R.id.btn_register).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openRegister());
        view.findViewById(R.id.card_biometric).setOnClickListener(v -> loginWithDevice());
        view.findViewById(R.id.link_lost_access).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.login_lost_access)
                        .setMessage(R.string.login_demo_hint)
                        .setPositiveButton(R.string.action_done, null)
                        .show());
    }

    private void authenticate() {
        String identifier = ninInput.getText() == null ? "" : ninInput.getText().toString().trim();
        String password = passcodeInput.getText() == null ? "" : passcodeInput.getText().toString();
        if (identifier.isEmpty() || password.isEmpty()) {
            Snackbar.make(root, "Ingresa tu NIN y tu código de acceso.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        repository.login(identifier, password, trustDevice.isChecked(), new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                ((MainActivity) requireActivity()).openHome();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loginWithDevice() {
        setLoading(true);
        repository.loginWithDevice(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                ((MainActivity) requireActivity()).openHome();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (authenticateButton == null) {
            return;
        }
        authenticateButton.setEnabled(!loading);
        authenticateButton.setText(loading ? R.string.login_loading : R.string.login_authenticate);
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
