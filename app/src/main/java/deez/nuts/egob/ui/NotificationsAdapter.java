package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface Listener {
        void onAction(@NonNull NotificationItem item);

        void onDismiss(@NonNull NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final Listener listener;

    public NotificationsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<NotificationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView category;
        private final TextView time;
        private final TextView title;
        private final TextView body;
        private final TextView action;
        private final TextView footer;
        private final View actionRow;
        private final View chipsRow;
        private final Chip chipPrimary;
        private final Chip chipSecondary;
        private final ImageButton dismiss;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notification_icon);
            category = itemView.findViewById(R.id.notification_category);
            time = itemView.findViewById(R.id.notification_time);
            title = itemView.findViewById(R.id.notification_title);
            body = itemView.findViewById(R.id.notification_body);
            action = itemView.findViewById(R.id.notification_action);
            footer = itemView.findViewById(R.id.notification_footer);
            actionRow = itemView.findViewById(R.id.notification_action_row);
            chipsRow = itemView.findViewById(R.id.notification_chips);
            chipPrimary = itemView.findViewById(R.id.notification_chip_primary);
            chipSecondary = itemView.findViewById(R.id.notification_chip_secondary);
            dismiss = itemView.findViewById(R.id.btn_dismiss_notification);
        }

        void bind(@NonNull NotificationItem item) {
            icon.setImageResource(item.iconRes);
            icon.setBackgroundResource(item.iconBackgroundRes);
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), item.accentColorRes)));

            category.setText(item.category);
            category.setTextColor(ContextCompat.getColor(itemView.getContext(), item.accentColorRes));
            time.setText(item.time);
            title.setText(item.title);
            body.setText(item.body);

            if (item.chipPrimary == null) {
                chipPrimary.setVisibility(View.GONE);
            } else {
                chipPrimary.setVisibility(View.VISIBLE);
                chipPrimary.setText(item.chipPrimary);
            }

            if (item.chipSecondary == null) {
                chipSecondary.setVisibility(View.GONE);
            } else {
                chipSecondary.setVisibility(View.VISIBLE);
                chipSecondary.setText(item.chipSecondary);
            }
            chipsRow.setVisibility(item.chipPrimary == null && item.chipSecondary == null
                    ? View.GONE : View.VISIBLE);

            if (item.action == null) {
                actionRow.setVisibility(View.GONE);
            } else {
                actionRow.setVisibility(View.VISIBLE);
                action.setText(item.action);
                actionRow.setOnClickListener(v -> listener.onAction(item));
            }

            if (item.footer == null) {
                footer.setVisibility(View.GONE);
            } else {
                footer.setVisibility(View.VISIBLE);
                footer.setText(item.footer);
            }

            dismiss.setVisibility(item.dismissible ? View.VISIBLE : View.GONE);
            dismiss.setOnClickListener(v -> listener.onDismiss(item));
        }
    }
}
