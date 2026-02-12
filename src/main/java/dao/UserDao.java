package dao;

import model.User;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                users.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public void updateVerification(User user) {

        String sql = """
        UPDATE users
        SET verified = ?, verification_code = ?
        WHERE email = ?
    """;

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setBoolean(1, user.isVerified());
            ps.setString(2, user.getVerificationCode());
            ps.setString(3, user.getEmail());
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Add this method to UserDao.java
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password = ? WHERE id = ?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setLong(4, user.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (
            Connection c = DBConnection.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void save(User user) {

        String sql = """
        INSERT INTO users(username, email, password, created_at, verification_code, verified)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));
            ps.setString(5, user.getVerificationCode());
            ps.setBoolean(6, user.isVerified());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private User map(ResultSet rs) throws SQLException {
        User u = new User();

        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        u.setVerificationCode(rs.getString("verification_code"));
        u.setVerified(rs.getBoolean("verified"));

        return u;
    }

}
