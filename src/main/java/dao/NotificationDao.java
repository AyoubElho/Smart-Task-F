package dao;


import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDao {

    public List<Notification> findByUser(Long userId) {
        List<Notification> notifications = new ArrayList<>();

        String sql = """
            SELECT id, user_id, message, is_read, created_at
            FROM notifications
            WHERE user_id = ?
            ORDER BY created_at DESC
        """;

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return notifications;
    }

    public Optional<Notification> findById(Long id, Long userId) {
        String sql = """
            SELECT id, user_id, message, is_read, created_at
            FROM notifications
            WHERE id = ? AND user_id = ?
        """;

        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, id);
            ps.setLong(2, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public void save(Notification notification) {
        String sql = """
            INSERT INTO notifications (user_id, message, is_read, created_at)
            VALUES (?, ?, false, CURRENT_TIMESTAMP)
        """;

        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, notification.getUserId());
            ps.setString(2, notification.getMessage());
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean markAsRead(Long id, Long userId) {
        String sql = """
            UPDATE notifications
            SET is_read = true
            WHERE id = ? AND user_id = ?
        """;

        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, id);
            ps.setLong(2, userId);

            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public int countUnread(Long userId) {
        String sql = """
            SELECT COUNT(*)
            FROM notifications
            WHERE user_id = ? AND is_read = false
        """;

        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return n;
    }
}
