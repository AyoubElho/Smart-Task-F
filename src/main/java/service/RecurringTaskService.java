package service;

import dao.TaskDao;
import model.Recurrence;
import model.Status;
import model.Task;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class RecurringTaskService {

    private final TaskDao dao = new TaskDao();

    public void generateAll() {

        List<Task> templates = dao.findRecurringTemplates();

        System.out.println("Templates found: " + templates.size());

        for (Task template : templates) {

            System.out.println("Generating from template ID: " + template.getId());

            Task copy = cloneTask(template);
            dao.save(copy);

            updateNextRun(template);
        }
    }


    private Task cloneTask(Task t) {

        Task copy = new Task();

        copy.setTitle(t.getTitle());
        copy.setDescription(t.getDescription());
        copy.setPriority(t.getPriority());
        copy.setStatus(Status.TODO);

        copy.setDueDate(t.getNextRun());
        copy.setEndDate(t.getNextRun().plusHours(1));

        copy.setCreatedAt(LocalDateTime.now()); // ⭐ ADD THIS

        copy.setUserId(t.getUserId());
        copy.setParentTaskId(t.getId());

        return copy;
    }


    private void updateNextRun(Task t) {

        LocalDateTime next = t.getNextRun();

        if (next == null)
            next = LocalDateTime.now();

        switch (t.getRecurrence()) {
            case DAILY -> next = next.plusDays(1);
            case WEEKLY -> next = next.plusWeeks(1);
            case MONTHLY -> next = next.plusMonths(1);
        }

        t.setNextRun(next);
        dao.updateNextRun(t.getId(), Timestamp.valueOf(next));
    }

}
