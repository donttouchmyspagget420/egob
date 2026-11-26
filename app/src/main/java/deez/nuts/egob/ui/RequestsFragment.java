package deez.nuts.egob.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Attachment;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.FileShare;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.RequestStatus;
import deez.nuts.egob.util.SessionManager;

public class RequestsFragment extends Fragment implements TabFragment {

    private final List<RequestWithService> allRequests = new ArrayList<>();
    private final List<RequestWithService> visibleRequests = new ArrayList<>();
    private AppRepository repository;
    private RequestsAdapter adapter;
    private View root;
    private View emptyState;
    private RecyclerView list;
    private Chip chipAll;
    private Chip chipProgress;
    private Chip chipAction;
    private Chip chipCompleted;
    private String selectedFilter = "all";
    private int userId = -1;
    private int pendingUploadRequestId = -1;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && pendingUploadRequestId > 0) {
                    uploadAttachment(uri, pendingUploadRequestId);
                }
                pendingUploadRequestId = 0;
            });

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
        root = view;
        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        MainActivity activity = (MainActivity) requireActivity();
        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.requests_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v -> activity.openProfile());
        view.findViewById(R.id.btn_explore_services).setOnClickListener(v -> activity.openServices());

        list = view.findViewById(R.id.list_requests);
        emptyState = view.findViewById(R.id.empty_requests);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RequestsAdapter(new RequestsAdapter.Listener() {
            @Override
            public void onAction(@NonNull RequestWithService row) {
                if (RequestStatus.ACTION.equals(row.request.status)) {
                    pendingUploadRequestId = row.request.id;
                    filePicker.launch(new String[]{"application/pdf", "image/*"});
                } else if (RequestStatus.COMPLETED.equals(row.request.status)) {
                    downloadReceipt(row.request.id);
                }
            }

            @Override
            public void onToggle(@NonNull RequestWithService row, boolean expanded) {
                if (expanded) {
                    loadAttachments(row.request.id);
                }
            }
        });
        list.setAdapter(adapter);

        chipAll = view.findViewById(R.id.chip_requests_all);
        chipProgress = view.findViewById(R.id.chip_requests_progress);
        chipAction = view.findViewById(R.id.chip_requests_action);
        chipCompleted = view.findViewById(R.id.chip_requests_completed);

        ChipGroup filters = view.findViewById(R.id.chip_group_requests);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            selectedFilter = chip == null ? "all" : String.valueOf(chip.getTag());
            applyFilter();
        });

        if (userId <= 0) {
            ((MainActivity) requireActivity()).openLogin();
            return;
        }
        loadRequests();
    }

    private void loadRequests() {
        repository.getRequests(userId, new RepositoryCallback<List<RequestWithService>>() {
            @Override
            public void onSuccess(List<RequestWithService> value) {
                if (!isAdded() || root == null) {
                    return;
                }
                allRequests.clear();
                allRequests.addAll(value);
                updateFilterLabels();
                applyFilter();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateFilterLabels() {
        int progress = 0;
        int action = 0;
        int completed = 0;
        for (RequestWithService row : allRequests) {
            if (row.request == null) {
                continue;
            }
            if (RequestStatus.PENDING.equals(row.request.status)
                    || RequestStatus.IN_PROGRESS.equals(row.request.status)) {
                progress++;
            } else if (RequestStatus.ACTION.equals(row.request.status)) {
                action++;
            } else if (RequestStatus.COMPLETED.equals(row.request.status)) {
                completed++;
            }
        }
        chipAll.setText(getString(R.string.requests_filter_all, allRequests.size()));
        chipProgress.setText(getString(R.string.requests_filter_progress, progress));
        chipAction.setText(getString(R.string.requests_filter_action, action));
        chipCompleted.setText(getString(R.string.requests_filter_completed, completed));
    }

    private void applyFilter() {
        if (adapter == null) {
            return;
        }
        visibleRequests.clear();
        for (RequestWithService row : allRequests) {
            if (row.request == null) {
                continue;
            }
            if (matches(row.request.status)) {
                visibleRequests.add(row);
            }
        }
        adapter.submitList(new ArrayList<>(visibleRequests));
        boolean empty = visibleRequests.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matches(String status) {
        switch (selectedFilter) {
            case "progress":
                return RequestStatus.PENDING.equals(status) || RequestStatus.IN_PROGRESS.equals(status);
            case "action":
                return RequestStatus.ACTION.equals(status);
            case "completed":
                return RequestStatus.COMPLETED.equals(status);
            case "all":
            default:
                return true;
        }
    }

    private void loadAttachments(int requestId) {
        repository.getAttachments(requestId, new RepositoryCallback<List<Attachment>>() {
            @Override
            public void onSuccess(List<Attachment> value) {
                if (isAdded() && adapter != null) {
                    adapter.setAttachments(requestId, value);
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void uploadAttachment(@NonNull Uri uri, int requestId) {
        if (root == null) {
            return;
        }
        Snackbar.make(root, R.string.common_loading, Snackbar.LENGTH_SHORT).show();
        repository.copyFile(uri, new RepositoryCallback<FileStorage.StoredFile>() {
            @Override
            public void onSuccess(FileStorage.StoredFile value) {
                if (!isAdded()) {
                    return;
                }
                repository.addAttachment(userId, requestId, value, new RepositoryCallback<Attachment>() {
                    @Override
                    public void onSuccess(Attachment attachment) {
                        if (!isAdded() || root == null) {
                            return;
                        }
                        Snackbar.make(root, R.string.requests_attachment_added, Snackbar.LENGTH_LONG).show();
                        loadRequests();
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded() && root != null) {
                            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void downloadReceipt(int requestId) {
        if (root == null) {
            return;
        }
        repository.downloadRequestPdf(requestId, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File value) {
                if (!isAdded() || root == null) {
                    return;
                }
                try {
                    File saved = FileShare.saveToDownloads(requireContext(), value,
                            "egob_solicitud_" + requestId + ".pdf");
                    Snackbar.make(root, getString(R.string.requests_pdf_ready, saved.getAbsolutePath()),
                            Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Snackbar.make(root, R.string.requests_pdf_error, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}
