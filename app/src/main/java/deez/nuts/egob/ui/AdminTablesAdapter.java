package deez.nuts.egob.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import deez.nuts.egob.R;
import deez.nuts.egob.data.DbTableInfo;

public class AdminTablesAdapter extends RecyclerView.Adapter<AdminTablesAdapter.Holder> {

    public interface Listener {
        void onTableSelected(DbTableInfo table);
    }

    private final List<DbTableInfo> items = new ArrayList<>();
    private final Listener listener;

    public AdminTablesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<DbTableInfo> tables) {
        items.clear();
        if (tables != null) {
            items.addAll(tables);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_table, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DbTableInfo table = items.get(position);
        holder.name.setText(table.name);
        holder.count.setText(holder.itemView.getContext()
                .getString(R.string.admin_table_rows, table.rowCount));
        holder.itemView.setOnClickListener(v -> listener.onTableSelected(table));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView count;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_table_name);
            count = itemView.findViewById(R.id.text_table_count);
        }
    }
}
