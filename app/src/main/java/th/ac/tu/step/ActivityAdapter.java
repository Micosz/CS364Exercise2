package th.ac.tu.step;

import android.app.Activity;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

final class ActivityAdapter {
    interface DeleteAction { void delete(long id); }

    ActivityAdapter(Activity activity, RecyclerView view, DeleteAction action) {}

    void show(List<ActivitySession> rows, TextView empty) {
        empty.setText(rows.isEmpty() ? "ยังไม่มีประวัติกิจกรรม" : "กิจกรรมที่บันทึกไว้ " + rows.size() + " รอบ");
    }
}
