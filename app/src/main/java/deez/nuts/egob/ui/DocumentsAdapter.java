package deez.nuts.egob.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;
import deez.nuts.egob.database.entities.Document;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.Formatters;

public class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder> {

    public interface Listener {
        void onOpen(@NonNull Document document);

        void onMenu(@NonNull Document document, @NonNull View anchor);
    }

    private final List<Document> items = new ArrayList<>();
    private final Listener listener;

    public DocumentsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Document> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView subtitle;
        private final Chip verified;
        private final ImageButton menu;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.doc_title);
            subtitle = itemView.findViewById(R.id.doc_subtitle);
            verified = itemView.findViewById(R.id.doc_verified);
            menu = itemView.findViewById(R.id.doc_menu);
        }

        void bind(@NonNull Document document) {
            title.setText(document.title);
            subtitle.setText(itemView.getContext().getString(R.string.documents_uploaded_detail,
                    FileStorage.humanSize(document.fileSize), Formatters.date(document.createdAt)));
            verified.setText(document.verified
                    ? R.string.detail_verified : R.string.documents_unverified);
            int textColor = document.verified ? R.color.success : R.color.warning;
            int background = document.verified ? R.color.tertiary_fixed : R.color.surface_container_high;
            verified.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
            verified.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), background)));

            itemView.setOnClickListener(v -> listener.onOpen(document));
            menu.setOnClickListener(v -> listener.onMenu(document, menu));
        }
    }
}
