package dao;

import model.Priority;
import model.Status;
import model.Task;
import util.DBConnection;

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

    // ===================== SAVE (WITH CATEGORY) =====================
    public void save(Task task) {

        String sql = """
            INSERT INTO task(
                title, description, priority, status,
                due_date, created_at, user_id, category_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getPriority().name());
            ps.setString(4, task.getStatus().name());

            // due_date (nullable)
            if (task.getDueDate() != null)
                ps.setTimestamp(5, Timestamp.valueOf(task.getDueDate()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            ps.setTimestamp(6, Timestamp.valueOf(task.getCreatedAt()));
            ps.setLong(7, task.getUserId());

            // 🔥 category_id (nullable)
            if (task.getCategoryId() != null)
                ps.setLong(8, task.getCategoryId());
            else
                ps.setNull(8, Types.BIGINT);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(Long taskId, Status status) {
        execute("UPDATE task SET status = ? WHERE id = ?", status.name(), taskId);
    }

    public void updateDueDate(Long taskId, Timestamp dueDate) {
        execute("UPDATE task SET due_date = ? WHERE id = ?", dueDate, taskId);
    }

    public void shareTask(Long taskId, Long userId) {
        execute("INSERT INTO task_shared(task_id, user_id) VALUES (?, ?)", taskId, userId);
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

                t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                t.setUserId(rs.getLong("user_id"));

                // 🔥 category_id
                Long categoryId = rs.getObject("category_id", Long.class);
                t.setCategoryId(categoryId);

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
