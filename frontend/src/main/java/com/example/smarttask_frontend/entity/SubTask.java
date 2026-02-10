package com.example.smarttask_frontend.entity;

public class SubTask {

    private Long id;
    private String title;
    private boolean completed; // ✅ better naming
    private Long taskId;
    // 🔹 Constructors
    public SubTask() {}

    public SubTask(Long id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    // 🔹 Getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    // 🔹 JavaFX ListView display
    @Override
    public String toString() {
        return completed ? "✔ " + title : title;
    }
}
