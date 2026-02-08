package model;

import java.time.LocalDateTime;

public class UpdateDueDateRequest {
    private LocalDateTime dueDate;
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}
