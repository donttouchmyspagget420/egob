package deez.nuts.egob.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.SessionManager;

public class ServiceDetailFragment extends Fragment {

    public static final String ARG_TITLE = "arg_title";
    public static final String ARG_DESCRIPTION = "arg_description";
    public static final String ARG_SERVICE_ID = "arg_service_id";

    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::selectFile);

    private MaterialCardView courierCard;
    private MaterialCardView pickupCard;
    private View courierRadio;
    private View pickupRadio;
    private MaterialButton submitButton;
    private View uploadZone;
    private View uploadedFile;
    private TextView uploadedName;
    private TextView uploadedMeta;
    private AppRepository repository;
    private Service service;
    private FileStorage.StoredFile storedFile;
    private boolean courierSelected = true;
    private int serviceId;
    private int userId;
    private boolean submitting;

    public ServiceDetailFragment() {
        super(R.layout.fragment_service_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        Bundle args = getArguments();
        serviceId = args == null ? 0 : args.getInt(ARG_SERVICE_ID, 0);
        String title = args == null ? null : args.getString(ARG_TITLE);
        String description = args == null ? null : args.getString(ARG_DESCRIPTION);
        if (title != null) {
            ((TextView) view.findViewById(R.id.detail_title)).setText(title);
        }
        if (description != null) {
            ((TextView) view.findViewById(R.id.detail_description)).setText(description);
        }

        courierCard = view.findViewById(R.id.card_courier);
        pickupCard = view.findViewById(R.id.card_pickup);
        courierRadio = view.findViewById(R.id.radio_courier);
        pickupRadio = view.findViewById(R.id.radio_pickup);
        submitButton = view.findViewById(R.id.btn_submit_request);
        uploadZone = view.findViewById(R.id.upload_zone);
        uploadedFile = view.findViewById(R.id.uploaded_file);
        uploadedName = view.findViewById(R.id.detail_uploaded_name);
        uploadedMeta = view.findViewById(R.id.detail_uploaded_meta);

        courierCard.setOnClickListener(v -> selectDelivery(true));
        pickupCard.setOnClickListener(v -> selectDelivery(false));

        uploadZone.setOnClickListener(v -> filePicker.launch(new String[]{"application/pdf", "image/*"}));
        view.findViewById(R.id.btn_select_files).setOnClickListener(v ->
                filePicker.launch(new String[]{"application/pdf", "image/*"}));
        view.findViewById(R.id.btn_remove_file).setOnClickListener(v -> {
            storedFile = null;
            uploadedFile.setVisibility(View.GONE);
            uploadZone.setVisibility(View.VISIBLE);
        });

        submitButton.setOnClickListener(v -> submitRequest());
        selectDelivery(true);

        if (serviceId > 0) {
            loadService();
        }
    }

    private void loadService() {
        repository.getService(serviceId, new RepositoryCallback<Service>() {
            @Override
            public void onSuccess(Service value) {
                if (!isAdded() || getView() == null) {
                    return;
                }
                service = value;
                bindService();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindService() {
        if (service == null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }
        ((MaterialToolbar) view.findViewById(R.id.toolbar)).setSubtitle(service.authority);
        ((TextView) view.findViewById(R.id.detail_title)).setText(service.name);
        ((TextView) view.findViewById(R.id.detail_description)).setText(service.description);
        ((TextView) view.findViewById(R.id.detail_fee_value)).setText(
                service.fee <= 0 ? getString(R.string.service_free) : Formatters.currency(service.fee));
        ((TextView) view.findViewById(R.id.detail_validity_value)).setText(service.validity);
        ((TextView) view.findViewById(R.id.detail_turnaround_value)).setText(service.time);
        ((TextView) view.findViewById(R.id.detail_courier_price)).setText(
                "+ " + Formatters.currency(courierFee() - service.fee));
        selectDelivery(courierSelected);
    }

    private void selectFile(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        Snackbar.make(getView(), R.string.common_loading, Snackbar.LENGTH_SHORT).show();
        repository.copyFile(uri, new RepositoryCallback<FileStorage.StoredFile>() {
            @Override
            public void onSuccess(FileStorage.StoredFile value) {
                if (!isAdded() || getView() == null) {
                    return;
                }
                storedFile = value;
                uploadedName.setText(value.name);
                uploadedMeta.setText(getString(R.string.detail_file_selected,
                        FileStorage.humanSize(value.size), value.mimeType));
                uploadZone.setVisibility(View.GONE);
                uploadedFile.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void submitRequest() {
        View view = getView();
        if (view == null || submitting) {
            return;
        }
        if (userId <= 0) {
            Snackbar.make(view, R.string.detail_requires_session, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (service == null) {
            Snackbar.make(view, R.string.detail_load_error, Snackbar.LENGTH_LONG).show();
            return;
        }

        submitting = true;
        submitButton.setEnabled(false);
        String delivery = courierSelected
                ? getString(R.string.detail_delivery_courier)
                : getString(R.string.detail_delivery_pickup);
        List<FileStorage.StoredFile> files = storedFile == null
                ? Collections.emptyList()
                : Collections.singletonList(storedFile);

        repository.createRequest(userId, service.id, delivery, courierFee(), new ArrayList<>(files),
                new RepositoryCallback<RequestWithService>() {
                    @Override
                    public void onSuccess(RequestWithService value) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        submitting = false;
                        submitButton.setEnabled(true);
                        Snackbar.make(getView(), getString(R.string.detail_request_created,
                                value.request.reference), Snackbar.LENGTH_LONG).show();
                        getView().postDelayed(() -> {
                            if (isAdded()) {
                                ((MainActivity) requireActivity()).openRequests();
                            }
                        }, 1200);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        submitting = false;
                        submitButton.setEnabled(true);
                        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                    }
                });
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

        submitButton.setText(getString(R.string.detail_submit_amount,
                Formatters.currency(courierFee())));
    }

    private double courierFee() {
        double base = service == null ? 0 : service.fee;
        return courierSelected ? base + 5.0 : base;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
