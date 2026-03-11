package service;

import dao.CategoryDao;
import model.CategoryDTO;

import java.util.List;

public class CategoryService {

    private final CategoryDao categoryDao = new CategoryDao();

    public List<CategoryDTO> getAllCategories() {
        return categoryDao.findAll();
    }

    public CategoryDTO getCategoryById(Long id) {
        return categoryDao.findById(id);
    }
}
