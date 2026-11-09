package th.ac.tu.step;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

final class ActivityAdapter extends ListAdapter<ActivitySession, ActivityAdapter.Row> {
    interface DeleteAction { void delete(long id); }

    private static final DiffUtil.ItemCallback<ActivitySession> CHANGES =
        new DiffUtil.ItemCallback<ActivitySession>() {
            @Override public boolean areItemsTheSame(ActivitySession old, ActivitySession next) {
                return old.id == next.id;
            }

            @Override public boolean areContentsTheSame(ActivitySession old, ActivitySession next) {
                return old.startedAt == next.startedAt && old.durationMs == next.durationMs
                    && old.steps == next.steps && old.shakes == next.shakes;
            }
        };

    private final Activity screen;
    private final RecyclerView list;
    private final DeleteAction deleteAction;
    private final DateFormat dates =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    ActivityAdapter(Activity activity, RecyclerView view, DeleteAction action) {
        super(CHANGES);
        screen = activity;
        list = view;
        deleteAction = action;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(this);
    }

    void show(List<ActivitySession> rows, TextView empty) {
        // Own a snapshot: sorting and later database refreshes must not mutate the caller's list.
        List<ActivitySession> snapshot = new ArrayList<>(rows);
        snapshot.sort(Comparator.comparingLong((ActivitySession s) -> s.startedAt)
            .thenComparingLong(s -> s.id).reversed());
        submitList(snapshot);
        empty.setText(R.string.history_empty);
        empty.setVisibility(snapshot.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(snapshot.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override public long getItemId(int position) { return getItem(position).id; }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView date, counts, duration;

        Row(View view) {
            super(view);
            date = view.findViewById(R.id.itemDate);
            counts = view.findViewById(R.id.itemCounts);
            duration = view.findViewById(R.id.itemDuration);
        }
    }

    @Override public Row onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_activity, parent, false));
    }

    private String duration(long milliseconds) {
        long seconds = milliseconds / 1000;
        return screen.getString(R.string.history_duration_value, seconds / 60, seconds % 60);
    }

    @Override public void onBindViewHolder(Row row, int position) {
        ActivitySession session = getItem(position);
        String date = dates.format(new Date(session.startedAt));
        String counts = screen.getString(R.string.history_counts, session.steps, session.shakes);
        String elapsed = screen.getString(R.string.history_duration, duration(session.durationMs));
        row.date.setText(date);
        row.counts.setText(counts);
        row.duration.setText(elapsed);
        row.itemView.setContentDescription(screen.getString(
            R.string.history_row_description, date, counts, elapsed));
        row.itemView.setOnClickListener(view -> details(session));
    }

    private void details(ActivitySession session) {
        View body = screen.getLayoutInflater().inflate(R.layout.dialog_activity_detail, null);
        TextView date = body.findViewById(R.id.detailDate);
        TextView metrics = body.findViewById(R.id.detailMetrics);
        date.setText(screen.getString(R.string.history_started_at,
            dates.format(new Date(session.startedAt))));
        metrics.setText(screen.getString(R.string.history_detail_metrics,
            duration(session.durationMs), session.steps, session.shakes));
        new AlertDialog.Builder(screen)
            .setTitle(R.string.history_detail_title)
            .setView(body)
            .setPositiveButton(R.string.history_close, null)
            // MainActivity owns confirmation, persistence, and the shared statistics refresh.
            .setNegativeButton(R.string.history_delete, (dialog, which) -> deleteAction.delete(session.id))
            .show();
    }
}
