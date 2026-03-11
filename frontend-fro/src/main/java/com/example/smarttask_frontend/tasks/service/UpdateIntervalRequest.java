package com.example.smarttask_frontend.tasks.service;

import java.time.LocalDateTime;

public class UpdateIntervalRequest {

    private LocalDateTime dueDate;  // start
    private LocalDateTime endDate;  // end

    public UpdateIntervalRequest() {}

    public UpdateIntervalRequest(LocalDateTime dueDate, LocalDateTime endDate) {
        this.dueDate = dueDate;
        this.endDate = endDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
