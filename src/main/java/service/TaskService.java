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

        if (task.getStatus() == null)
            task.setStatus(Status.TODO);

        if (task.getCreatedAt() == null)
            task.setCreatedAt(LocalDateTime.now());

        taskDao.save(task);
        return task;
    }

    public Task createTask(Task task, Long userId) {

        task.setUserId(userId);

        if (task.getStatus() == null)
            task.setStatus(Status.TODO);

        task.setCreatedAt(LocalDateTime.now());

        if (task.getTitle() == null)
            task.setTitle("");

        if (task.getDescription() == null)
            task.setDescription("");

        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Task must have start date");
        }

        boolean recurring = task.getRecurrence() != null
                && task.getRecurrence() != model.Recurrence.NONE;

        if (!recurring && task.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "Non-recurring task must have end date"
            );
        }

        if (task.getEndDate() != null &&
                task.getEndDate().isBefore(task.getDueDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        if (task.getRecurrence() != null &&
                task.getRecurrence() != model.Recurrence.NONE) {

            // first run = start date
            LocalDateTime next = task.getDueDate();

            switch (task.getRecurrence()) {
                case DAILY -> next = next.plusDays(1);
                case WEEKLY -> next = next.plusWeeks(1);
                case MONTHLY -> next = next.plusMonths(1);
            }

            task.setNextRun(next);
            task.setParentTaskId(null);
        }

        try {
            CategoryDTO category = aiClient.suggestCategory(task);
            task.setCategoryId(category.getId());
        } catch (Exception e) {
            task.setCategoryId(5L);
        }

        taskDao.save(task);
        return task;
    }

    // ===================== GOOGLE =====================

    public void updateGoogleEventId(Long taskId, String eventId) {
        taskDao.updateGoogleEventId(taskId, eventId);
    }

    // ===================== UPDATE =====================

    public void updateStatus(Long taskId, Status status) {
        taskDao.updateStatus(taskId, status);
    }

    // start time = due_date column
    public void updateStartDate(Long taskId, LocalDateTime start) {
        taskDao.updateDueDate(taskId, Timestamp.valueOf(start));
    }

    // end time = end_date column
    public void updateEndDate(Long taskId, LocalDateTime end) {
        taskDao.updateEndDate(taskId, Timestamp.valueOf(end));
    }

    // update both (calendar drag)
    public void updateInterval(Long taskId,
                               LocalDateTime start,
                               LocalDateTime end) {

        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        taskDao.updateDueDate(taskId, Timestamp.valueOf(start));
        taskDao.updateEndDate(taskId, Timestamp.valueOf(end));
    }


    // ==================== SHARE =====================

    public void shareTask(Long taskId, Long userId) {
        taskDao.shareTask(taskId, userId);
    }

    // ===================== DEPENDENCIES =====================

    public void addDependency(Long taskId, Long dependencyId) {

        if (taskId.equals(dependencyId)) {
            throw new IllegalArgumentException(
                    "Task cannot depend on itself"
            );
        }

        taskDao.addDependency(taskId, dependencyId);
    }

    public void removeDependency(Long taskId, Long dependencyId) {
        taskDao.removeDependency(taskId, dependencyId);
    }
}
