package deez.nuts.egob.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.AuditLog;
import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.database.entities.Profile;
import deez.nuts.egob.util.FileShare;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.SessionManager;

public class DocumentsFragment extends Fragment implements TabFragment {

    private final List<Document> uploaded = new ArrayList<>();
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importDocument);

    private AppRepository repository;
    private DocumentsAdapter adapter;
    private View root;
    private View emptyState;
    private Profile profile;
    private int userId = -1;

    public DocumentsFragment() {
        super(R.layout.fragment_documents);
    }

    @Override
    public int getTabId() {
        return R.id.nav_documents;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        MainActivity activity = (MainActivity) requireActivity();
        TextView subtitle = view.findViewById(R.id.top_bar_subtitle);
        subtitle.setText(R.string.documents_subtitle);

        view.findViewById(R.id.btn_assistant).setOnClickListener(v -> activity.openAssistant());
        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> activity.openNotifications());
        view.findViewById(R.id.top_bar_avatar).setOnClickListener(v -> activity.openProfile());

        View.OnClickListener inspect = v -> QrDialog.show(this);
        view.findViewById(R.id.card_nid).setOnClickListener(inspect);
        view.findViewById(R.id.card_license).setOnClickListener(inspect);
        view.findViewById(R.id.card_medicare).setOnClickListener(inspect);
        view.findViewById(R.id.card_show_qr).setOnClickListener(inspect);

        view.findViewById(R.id.btn_add_document).setOnClickListener(v ->
                filePicker.launch(new String[]{"application/pdf", "image/*"}));

        view.findViewById(R.id.row_download_pdf).setOnClickListener(v -> downloadProfile());
        view.findViewById(R.id.row_share).setOnClickListener(v -> shareProfile());
        view.findViewById(R.id.row_audit).setOnClickListener(v -> showAudit());

        emptyState = view.findViewById(R.id.empty_documents);
        RecyclerView list = view.findViewById(R.id.list_documents);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DocumentsAdapter(new DocumentsAdapter.Listener() {
            @Override
            public void onOpen(@NonNull Document document) {
                openDocument(document);
            }

            @Override
            public void onMenu(@NonNull Document document, @NonNull View anchor) {
                showDocumentMenu(document);
            }
        });
        list.setAdapter(adapter);

        if (userId <= 0) {
            activity.openLogin();
            return;
        }
        loadProfile();
        loadDocuments();
    }

    private void loadProfile() {
        repository.currentSession(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded() || root == null) {
                    return;
                }
                profile = value.profile;
                bindProfile();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindProfile() {
        if (profile == null) {
            return;
        }
        ((TextView) root.findViewById(R.id.text_nid_name)).setText(profile.fullName);
        ((TextView) root.findViewById(R.id.text_nid_dob)).setText(profile.dateOfBirth);
        ((TextView) root.findViewById(R.id.text_nid_number)).setText(profile.dni);
        ((TextView) root.findViewById(R.id.text_documents_count)).setText(
                getString(R.string.documents_active_credentials_count, 3));
    }

    private void loadDocuments() {
        repository.getDocuments(userId, new RepositoryCallback<List<Document>>() {
            @Override
            public void onSuccess(List<Document> value) {
                if (!isAdded() || root == null) {
                    return;
                }
                bindIdentityDocuments(value);
                uploaded.clear();
                for (Document document : value) {
                    if ("uploaded".equals(document.type)) {
                        uploaded.add(document);
                    }
                }
                adapter.submitList(new ArrayList<>(uploaded));
                emptyState.setVisibility(uploaded.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindIdentityDocuments(List<Document> documents) {
        for (Document document : documents) {
            if ("identity".equals(document.type)) {
                ((TextView) root.findViewById(R.id.text_nid_number)).setText(document.documentNumber);
                ((TextView) root.findViewById(R.id.text_nid_expires)).setText(
                        getString(R.string.documents_expires_value, document.expiresAt));
            } else if ("license".equals(document.type)) {
                ((TextView) root.findViewById(R.id.text_license_number)).setText(document.documentNumber);
                ((TextView) root.findViewById(R.id.text_license_class)).setText(
                        getString(R.string.documents_class_c));
                ((TextView) root.findViewById(R.id.text_license_expires)).setText(
                        getString(R.string.documents_expires_value, document.expiresAt));
            } else if ("health".equals(document.type)) {
                ((TextView) root.findViewById(R.id.text_medicare_policy)).setText(document.documentNumber);
                ((TextView) root.findViewById(R.id.text_medicare_dependent)).setText(
                        R.string.documents_primary);
                ((TextView) root.findViewById(R.id.text_medicare_copay)).setText(
                        R.string.documents_copay_zero);
            }
        }
    }

    private void importDocument(@Nullable Uri uri) {
        if (uri == null || root == null) {
            return;
        }
        Snackbar.make(root, R.string.common_loading, Snackbar.LENGTH_SHORT).show();
        repository.copyFile(uri, new RepositoryCallback<FileStorage.StoredFile>() {
            @Override
            public void onSuccess(FileStorage.StoredFile value) {
                if (!isAdded()) {
                    return;
                }
                repository.addDocument(userId, value, value.name, new RepositoryCallback<Document>() {
                    @Override
                    public void onSuccess(Document document) {
                        if (!isAdded() || root == null) {
                            return;
                        }
                        Snackbar.make(root, R.string.documents_saved, Snackbar.LENGTH_LONG).show();
                        loadDocuments();
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

    private void openDocument(@NonNull Document document) {
        if (document.path != null && !document.path.isEmpty()) {
            File file = new File(document.path);
            if (file.exists()) {
                try {
                    FileShare.open(requireContext(), file,
                            document.mimeType == null ? "application/pdf" : document.mimeType);
                    return;
                } catch (RuntimeException ignored) {
                    // Fall through to the metadata dialog.
                }
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(document.title)
                .setMessage(getString(R.string.documents_detail_body,
                        document.documentNumber, document.issuedAt, document.expiresAt,
                        FileStorage.humanSize(document.fileSize)))
                .setPositiveButton(R.string.action_done, null)
                .show();
    }

    private void showDocumentMenu(@NonNull Document document) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(document.title)
                .setItems(new CharSequence[]{
                        getString(R.string.common_open),
                        getString(R.string.common_share),
                        getString(R.string.common_delete)
                }, (dialog, which) -> {
                    if (which == 0) {
                        openDocument(document);
                    } else if (which == 1) {
                        shareUploaded(document);
                    } else {
                        confirmDelete(document);
                    }
                })
                .show();
    }

    private void confirmDelete(@NonNull Document document) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.documents_delete_title)
                .setMessage(getString(R.string.documents_delete_body, document.title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.common_delete, (dialog, which) ->
                        repository.deleteDocument(userId, document, new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                if (!isAdded() || root == null) {
                                    return;
                                }
                                Snackbar.make(root, R.string.documents_deleted, Snackbar.LENGTH_SHORT).show();
                                loadDocuments();
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded() && root != null) {
                                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }))
                .show();
    }

    private void shareUploaded(@NonNull Document document) {
        if (document.path == null || document.path.isEmpty()) {
            Snackbar.make(root, R.string.documents_open_error, Snackbar.LENGTH_LONG).show();
            return;
        }
        File file = new File(document.path);
        if (!file.exists()) {
            Snackbar.make(root, R.string.documents_open_error, Snackbar.LENGTH_LONG).show();
            return;
        }
        FileShare.share(requireContext(), file,
                document.mimeType == null ? "application/pdf" : document.mimeType,
                getString(R.string.common_share_title));
    }

    private void downloadProfile() {
        if (root == null) {
            return;
        }
        repository.downloadProfilePdf(userId, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File value) {
                if (!isAdded() || root == null) {
                    return;
                }
                try {
                    File saved = FileShare.saveToDownloads(requireContext(), value,
                            "egob_identidad.pdf");
                    Snackbar.make(root, getString(R.string.documents_pdf_saved, saved.getAbsolutePath()),
                            Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Snackbar.make(root, R.string.profile_download_error, Snackbar.LENGTH_LONG).show();
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

    private void shareProfile() {
        if (root == null) {
            return;
        }
        repository.downloadProfilePdf(userId, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File value) {
                if (!isAdded()) {
                    return;
                }
                FileShare.share(requireContext(), value, "application/pdf",
                        getString(R.string.common_share_title));
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showAudit() {
        repository.getAuditLogsForUser(userId, 25, new RepositoryCallback<List<AuditLog>>() {
            @Override
            public void onSuccess(List<AuditLog> value) {
                if (!isAdded()) {
                    return;
                }
                StringBuilder builder = new StringBuilder();
                if (value.isEmpty()) {
                    builder.append(getString(R.string.documents_audit_empty));
                } else {
                    for (AuditLog log : value) {
                        builder.append(Formatters.dateTime(log.createdAt))
                                .append(" · ").append(log.details).append('\n');
                    }
                }
                AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.documents_audit_title)
                        .setMessage(builder.toString())
                        .setPositiveButton(R.string.action_done, null)
                        .create();
                dialog.show();
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
