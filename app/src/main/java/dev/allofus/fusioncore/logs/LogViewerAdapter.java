package dev.allofus.fusioncore.logs;

import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LogViewerAdapter extends RecyclerView.Adapter<LogViewerAdapter.LogViewHolder> {
    private List<String> logLines;

    public LogViewerAdapter(List<String> logLines) {
        this.logLines = logLines;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextColor(0xFFD4D4D4);
        tv.setTextSize(12f);
        return new LogViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.textView.setText(logLines.get(position));
    }

    @Override
    public int getItemCount() {
        return logLines.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public LogViewHolder(TextView itemView) {
            super(itemView);
            textView = itemView;
        }
    }
}
