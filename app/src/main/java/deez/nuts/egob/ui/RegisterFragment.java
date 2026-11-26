package deez.nuts.egob.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.Registration;
import deez.nuts.egob.data.RepositoryCallback;

public class RegisterFragment extends Fragment {

    private static final Pattern NIN_PATTERN =
            Pattern.compile("^[A-Z]{3}-\\d{4}-\\d{4}-[0-9A-Z]{2}$");

    private final Calendar dateOfBirth = Calendar.getInstance();
    private AppRepository repository;
    private MaterialButton registerButton;
    private View root;

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> close());

        MaterialAutoCompleteTextView district = view.findViewById(R.id.input_district);
        String[] districts = getResources().getStringArray(R.array.districts);
        district.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, districts));
        district.setText(districts[0], false);

        TextInputEditText dob = view.findViewById(R.id.input_dob);
        dateOfBirth.set(1989, Calendar.OCTOBER, 14);
        dob.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(dateOfBirth.getTime()));
        dob.setOnClickListener(v -> showDatePicker(dob));

        TextInputEditText nin = view.findViewById(R.id.input_nin);
        TextInputEditText fullName = view.findViewById(R.id.input_full_name);
        TextInputEditText phone = view.findViewById(R.id.input_phone);
        TextInputEditText password = view.findViewById(R.id.input_password);
        TextInputEditText passwordConfirm = view.findViewById(R.id.input_password_confirm);
        MaterialCheckBox consent = view.findViewById(R.id.check_consent);
        registerButton = view.findViewById(R.id.btn_register);

        registerButton.setOnClickListener(v -> {
            if (!consent.isChecked()) {
                Snackbar.make(root, R.string.register_consent_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            String ninValue = text(nin).toUpperCase(Locale.US);
            String nameValue = text(fullName);
            String phoneValue = text(phone);
            String passwordValue = text(password);
            String confirmValue = text(passwordConfirm);

            if (!NIN_PATTERN.matcher(ninValue).matches()) {
                nin.setError(getString(R.string.register_nin_invalid));
                nin.requestFocus();
                return;
            }
            if (nameValue.length() < 3) {
                fullName.setError(getString(R.string.register_name_required));
                fullName.requestFocus();
                return;
            }
            if (phoneValue.replaceAll("[^0-9]", "").length() < 7) {
                phone.setError(getString(R.string.register_phone_invalid));
                phone.requestFocus();
                return;
            }
            if (passwordValue.length() < 8) {
                password.setError(getString(R.string.register_password_short));
                password.requestFocus();
                return;
            }
            if (!passwordValue.equals(confirmValue)) {
                passwordConfirm.setError(getString(R.string.register_password_mismatch));
                passwordConfirm.requestFocus();
                return;
            }

            setLoading(true);
            Registration registration = new Registration(ninValue, nameValue, text(dob),
                    phoneValue, String.valueOf(district.getText()), passwordValue);
            repository.register(registration, new RepositoryCallback<AuthSession>() {
                @Override
                public void onSuccess(AuthSession value) {
                    if (!isAdded()) {
                        return;
                    }
                    setLoading(false);
                    Snackbar.make(root, R.string.register_success, Snackbar.LENGTH_SHORT).show();
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
        });

        view.findViewById(R.id.link_sign_in).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                ((MainActivity) requireActivity()).openLogin();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (registerButton == null) {
            return;
        }
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? R.string.register_loading : R.string.register_submit);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void showDatePicker(@NonNull TextInputEditText target) {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            dateOfBirth.set(year, month, day);
            String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(dateOfBirth.getTime());
            target.setText(formatted);
        }, dateOfBirth.get(Calendar.YEAR), dateOfBirth.get(Calendar.MONTH),
                dateOfBirth.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void close() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            ((MainActivity) requireActivity()).openLogin();
        }
    }
}
