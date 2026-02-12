package dao;

import model.SubTask;
import util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubTaskDao {

    public void save(SubTask subTask) {

        String sql = "INSERT INTO sub_task(title, is_completed, task_id) VALUES (?, ?, ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, subTask.getTitle());
            ps.setBoolean(2, subTask.isCompleted());
            ps.setLong(3, subTask.getTaskId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                subTask.setId(rs.getLong(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(Long id, boolean completed) {

        String sql = "UPDATE sub_task SET is_completed=? WHERE id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setBoolean(1, completed);
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SubTask> findByTaskId(Long taskId) {

        List<SubTask> list = new ArrayList<>();
        String sql = "SELECT * FROM sub_task WHERE task_id=?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, taskId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SubTask s = new SubTask();
                s.setId(rs.getLong("id"));
                s.setTitle(rs.getString("title"));
                s.setCompleted(rs.getBoolean("is_completed"));
                s.setTaskId(rs.getLong("task_id"));
                list.add(s);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
