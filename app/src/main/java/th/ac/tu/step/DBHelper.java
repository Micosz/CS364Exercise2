package th.ac.tu.step;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Management: สร้างตาราง SQLite และเมธอด Insert, Query, Delete (แนวทาง MA10 SQLite CRUD)
 * ชื่อเมธอด insert() / all() / delete() คงไว้ตามข้อตกลงใน Issue #3 เพื่อให้ MainActivity และงาน #4/#5 เรียกได้เหมือนเดิม
 */
final class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "step_by_step.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "sessions";
    private static final String COL_ID = "id";
    private static final String COL_STARTED_AT = "started_at";
    private static final String COL_DURATION_MS = "duration_ms";
    private static final String COL_STEPS = "steps";
    private static final String COL_SHAKES = "shakes";

    DBHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_STARTED_AT + " INTEGER NOT NULL, "
                + COL_DURATION_MS + " INTEGER NOT NULL CHECK(" + COL_DURATION_MS + " >= 0), "
                + COL_STEPS + " INTEGER NOT NULL CHECK(" + COL_STEPS + " >= 0), "
                + COL_SHAKES + " INTEGER NOT NULL CHECK(" + COL_SHAKES + " >= 0))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // Create — คืน ID ใหม่; ใช้ insertOrThrow เพื่อให้ try/catch ใน MainActivity จับกรณีบันทึกไม่สำเร็จได้
    long insert(ActivitySession session) {
        ContentValues values = new ContentValues();
        values.put(COL_STARTED_AT, session.startedAt);
        values.put(COL_DURATION_MS, session.durationMs);
        values.put(COL_STEPS, session.steps);
        values.put(COL_SHAKES, session.shakes);
        return getWritableDatabase().insertOrThrow(TABLE, null, values);
    }

    // Read (one)
    ActivitySession getById(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE, null,
                COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c != null && c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    // Read (all) — ล่าสุดก่อนด้วย id DESC
    List<ActivitySession> all() {
        List<ActivitySession> items = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null,
                null, null, COL_ID + " DESC")) {
            while (c.moveToNext())
                items.add(fromCursor(c));
        }
        return items;
    }

    // Delete — คืน true เมื่อลบได้หนึ่งแถว
    boolean delete(long id) {
        return getWritableDatabase().delete(TABLE, COL_ID + "=?",
                new String[]{String.valueOf(id)}) == 1;
    }

    private ActivitySession fromCursor(Cursor c) {
        return new ActivitySession(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(COL_STARTED_AT)),
                c.getLong(c.getColumnIndexOrThrow(COL_DURATION_MS)),
                c.getInt(c.getColumnIndexOrThrow(COL_STEPS)),
                c.getInt(c.getColumnIndexOrThrow(COL_SHAKES)));
    }
}
