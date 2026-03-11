package service;


import dao.SubTaskDao;
import model.SubTask;

import java.util.List;

public class SubTaskService {

    private final SubTaskDao subTaskDao = new SubTaskDao();

    public SubTask addSubTask(Long taskId, SubTask subTask) {
        subTask.setTaskId(taskId);
        subTaskDao.save(subTask);
        return subTask;
    }

    public SubTask markSubTaskAsDone(Long id, boolean completed) {
        subTaskDao.updateStatus(id, completed);
        SubTask s = new SubTask();
        s.setId(id);
        s.setCompleted(completed);
        return s;
    }

    public List<SubTask> getSubTasksByTaskId(Long taskId) {
        return subTaskDao.findByTaskId(taskId);
    }
}
