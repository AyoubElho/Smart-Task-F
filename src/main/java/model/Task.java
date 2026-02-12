    package model;

    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;

    public class Task {

        private long id;
        private String title;
        private String description;
        private Priority priority;
        private Status status;
        private LocalDateTime dueDate;
        private LocalDateTime createdAt;
        private String googleEventId;
        private LocalDateTime endDate;
        private Recurrence recurrence;
        private Long parentTaskId;
        private LocalDateTime nextRun;

        public Recurrence getRecurrence() {
            return recurrence;
        }

        public void setRecurrence(Recurrence recurrence) {
            this.recurrence = recurrence;
        }

        public Long getParentTaskId() {
            return parentTaskId;
        }

        public void setParentTaskId(Long parentTaskId) {
            this.parentTaskId = parentTaskId;
        }

        public LocalDateTime getNextRun() {
            return nextRun;
        }

        public void setNextRun(LocalDateTime nextRun) {
            this.nextRun = nextRun;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }
        private LocalDateTime startDate;
        public LocalDateTime getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
        }


        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
        }

        public String getGoogleEventId() {
            return googleEventId;
        }

        public void setGoogleEventId(String googleEventId) {
            this.googleEventId = googleEventId;
        }

        // relations by ID (JDBC way)
        private long userId;

        private List<Long> dependencyIds = new ArrayList<>();

        // ✅ NEW: category relation (JDBC)
        private Long categoryId;

        public Task() {}

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public LocalDateTime getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }
        public List<Long> getDependencyIds() {
            return dependencyIds;
        }

        public void setDependencyIds(List<Long> dependencyIds) {
            this.dependencyIds = dependencyIds;
        }
    }
