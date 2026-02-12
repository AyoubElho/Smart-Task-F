package dao;

import model.Priority;
import model.Status;
import model.Task;
import util.DBConnection;
import model.Recurrence;

import java.sql.*;
import java.util.*;

public class TaskDao {

    public List<Task> findAll() {
        return query("SELECT * FROM task");
    }

    public List<Task> findByUserId(Long userId) {
        return query("SELECT * FROM task WHERE user_id = ?", userId);
    }

    public Task findById(Long id) {
        List<Task> list = query("SELECT * FROM task WHERE id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Task> findSharedWithUser(Long userId) {
        String sql = """
                    SELECT t.* FROM task t
                    JOIN task_shared ts ON t.id = ts.task_id
                    WHERE ts.user_id = ?
                """;
        return query(sql, userId);
    }

    // ===================== SAVE =====================

    public void save(Task task) {

        String sql = """
        INSERT INTO task(
            title, description, priority, status,
            due_date, end_date, created_at,
            user_id, category_id, google_event_id,
            recurrence, parent_task_id, next_run
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps =
                        c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getPriority().name());
            ps.setString(4, task.getStatus().name());

            // due_date
            if (task.getDueDate() != null)
                ps.setTimestamp(5, Timestamp.valueOf(task.getDueDate()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            // end_date
            if (task.getEndDate() != null)
                ps.setTimestamp(6, Timestamp.valueOf(task.getEndDate()));
            else
                ps.setNull(6, Types.TIMESTAMP);

            // created_at
            ps.setTimestamp(7, Timestamp.valueOf(task.getCreatedAt()));

            // user_id
            ps.setLong(8, task.getUserId());

            // category_id
            if (task.getCategoryId() != null)
                ps.setLong(9, task.getCategoryId());
            else
                ps.setNull(9, Types.BIGINT);

            // google_event_id
            if (task.getGoogleEventId() != null)
                ps.setString(10, task.getGoogleEventId());
            else
                ps.setNull(10, Types.VARCHAR);

            // recurrence enum
            if (task.getRecurrence() != null)
                ps.setString(11, task.getRecurrence().name());
            else
                ps.setNull(11, Types.VARCHAR);

            // parent task
            if (task.getParentTaskId() != null)
                ps.setLong(12, task.getParentTaskId());
            else
                ps.setNull(12, Types.BIGINT);

            // next run
            if (task.getNextRun() != null)
                ps.setTimestamp(13, Timestamp.valueOf(task.getNextRun()));
            else
                ps.setNull(13, Types.TIMESTAMP);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getLong(1));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void updateEndDate(Long taskId, Timestamp endDate) {
        execute("UPDATE task SET end_date = ? WHERE id = ?", endDate, taskId);
    }
    public List<Task> findRecurringTemplates() {
        String sql = """
        SELECT * FROM task
        WHERE recurrence IS NOT NULL
        AND parent_task_id IS NULL
        AND next_run <= NOW()
    """;

        return query(sql);
    }


    // ===================== UPDATE =====================

    public void updateStatus(Long taskId, Status status) {
        execute("UPDATE task SET status = ? WHERE id = ?", status.name(), taskId);
    }

    public void updateDueDate(Long taskId, Timestamp dueDate) {
        execute("UPDATE task SET due_date = ? WHERE id = ?", dueDate, taskId);
    }

    // ✅ NEW: Google event update
    public void updateGoogleEventId(Long taskId, String eventId) {
        execute("UPDATE task SET google_event_id = ? WHERE id = ?", eventId, taskId);
    }

    public void shareTask(Long taskId, Long userId) {
        execute("INSERT INTO task_shared(task_id, user_id) VALUES (?, ?)", taskId, userId);
    }

    // ===================== DEPENDENCIES =====================

    public void addDependency(Long taskId, Long dependencyId) {
        if (taskId.equals(dependencyId)) return;

        String sql = "INSERT INTO task_dependencies (task_id, depends_on_id) VALUES (?, ?)";
        execute(sql, taskId, dependencyId);
    }

    public void removeDependency(Long taskId, Long dependencyId) {
        String sql = "DELETE FROM task_dependencies WHERE task_id = ? AND depends_on_id = ?";
        execute(sql, taskId, dependencyId);
    }

    private List<Long> getDependencyIds(Long taskId) {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT depends_on_id FROM task_dependencies WHERE task_id = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("depends_on_id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ids;
    }
    public void updateNextRun(Long id, Timestamp next) {
        execute("UPDATE task SET next_run = ? WHERE id = ?", next, id);
    }

    // ===================== QUERY HELPER =====================

    private List<Task> query(String sql, Object... params) {
        List<Task> list = new ArrayList<>();

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {

            for (int i = 0; i < params.length; i++)
                ps.setObject(i + 1, params[i]);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Task t = new Task();

                t.setId(rs.getLong("id"));
                t.setTitle(rs.getString("title"));
                t.setDescription(rs.getString("description"));
                t.setPriority(Priority.valueOf(rs.getString("priority")));
                t.setStatus(Status.valueOf(rs.getString("status")));

                Timestamp dueTs = rs.getTimestamp("due_date");
                t.setDueDate(dueTs != null ? dueTs.toLocalDateTime() : null);

                Timestamp endTs = rs.getTimestamp("end_date");
                t.setEndDate(endTs != null ? endTs.toLocalDateTime() : null);

                t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                t.setUserId(rs.getLong("user_id"));

                t.setCategoryId(rs.getObject("category_id", Long.class));
                t.setGoogleEventId(rs.getString("google_event_id"));

                // recurrence enum
                String rec = rs.getString("recurrence");

                if (rec != null && !rec.isBlank()) {
                    try {
                        t.setRecurrence(Recurrence.valueOf(rec.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.out.println("⚠ Invalid recurrence value in DB: " + rec);
                        t.setRecurrence(null);
                    }
                }


                // parent
                t.setParentTaskId(rs.getObject("parent_task_id", Long.class));

                // next run
                Timestamp nextTs = rs.getTimestamp("next_run");
                t.setNextRun(nextTs != null ? nextTs.toLocalDateTime() : null);

                t.setDependencyIds(getDependencyIds(t.getId()));

                list.add(t);
            }



        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private void execute(String sql, Object... params) {
        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            for (int i = 0; i < params.length; i++)
                ps.setObject(i + 1, params[i]);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
