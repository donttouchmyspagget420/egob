package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class ServiceDetailFragment extends Fragment {

    public static final String ARG_TITLE = "arg_title";
    public static final String ARG_DESCRIPTION = "arg_description";

    private MaterialCardView courierCard;
    private MaterialCardView pickupCard;
    private View courierRadio;
    private View pickupRadio;
    private MaterialButton submitButton;
    private boolean courierSelected = true;

    public ServiceDetailFragment() {
        super(R.layout.fragment_service_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString(ARG_TITLE);
            String description = args.getString(ARG_DESCRIPTION);
            if (title != null) {
                ((TextView) view.findViewById(R.id.detail_title)).setText(title);
            }
            if (description != null) {
                ((TextView) view.findViewById(R.id.detail_description)).setText(description);
            }
        }

        courierCard = view.findViewById(R.id.card_courier);
        pickupCard = view.findViewById(R.id.card_pickup);
        courierRadio = view.findViewById(R.id.radio_courier);
        pickupRadio = view.findViewById(R.id.radio_pickup);
        submitButton = view.findViewById(R.id.btn_submit_request);

        courierCard.setOnClickListener(v -> selectDelivery(true));
        pickupCard.setOnClickListener(v -> selectDelivery(false));

        View uploadZone = view.findViewById(R.id.upload_zone);
        uploadZone.setOnClickListener(v ->
                Snackbar.make(view, R.string.detail_file_picker, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_select_files).setOnClickListener(v ->
                Snackbar.make(view, R.string.detail_file_picker, Snackbar.LENGTH_SHORT).show());

        View uploadedFile = view.findViewById(R.id.uploaded_file);
        view.findViewById(R.id.btn_remove_file).setOnClickListener(v -> {
            uploadedFile.setVisibility(View.GONE);
            uploadZone.setVisibility(View.VISIBLE);
        });

        submitButton.setOnClickListener(v -> {
            Snackbar.make(view, R.string.detail_request_sent, Snackbar.LENGTH_LONG).show();
            view.postDelayed(() -> ((MainActivity) requireActivity()).openRequests(), 1200);
        });

        selectDelivery(true);
    }

    private void selectDelivery(boolean courier) {
        courierSelected = courier;
        int secondary = ContextCompat.getColor(requireContext(), R.color.secondary);
        int outline = ContextCompat.getColor(requireContext(), R.color.surface_container);

        courierCard.setStrokeColor(courier ? secondary : outline);
        courierCard.setStrokeWidth(courier ? dp(2) : dp(1));
        pickupCard.setStrokeColor(courier ? outline : secondary);
        pickupCard.setStrokeWidth(courier ? dp(1) : dp(2));

        courierRadio.setBackgroundResource(courier
                ? R.drawable.bg_radio_selected : R.drawable.bg_radio_unselected);
        pickupRadio.setBackgroundResource(courier
                ? R.drawable.bg_radio_unselected : R.drawable.bg_radio_selected);

        submitButton.setText(courierSelected ? R.string.detail_submit : R.string.detail_submit_pickup);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
