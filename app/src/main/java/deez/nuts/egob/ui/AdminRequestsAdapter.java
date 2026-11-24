package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;

public class AdminRequestsAdapter extends RecyclerView.Adapter<AdminRequestsAdapter.RequestViewHolder> {

    public interface Listener {
        void onAction(@NonNull AdminRequest request);
    }

    private final List<AdminRequest> items = new ArrayList<>();
    private final Listener listener;

    public AdminRequestsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<AdminRequest> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
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

        private final TextView id;
        private final TextView submitted;
        private final TextView initials;
        private final TextView citizen;
        private final TextView service;
        private final TextView officer;
        private final TextView action;
        private final Chip status;
        private final View actionRow;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.admin_req_id);
            submitted = itemView.findViewById(R.id.admin_req_submitted);
            initials = itemView.findViewById(R.id.admin_req_initials);
            citizen = itemView.findViewById(R.id.admin_req_citizen);
            service = itemView.findViewById(R.id.admin_req_service);
            officer = itemView.findViewById(R.id.admin_req_officer);
            action = itemView.findViewById(R.id.admin_req_action);
            status = itemView.findViewById(R.id.admin_req_status);
            actionRow = itemView.findViewById(R.id.admin_req_action_row);
        }

        void bind(@NonNull AdminRequest request) {
            id.setText(request.id);
            submitted.setText(request.submitted);
            initials.setText(request.initials);
            citizen.setText(request.citizen);
            service.setText(request.service);
            officer.setText(request.officer);
            action.setText(request.action);
            status.setText(request.status);

            int textColor;
            int backgroundColor;
            switch (request.statusType) {
                case AdminRequest.STATUS_ERROR:
                    textColor = R.color.error;
                    backgroundColor = R.color.error_container;
                    break;
                case AdminRequest.STATUS_WARNING:
                    textColor = R.color.warning;
                    backgroundColor = R.color.surface_container_high;
                    break;
                case AdminRequest.STATUS_SUCCESS:
                    textColor = R.color.success;
                    backgroundColor = R.color.tertiary_fixed;
                    break;
                case AdminRequest.STATUS_INFO:
                default:
                    textColor = R.color.secondary;
                    backgroundColor = R.color.surface_container_low;
                    break;
            }

            status.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
            status.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), backgroundColor)));
            actionRow.setOnClickListener(v -> listener.onAction(request));
        }
    }
}
