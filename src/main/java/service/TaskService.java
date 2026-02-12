package service;

import ai.AIClient;
import dao.TaskDao;
import model.CategoryDTO;
import model.Status;
import model.Task;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class TaskService {

    private final TaskDao taskDao = new TaskDao();
    private final AIClient aiClient = new AIClient();

    // ===================== READ =====================
    public List<Task> getTasks() {
        return taskDao.findAll();
    }

    public Task findById(Long id) {
        return taskDao.findById(id);
    }

    public List<Task> getTasksByUser(Long userId) {
        return taskDao.findByUserId(userId);
    }

    public List<Task> getTasksSharedWithUser(Long userId) {
        return taskDao.findSharedWithUser(userId);
    }

    // ===================== CREATE =====================
    public Task save(Task task) {
        if (task.getStatus() == null) task.setStatus(Status.TODO);
        if (task.getCreatedAt() == null) task.setCreatedAt(LocalDateTime.now());
        taskDao.save(task);
        return task;
    }

    public Task createTask(Task task, Long userId) {
        task.setUserId(userId);
        task.setStatus(Status.TODO);
        task.setCreatedAt(LocalDateTime.now());

        if (task.getTitle() == null) task.setTitle("");
        if (task.getDescription() == null) task.setDescription("");

        try {
            CategoryDTO category = aiClient.suggestCategory(task);
            System.out.println("AI CATEGORY = " + category.getName());
            task.setCategoryId(category.getId());
        } catch (Exception e) {
            System.out.println("❌ AI FAILED");
            e.printStackTrace();
            task.setCategoryId(5L); // Default
        }

        taskDao.save(task);
        return task;
    }

    // ===================== UPDATE =====================
    public void updateStatus(Long taskId, Status status) {
        taskDao.updateStatus(taskId, status);
    }

    public void updateDueDate(Long taskId, LocalDateTime date) {
        taskDao.updateDueDate(taskId, Timestamp.valueOf(date));
    }

    // ===================== SHARE =====================
    public void shareTask(Long taskId, Long userId) {
        taskDao.shareTask(taskId, userId);
    }

    // ===================== ✅ NEW: DEPENDENCIES =====================
    public void addDependency(Long taskId, Long dependencyId) {
        if (taskId.equals(dependencyId)) {
            throw new IllegalArgumentException("Task cannot depend on itself");
        }
        taskDao.addDependency(taskId, dependencyId);
    }

    public void removeDependency(Long taskId, Long dependencyId) {
        taskDao.removeDependency(taskId, dependencyId);
    }
}