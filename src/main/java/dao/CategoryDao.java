package dao;

import model.CategoryDTO;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    // =========================
    // GET ALL CATEGORIES
    // =========================
    public List<CategoryDTO> findAll() {

        String sql = "SELECT id, name, color_code FROM category";
        List<CategoryDTO> categories = new ArrayList<>();

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                CategoryDTO cat = new CategoryDTO();
                cat.setId(rs.getLong("id"));
                cat.setName(rs.getString("name"));
                cat.setColorCode(rs.getString("color_code"));
                categories.add(cat);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return categories;
    }

    // =========================
    // GET CATEGORY BY ID
    // =========================
    public CategoryDTO findById(Long id) {

        String sql = "SELECT id, name, color_code FROM category WHERE id = ?";
        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)
        ) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                CategoryDTO cat = new CategoryDTO();
                cat.setId(rs.getLong("id"));
                cat.setName(rs.getString("name"));
                cat.setColorCode(rs.getString("color_code"));
                return cat;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
