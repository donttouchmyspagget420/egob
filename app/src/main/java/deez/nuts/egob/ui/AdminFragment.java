package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import deez.nuts.egob.R;
import deez.nuts.egob.data.AdminStats;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.AuthSession;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.AuditLog;
import deez.nuts.egob.database.entities.User;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.RequestStatus;

public class AdminFragment extends Fragment {

    private AppRepository repository;
    private View root;
    private AdminRequestsAdapter adapter;
    private User admin;
    private boolean loading;

    public AdminFragment() {
        super(R.layout.fragment_admin);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        root = view;
        repository = AppRepository.get(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView list = view.findViewById(R.id.list_admin_requests);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminRequestsAdapter(this::showRequestActions);
        list.setAdapter(adapter);

        view.findViewById(R.id.btn_daily_audit).setOnClickListener(v -> showAuditReport());
        view.findViewById(R.id.btn_manage_queues).setOnClickListener(v -> showQueues());
        view.findViewById(R.id.btn_broadcast).setOnClickListener(v -> showBroadcast());
        view.findViewById(R.id.btn_audit_queue).setOnClickListener(v -> showAuditReport());

        loadSession();
    }

    private void loadSession() {
        repository.currentSession(new RepositoryCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession value) {
                if (!isAdded() || root == null) {
                    return;
                }
                if (!"admin".equals(value.user.role)) {
                    showRestricted();
                    return;
                }
                admin = value.user;
                loadStats();
                loadQueue();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showRestricted() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_restricted_title)
                .setMessage(R.string.admin_restricted_body)
                .setPositiveButton(R.string.action_done, (dialog, which) -> {
                    if (isAdded()) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void loadStats() {
        repository.getAdminStats(new RepositoryCallback<AdminStats>() {
            @Override
            public void onSuccess(AdminStats value) {
                if (!isAdded() || root == null) {
                    return;
                }
                ((TextView) root.findViewById(R.id.admin_kpi_active)).setText(
                        String.valueOf(value.activeRequests));
                ((TextView) root.findViewById(R.id.admin_kpi_pending)).setText(
                        String.valueOf(value.pendingVerifications));
                ((TextView) root.findViewById(R.id.admin_kpi_issued)).setText(
                        String.valueOf(value.completedTotal));
                ((TextView) root.findViewById(R.id.admin_kpi_citizens)).setText(
                        String.valueOf(value.citizens));
                ((TextView) root.findViewById(R.id.admin_kpi_compliance)).setText(
                        value.complianceText());
                ((Chip) root.findViewById(R.id.admin_pending_chip)).setText(
                        getString(R.string.admin_pending_review_count, value.pendingVerifications));
                ((TextView) root.findViewById(R.id.admin_vault_label)).setText(
                        R.string.admin_citizens);
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadQueue() {
        if (loading) {
            return;
        }
        loading = true;
        repository.getAllRequests(new RepositoryCallback<List<RequestWithService>>() {
            @Override
            public void onSuccess(List<RequestWithService> value) {
                loading = false;
                if (!isAdded() || root == null) {
                    return;
                }
                List<RequestWithService> sorted = new ArrayList<>(value);
                Collections.sort(sorted, (left, right) -> Integer.compare(
                        priority(left.request.status), priority(right.request.status)));
                List<AdminRequest> items = new ArrayList<>();
                for (RequestWithService row : sorted) {
                    items.add(toAdminRequest(row));
                }
                adapter.submitList(items);
            }

            @Override
            public void onError(String message) {
                loading = false;
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private AdminRequest toAdminRequest(@NonNull RequestWithService row) {
        int statusType;
        String action;
        switch (row.request.status) {
            case RequestStatus.ACTION:
                statusType = AdminRequest.STATUS_ERROR;
                action = getString(R.string.admin_view_dossier_action);
                break;
            case RequestStatus.PENDING:
                statusType = AdminRequest.STATUS_WARNING;
                action = getString(R.string.admin_review_dossier);
                break;
            case RequestStatus.IN_PROGRESS:
                statusType = AdminRequest.STATUS_INFO;
                action = getString(R.string.admin_review_dossier);
                break;
            case RequestStatus.REJECTED:
                statusType = AdminRequest.STATUS_ERROR;
                action = getString(R.string.admin_view_dossier_action);
                break;
            case RequestStatus.COMPLETED:
            default:
                statusType = AdminRequest.STATUS_SUCCESS;
                action = getString(R.string.admin_view_dossier_action);
                break;
        }
        AdminRequest request = new AdminRequest(row.request.reference,
                safe(row.citizenName), Formatters.initials(row.citizenName),
                safe(row.serviceName), Formatters.date(row.request.createdAt),
                safe(row.request.assignedOfficer), RequestStatus.label(row.request.status),
                statusType, action);
        request.requestId = row.request.id;
        request.rawStatus = row.request.status;
        return request;
    }

    private static int priority(String status) {
        if (RequestStatus.ACTION.equals(status)) {
            return 0;
        }
        if (RequestStatus.PENDING.equals(status)) {
            return 1;
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return 2;
        }
        if (RequestStatus.REJECTED.equals(status)) {
            return 3;
        }
        return 4;
    }

    private void showRequestActions(@NonNull AdminRequest request) {
        if (request.rawStatus == null
                || RequestStatus.COMPLETED.equals(request.rawStatus)
                || RequestStatus.REJECTED.equals(request.rawStatus)) {
            showDossier(request);
            return;
        }

        String[] actions = {
                getString(R.string.admin_approve_action),
                getString(R.string.admin_request_docs_action),
                getString(R.string.admin_reject_action),
                getString(R.string.admin_view_dossier_action)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_action_title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        confirmStatus(request, RequestStatus.COMPLETED,
                                getString(R.string.admin_approve_note));
                    } else if (which == 1) {
                        confirmStatus(request, RequestStatus.ACTION,
                                getString(R.string.admin_request_docs_note));
                    } else if (which == 2) {
                        confirmStatus(request, RequestStatus.REJECTED,
                                getString(R.string.admin_reject_note));
                    } else {
                        showDossier(request);
                    }
                })
                .show();
    }

    private void confirmStatus(@NonNull AdminRequest request, String status, String note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_action_title)
                .setMessage(getString(R.string.admin_action_confirm,
                        RequestStatus.label(status), request.id))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.common_confirm, (dialog, which) ->
                        updateStatus(request, status, note))
                .show();
    }

    private void updateStatus(@NonNull AdminRequest request, String status, String note) {
        if (admin == null) {
            return;
        }
        repository.updateRequestStatus(admin.id, request.requestId, status, note,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        if (!isAdded() || root == null) {
                            return;
                        }
                        Snackbar.make(root, getString(R.string.admin_status_updated_body,
                                request.id, RequestStatus.label(status)), Snackbar.LENGTH_LONG).show();
                        loadStats();
                        loadQueue();
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded() && root != null) {
                            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showDossier(@NonNull AdminRequest request) {
        repository.getAttachments(request.requestId, new RepositoryCallback<List<deez.nuts.egob.database.entities.Attachment>>() {
            @Override
            public void onSuccess(List<deez.nuts.egob.database.entities.Attachment> value) {
                if (!isAdded()) {
                    return;
                }
                String body = getString(R.string.admin_dossier_body,
                        request.service, request.citizen, request.officer,
                        getString(R.string.admin_dossier_attachments, value.size()));
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.admin_dossier_title, request.id))
                        .setMessage(body)
                        .setPositiveButton(R.string.action_done, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showAuditReport() {
        repository.getAdminStats(new RepositoryCallback<AdminStats>() {
            @Override
            public void onSuccess(AdminStats stats) {
                repository.getAuditLogs(12, new RepositoryCallback<List<AuditLog>>() {
                    @Override
                    public void onSuccess(List<AuditLog> logs) {
                        if (!isAdded()) {
                            return;
                        }
                        StringBuilder builder = new StringBuilder();
                        builder.append(getString(R.string.admin_audit_report,
                                stats.totalRequests, stats.activeRequests, stats.completedToday,
                                stats.complianceText())).append("\n\n");
                        if (logs.isEmpty()) {
                            builder.append(getString(R.string.admin_audit_empty));
                        } else {
                            for (AuditLog log : logs) {
                                builder.append(Formatters.dateTime(log.createdAt))
                                        .append(" · ").append(log.action)
                                        .append(" · ").append(log.details).append('\n');
                            }
                        }
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.admin_audit_report_title)
                                .setMessage(builder.toString())
                                .setPositiveButton(R.string.action_done, null)
                                .show();
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

    private void showQueues() {
        repository.getAdminStats(new RepositoryCallback<AdminStats>() {
            @Override
            public void onSuccess(AdminStats stats) {
                if (!isAdded()) {
                    return;
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.admin_queue_title)
                        .setMessage(getString(R.string.admin_queue_body, stats.activeRequests,
                                stats.pendingVerifications, stats.citizens))
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.admin_reassign, (dialog, which) -> reassign())
                        .show();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void reassign() {
        if (admin == null) {
            return;
        }
        repository.reassignQueue(admin.id, "Agente de turno", new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer value) {
                if (!isAdded() || root == null) {
                    return;
                }
                Snackbar.make(root, getString(R.string.admin_queue_reassigned_count, value),
                        Snackbar.LENGTH_LONG).show();
                loadQueue();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && root != null) {
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showBroadcast() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_broadcast, null, false);
        TextInputEditText title = content.findViewById(R.id.input_title);
        TextInputEditText body = content.findViewById(R.id.input_body);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_broadcast_title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    String titleValue = title.getText() == null ? "" : title.getText().toString().trim();
                    String bodyValue = body.getText() == null ? "" : body.getText().toString().trim();
                    if (titleValue.isEmpty() || bodyValue.isEmpty() || admin == null) {
                        Snackbar.make(root, R.string.admin_broadcast_required, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    repository.broadcast(admin.id, titleValue, bodyValue,
                            new RepositoryCallback<Integer>() {
                                @Override
                                public void onSuccess(Integer value) {
                                    if (!isAdded() || root == null) {
                                        return;
                                    }
                                    Snackbar.make(root, getString(R.string.admin_broadcast_sent_count, value),
                                            Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message) {
                                    if (isAdded() && root != null) {
                                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });
                })
                .show();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
