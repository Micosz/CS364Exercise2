package th.ac.tu.step;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/** In-memory store used by the shared UI until the database branch is merged. */
final class DBHelper {
    private static final List<ActivitySession> rows = new ArrayList<>();
    private static long nextId = 1;

    DBHelper(Context context) {}

    void insert(ActivitySession session) { rows.add(0, session.withId(nextId++)); }

    List<ActivitySession> all() { return new ArrayList<>(rows); }

    boolean delete(long id) { return rows.removeIf(session -> session.id == id); }

    void close() {}
}
