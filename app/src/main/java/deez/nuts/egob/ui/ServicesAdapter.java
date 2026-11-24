package deez.nuts.egob.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;

public class ServicesAdapter extends RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder> {

    public interface Listener {
        void onServiceClick(@NonNull ServiceItem item);

        void onBookmarkClick(@NonNull ServiceItem item, int position);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final Listener listener;

    public ServicesAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<ServiceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView authority;
        private final TextView badge;
        private final TextView title;
        private final TextView description;
        private final TextView time;
        private final TextView price;
        private final TextView action;
        private final ImageButton bookmark;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.service_icon);
            authority = itemView.findViewById(R.id.service_authority);
            badge = itemView.findViewById(R.id.service_badge);
            title = itemView.findViewById(R.id.service_title);
            description = itemView.findViewById(R.id.service_desc);
            time = itemView.findViewById(R.id.service_time);
            price = itemView.findViewById(R.id.service_price);
            action = itemView.findViewById(R.id.service_action);
            bookmark = itemView.findViewById(R.id.btn_bookmark);
        }

        void bind(@NonNull ServiceItem item) {
            icon.setImageResource(item.iconRes);
            authority.setText(item.authority);
            title.setText(item.title);
            description.setText(item.description);
            time.setText(item.time);
            price.setText(item.price);
            action.setText(item.action);

            if (item.badge == null) {
                badge.setVisibility(View.GONE);
            } else {
                badge.setVisibility(View.VISIBLE);
                badge.setText(item.badge);
            }

            bookmark.setImageResource(item.bookmarked
                    ? R.drawable.ic_bookmark
                    : R.drawable.ic_bookmark_border);

            itemView.setOnClickListener(v -> listener.onServiceClick(item));
            bookmark.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onBookmarkClick(item, position);
                }
            });
        }
    }
}
