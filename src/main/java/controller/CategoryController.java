package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.CategoryDTO;
import service.CategoryService;

import java.io.IOException;
import java.util.List;

@WebServlet("/categories/*")
public class CategoryController extends HttpServlet {

    private final CategoryService categoryService = new CategoryService();
    private final Gson gson = new GsonBuilder().create();

    // =========================
    // GET
    // =========================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        // GET /categories
        if (path == null || path.equals("/")) {
            List<CategoryDTO> categories = categoryService.getAllCategories();
            write(resp, categories);
            return;
        }

        // GET /categories/{id}
        try {
            Long id = Long.parseLong(path.substring(1));
            CategoryDTO category = categoryService.getCategoryById(id);

            if (category == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                write(resp, "Category not found");
                return;
            }

            write(resp, category);

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "Invalid category id");
        }
    }

    // =========================
    // HELPERS
    // =========================
    private void write(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}
