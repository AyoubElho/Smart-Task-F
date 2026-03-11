package com.example.smarttask_frontend.dto;

import java.time.LocalDateTime;

public class UpdateDueDateRequest {

    private LocalDateTime dueDate;

    public UpdateDueDateRequest() {
    }

    public UpdateDueDateRequest(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}
