package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import deez.nuts.egob.R;
import deez.nuts.egob.database.entities.Attachment;
import deez.nuts.egob.database.pojos.RequestWithService;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.Formatters;
import deez.nuts.egob.util.RequestStatus;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    public interface Listener {
        void onAction(@NonNull RequestWithService row);

        void onToggle(@NonNull RequestWithService row, boolean expanded);
    }

    private final List<RequestWithService> items = new ArrayList<>();
    private final Map<Integer, List<Attachment>> attachments = new HashMap<>();
    private final Set<Integer> expanded = new HashSet<>();
    private final Listener listener;

    public RequestsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<RequestWithService> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setAttachments(int requestId, @NonNull List<Attachment> value) {
        attachments.put(requestId, value);
        int index = indexOf(requestId);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    public boolean isExpanded(int requestId) {
        return expanded.contains(requestId);
    }

    private int indexOf(int requestId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).request != null && items.get(i).request.id == requestId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {

        private final TextView ref;
        private final TextView date;
        private final TextView title;
        private final TextView noteLabel;
        private final TextView note;
        private final View noteBox;
        private final TextView step;
        private final TextView percent;
        private final ProgressBar progress;
        private final TextView action;
        private final View actionRow;
        private final View details;
        private final TextView detailService;
        private final TextView detailDelivery;
        private final TextView detailFee;
        private final TextView detailOfficer;
        private final TextView detailAttachments;
        private final Chip status;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ref = itemView.findViewById(R.id.req_ref);
            date = itemView.findViewById(R.id.req_date);
            title = itemView.findViewById(R.id.req_title);
            noteLabel = itemView.findViewById(R.id.req_note_label);
            note = itemView.findViewById(R.id.req_note);
            noteBox = itemView.findViewById(R.id.req_note_box);
            step = itemView.findViewById(R.id.req_step);
            percent = itemView.findViewById(R.id.req_percent);
            progress = itemView.findViewById(R.id.req_progress);
            action = itemView.findViewById(R.id.req_action);
            actionRow = itemView.findViewById(R.id.req_action_row);
            details = itemView.findViewById(R.id.req_details);
            detailService = itemView.findViewById(R.id.req_detail_service);
            detailDelivery = itemView.findViewById(R.id.req_detail_delivery);
            detailFee = itemView.findViewById(R.id.req_detail_fee);
            detailOfficer = itemView.findViewById(R.id.req_detail_officer);
            detailAttachments = itemView.findViewById(R.id.req_detail_attachments);
            status = itemView.findViewById(R.id.req_status);
        }

        void bind(@NonNull RequestWithService row) {
            if (row.request == null) {
                return;
            }
            ref.setText(row.request.reference);
            date.setText(Formatters.date(row.request.createdAt));
            title.setText(row.serviceName);
            status.setText(RequestStatus.label(row.request.status));
            styleStatus(status, row.request.status);

            if (row.request.notes == null || row.request.notes.trim().isEmpty()) {
                noteBox.setVisibility(View.GONE);
            } else {
                noteBox.setVisibility(View.VISIBLE);
                noteLabel.setText(R.string.requests_officer_note);
                note.setText(row.request.notes);
            }

            int progressValue = progressFor(row.request.status);
            progress.setProgress(progressValue);
            percent.setText(progressValue + "%");
            step.setText(stepLabel(itemView.getContext(), row.request.status));

            boolean isExpanded = expanded.contains(row.request.id);
            details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            String actionText;
            if (RequestStatus.ACTION.equals(row.request.status)) {
                actionText = itemView.getContext().getString(R.string.requests_action_upload);
            } else if (RequestStatus.COMPLETED.equals(row.request.status)) {
                actionText = itemView.getContext().getString(R.string.requests_action_download);
            } else {
                actionText = itemView.getContext().getString(isExpanded
                        ? R.string.requests_action_hide
                        : R.string.requests_action_details);
            }
            action.setText(actionText);

            detailService.setText(row.serviceName);
            detailDelivery.setText(row.request.deliveryMethod);
            detailFee.setText(Formatters.currency(row.request.fee));
            detailOfficer.setText(row.request.assignedOfficer);
            List<Attachment> files = attachments.get(row.request.id);
            if (files == null || files.isEmpty()) {
                detailAttachments.setText(R.string.requests_no_attachments);
            } else {
                StringBuilder builder = new StringBuilder();
                for (Attachment attachment : files) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append("• ").append(attachment.name)
                            .append(" (").append(FileStorage.humanSize(attachment.fileSize)).append(')');
                }
                detailAttachments.setText(builder.toString());
            }

            actionRow.setOnClickListener(v -> {
                if (RequestStatus.ACTION.equals(row.request.status)
                        || RequestStatus.COMPLETED.equals(row.request.status)) {
                    listener.onAction(row);
                    return;
                }
                boolean next = !expanded.contains(row.request.id);
                if (next) {
                    expanded.add(row.request.id);
                } else {
                    expanded.remove(row.request.id);
                }
                notifyItemChanged(getBindingAdapterPosition());
                listener.onToggle(row, next);
            });

            itemView.setOnClickListener(v -> {
                boolean next = !expanded.contains(row.request.id);
                if (next) {
                    expanded.add(row.request.id);
                } else {
                    expanded.remove(row.request.id);
                }
                notifyItemChanged(getBindingAdapterPosition());
                listener.onToggle(row, next);
            });
        }
    }

    private static int progressFor(String status) {
        if (RequestStatus.COMPLETED.equals(status) || RequestStatus.REJECTED.equals(status)) {
            return 100;
        }
        if (RequestStatus.ACTION.equals(status)) {
            return 50;
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return 75;
        }
        return 33;
    }

    private static String stepLabel(android.content.Context context, String status) {
        if (RequestStatus.ACTION.equals(status)) {
            return context.getString(R.string.requests_progress_action);
        }
        if (RequestStatus.IN_PROGRESS.equals(status)) {
            return context.getString(R.string.requests_progress_in_progress);
        }
        if (RequestStatus.COMPLETED.equals(status)) {
            return context.getString(R.string.requests_progress_completed);
        }
        if (RequestStatus.REJECTED.equals(status)) {
            return context.getString(R.string.requests_progress_rejected);
        }
        return context.getString(R.string.requests_progress_pending);
    }

    private static void styleStatus(Chip chip, String status) {
        int textColor;
        int background;
        if (RequestStatus.ACTION.equals(status) || RequestStatus.REJECTED.equals(status)) {
            textColor = R.color.error;
            background = R.color.error_container;
        } else if (RequestStatus.COMPLETED.equals(status)) {
            textColor = R.color.success;
            background = R.color.tertiary_fixed;
        } else {
            textColor = R.color.secondary;
            background = R.color.surface_container_low;
        }
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), textColor));
        chip.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(chip.getContext(), background)));
    }
}
