# SmartTask-F

SmartTask-F is a full-stack smart task manager built with a **Java Servlet backend** and a **JavaFX desktop frontend**.
It supports task CRUD, subtasks, dependencies, recurrence, sharing, comments with attachments, notifications, calendar planning, Google Calendar sync, and optional AI assistance.

## Project At A Glance

- Backend: Java 17, Jakarta Servlet, JDBC, MySQL, WebSocket
- Frontend: JavaFX (FXML + CSS), CalendarFX, Java HTTP client
- Realtime: Comments + Notifications via WebSocket
- Optional integrations: AI service (`localhost:9090`), Gmail SMTP verification, Google Calendar API

## Architecture

```text
JavaFX Desktop App (frontend-fro)
    |
    | HTTP + WebSocket
    v
Servlet Backend (this root module, WAR)
    |
    v
MySQL (db_tasks)

Optional:
- AI microservice on http://localhost:9090
- Google Calendar API
- Gmail SMTP for email verification
```

## Main Features

1. Authentication
- User registration + login
- Email verification flow using a generated code
- Profile update from Settings page

2. Task Management
- Create, edit, delete tasks
- Priority (`LOW`, `MEDIUM`, `HIGH`)
- Status (`TODO`, `IN_PROGRESS`, `DONE`)
- Date interval handling (start/end)
- Dependency graph between tasks
- Recurrence (`NONE`, `DAILY`, `WEEKLY`, `MONTHLY`)

3. Smart Features
- AI parse: convert natural language into a task
- AI category suggestion on create
- AI productivity insights on dashboard

4. Collaboration
- Share tasks with other users
- Task comments and live updates
- File attachments in comments

5. Productivity and Calendar
- Dashboard analytics cards and trend chart
- Calendar week view (CalendarFX)
- Drag/resize to update task intervals
- Google Calendar event sync with event-id persistence

6. Notifications
- REST notifications + unread counter
- Realtime notification refresh via WebSocket

7. Background Recurrence Engine
- `RecurringScheduler` runs every 30 seconds
- Duplicates due recurring templates
- Updates next run time

## Screenshots (All Main Pages)

### Login / Register
![Login page](docs/screenshots/login-page.png)

### Dashboard
![Dashboard page](docs/screenshots/dashboard-page.png)

### My Tasks
![MyTasks page](docs/screenshots/mytasks-page.png)

### Create Task
![Create task page](docs/screenshots/create-task-page.png)

### Update Task
![Update task page](docs/screenshots/update-task-page.png)

### Calendar
![Calendar page](docs/screenshots/calendar-page.png)

### Settings
![Settings page](docs/screenshots/settings-page.png)

### Share Task
![Share task page](docs/screenshots/share-task-page.png)

### Subtasks
![Subtasks page](docs/screenshots/subtasks-page.png)

## Repository Structure

```text
Smart-Task-F/
  src/main/java/                      # Backend (Servlet/JDBC)
    controller/                       # REST-like servlet controllers
    service/                          # Business logic
    dao/                              # SQL persistence
    model/                            # Backend DTO/entities
    socket/                           # Comment WebSocket endpoint
    ai/                               # Backend AI HTTP client
    mail/                             # SMTP verification
    util/                             # DB + JSON utils

  frontend-fro/
    src/main/java/com/example/smarttask_frontend/
      auth/ dashboard/ tasks/ subtasks/
      calendar/ settings/ comments/
      googleCalendarService/ aiClient/
    src/main/resources/views/         # FXML pages
    src/main/resources/styles/        # CSS themes
    src/main/resources/application.properties

  docs/screenshots/                   # README screenshots
```

## Backend API Reference

### Users (`/user/*`)

- `GET /user` -> list all users
- `POST /user/register` -> register
- `POST /user/login` -> login
- `POST /user/verify` -> verify email code
- `POST /user/update` -> update profile

### Tasks (`/tasks/*`)

- `GET /tasks` -> all tasks
- `GET /tasks/test` -> health check
- `GET /tasks/user/{userId}` -> tasks by owner
- `GET /tasks/shared/{userId}` -> tasks shared with user
- `GET /tasks/{id}` -> task by id
- `POST /tasks/create-task/id/{userId}` -> create task
- `POST /tasks/{taskId}/share/{userId}` -> share task
- `POST /tasks/{taskId}/dependency/{dependencyId}` -> add dependency
- `PUT /tasks/{id}` -> full update
- `PUT /tasks/{id}/status/{status}` -> status update
- `PUT /tasks/{id}/due-date` -> start date update
- `PUT /tasks/{id}/end-date` -> end date update
- `PUT /tasks/{id}/interval` -> update start+end interval
- `PUT /tasks/{id}/recurrence/{recurrence}` -> recurrence update
- `PUT /tasks/{id}/google-event/{eventId}` -> save Google event id
- `DELETE /tasks/{id}` -> delete task
- `DELETE /tasks/{taskId}/dependency/{dependencyId}` -> remove dependency

### Subtasks (`/subtasks/*`)

- `GET /subtasks/task/{taskId}` -> subtasks by task
- `POST /subtasks/add/{taskId}` -> add subtask
- `PUT /subtasks/{id}/status?is_completed=true|false` -> toggle completion

### Comments (`/comments/*`)

