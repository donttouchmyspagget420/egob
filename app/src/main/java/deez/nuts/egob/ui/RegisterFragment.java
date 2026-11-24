package deez.nuts.egob.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class RegisterFragment extends Fragment {

    private final Calendar dateOfBirth = Calendar.getInstance();

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> close());

        MaterialAutoCompleteTextView district = view.findViewById(R.id.input_district);
        String[] districts = getResources().getStringArray(R.array.districts);
        district.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, districts));
        district.setText(districts[0], false);

        TextInputEditText dob = view.findViewById(R.id.input_dob);
        dateOfBirth.set(1989, Calendar.OCTOBER, 14);
        dob.setOnClickListener(v -> showDatePicker(dob));

        MaterialCheckBox consent = view.findViewById(R.id.check_consent);
        view.findViewById(R.id.btn_register).setOnClickListener(v -> {
            if (!consent.isChecked()) {
                Snackbar.make(view, R.string.register_consent_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            ((MainActivity) requireActivity()).openHome();
        });

        view.findViewById(R.id.link_sign_in).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                ((MainActivity) requireActivity()).openLogin();
            }
        });
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
            requireActivity().finish();
        }
    }
}