- `GET /comments/{taskId}` -> comment history
- `POST /comments` -> add comment
- `POST /comments/upload` -> multipart upload (comment + file)
- `WS /comments` -> realtime comment push

### Categories (`/categories/*`)

- `GET /categories` -> all categories
- `GET /categories/{id}` -> category by id

### Notifications (`/notifications/*`)

- `GET /notifications/user/{userId}` -> user notifications
- `GET /notifications/user/{userId}/unread-count` -> unread count
- `GET /notifications/{notifId}/user/{userId}` -> one notification
- `POST /notifications/user/{userId}` -> create notification
- `PUT /notifications/{notifId}/user/{userId}/read` -> mark read
- `WS /ws/notifications` -> realtime notification signal

## Database Schema (Expected By DAO Layer)

> The following schema matches the SQL used in `dao/*` classes.

```sql
CREATE DATABASE IF NOT EXISTS db_tasks;
USE db_tasks;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  verification_code VARCHAR(32),
  verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  color_code VARCHAR(20)
);

CREATE TABLE task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  priority VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL,
  due_date DATETIME,
  end_date DATETIME,
  created_at DATETIME NOT NULL,
  user_id BIGINT NOT NULL,
  category_id BIGINT,
  google_event_id VARCHAR(255),
  recurrence VARCHAR(30),
  parent_task_id BIGINT,
  next_run DATETIME,
  CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_task_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES task(id)
);

CREATE TABLE task_shared (
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (task_id, user_id),
  CONSTRAINT fk_shared_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_shared_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE task_dependencies (
  task_id BIGINT NOT NULL,
  depends_on_id BIGINT NOT NULL,
  PRIMARY KEY (task_id, depends_on_id),
  CONSTRAINT fk_dep_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_dep_on FOREIGN KEY (depends_on_id) REFERENCES task(id) ON DELETE CASCADE
);

CREATE TABLE sub_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  task_id BIGINT NOT NULL,
  CONSTRAINT fk_subtask_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
);

CREATE TABLE comments (
  comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT,
  CONSTRAINT fk_comment_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE attachments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  filepath VARCHAR(500) NOT NULL,
  CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Optional category seed (backend fallback uses category id `5`):

```sql
INSERT INTO category (id, name, color_code) VALUES
(1, 'Work', '#3B82F6'),
(2, 'Meetings', '#8B5CF6'),
(3, 'Fitness', '#10B981'),
(4, 'Study', '#F59E0B'),
(5, 'General', '#6B7280')
ON DUPLICATE KEY UPDATE name = VALUES(name), color_code = VALUES(color_code);
```

## Configuration

### Backend

1. Database connection
- File: `src/main/java/util/DBConnection.java`
- Defaults:
  - URL: `jdbc:mysql://localhost:3306/db_tasks?useSSL=false&serverTimezone=UTC`
  - USER: `root`
  - PASSWORD: empty

2. Mail verification credentials
- File: `src/main/java/mail/MailSender.java`
- Required env vars:
  - `MAIL_USER`
  - `MAIL_PASS`

3. Upload directory
- Attachments saved under: `%USERPROFILE%/smarttask_uploads`

### Frontend

1. Backend base URL
- File: `frontend-fro/src/main/resources/application.properties`
- Default: `backend.base-url=http://localhost:8080/`

2. Google Calendar OAuth credentials
- Place `credentials.json` in:
  - `frontend-fro/src/main/resources/credentials.json`

3. AI service URLs
- File: `frontend-fro/.../aiClient/AIClient.java`
- Endpoints expected:
  - `POST http://localhost:9090/ai/parse`
  - `POST http://localhost:9090/ai/category`
  - `POST http://localhost:9090/ai/insights`

## How To Run

### 1. Start MySQL and create schema
Run the SQL script above (or equivalent migration) in MySQL.

### 2. Start backend (Servlet WAR)

This backend module is packaged as `war` and is intended for a Servlet container (Tomcat 10+).

```powershell
# from project root (requires Maven installed for backend module)
mvn clean package
```

Deploy generated WAR from `target/` to Tomcat `webapps`.

Important:
- Frontend default base URL is root (`http://localhost:8080/`).
- If backend is deployed under a context path, update frontend `backend.base-url` accordingly.

### 3. Start frontend (JavaFX)

```powershell
cd frontend-fro
.\mvnw.cmd javafx:run
```

### 4. Optional services

- Start AI service on port `9090` for AI features.
- Configure Gmail env vars for email verification.
- Add Google `credentials.json` for calendar sync.

## Important Notes

- Passwords are currently handled in plain text in DAO/service flow; production usage should add hashing (e.g., BCrypt).
- Backend and frontend communicate without JWT/session tokens; this is a desktop/session model, not hardened API auth.
- Notification WebSocket currently broadcasts a simple signal message; clients refresh notifications through REST.

## Troubleshooting

- `ConnectException` to `localhost:8080`: backend is not running or wrong `backend.base-url`.
- JavaFX runtime errors: run via `frontend-fro\mvnw.cmd javafx:run` (not plain `java` without module config).
- Google sync fails: missing/invalid `credentials.json`.
- Registration email fails: missing `MAIL_USER` / `MAIL_PASS`.
- Comments upload fails: check write permissions to `%USERPROFILE%/smarttask_uploads`.

---

SmartTask-F combines task planning, collaboration, scheduling, and optional AI into a single Java desktop + servlet stack.
