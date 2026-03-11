package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.aiClient.AIClient;
import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.comments.CommentCell;
import com.example.smarttask_frontend.comments.CommentService;
import com.example.smarttask_frontend.comments.LiveCommentClient;
import com.example.smarttask_frontend.entity.*;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.subtasks.controller.SubtaskController;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


public class MyTasksController implements Initializable {
    @FXML
    private Label totalTaskLabel; // <--- ADD THIS
    @FXML
    private Label taskTitleLabel;
    @FXML
    private Label sharedTotalLabel; // <--- 1. INJECT THIS
    @FXML
    private Tab myTasksTab;
    @FXML
    private Tab sharedTasksTab;
    // --- Shared Table Injectables ---
    @FXML
    private TableView<Task> sharedTasksTable;
    @FXML
    private TableColumn<Task, String> sharedTitleColumn;
    @FXML
    private TableColumn<Task, String> sharedPriorityColumn;
    @FXML
    private TableColumn<Task, String> sharedCategoryColumn; // NEW
    @FXML
    private TableColumn<Task, String> sharedStatusColumn;
    @FXML
    private TableColumn<Task, Void> sharedSubTasksColumn; // NEW
    @FXML
    private TableColumn<Task, String> dependenciesColumn;
    @FXML
    private TableColumn<Task, String> recurrenceColumn;
    @FXML
    private TableColumn<Task, String> sharedRecurrenceColumn;
    @FXML
    private TableColumn<Task, String> sharedDependenciesColumn;
    private final ObservableList<String> recurrenceOptions = FXCollections.observableArrayList("NONE", "DAILY", "WEEKLY", "MONTHLY");
    // --- Main Table Injectables ---
    @FXML
    private TableView<Task> taskTable;
    @FXML
    private TableColumn<Task, String> titleColumn;
    @FXML
    private TableColumn<Task, String> priorityColumn;
    @FXML
    private TableColumn<Task, String> categoryColumn;
    @FXML
    private TableColumn<Task, String> statusColumn;
    @FXML
    private TableColumn<Task, Void> subTasksColumn;
    @FXML
    private TableColumn<Task, Void> shareColumn;
    @FXML
    private VBox commentPanel;
    private boolean commentsOpen = false;
    // --- Other UI ---
    @FXML
    private ListView<Comment> commentList;
    @FXML
    private TextField commentField;
    @FXML
    private TableColumn<Task, String> startDateColumn;
    @FXML
    private TableColumn<Task, String> endDateColumn;
    @FXML
    private TableColumn<Task, String> sharedByColumn;
    @FXML
    private TableColumn<Task, String> sharedStartDateColumn;
    @FXML
    private TableColumn<Task, String> sharedEndDateColumn;
    @FXML
    private TableColumn<Task, Void> actionColumn;
    // --- Services & State ---
    private static final double PANEL_WIDTH = 300;
    private LiveCommentClient ws;
    private Task selectedTask;
    private final TaskService taskService = new TaskService();
    private final CommentService commentService = new CommentService();
    private final UserService userService = new UserService();
    private Map<Long, String> userMap = new HashMap<>();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList("TODO", "IN_PROGRESS", "DONE");
    private static final String[] AVATAR_COLORS = {
            "#3182CE", // Blue
            "#805AD5", // Purple
            "#DD6B20", // Orange
            "#38A169", // Green
            "#D53F8C", // Pink
            "#00B5D8", // Cyan
            "#E53E3E", // Red
            "#5B6B79"  // Muted Blue/Gray
    };
    // 2. Helper method to pick a consistent color based on the name
    private String getColorForUser(String username) {
        if (username == null || username.isEmpty()) return AVATAR_COLORS[7]; // Default gray
        // Use hashCode() so the same name always gets the same color index
        int index = Math.abs(username.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }
    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");

            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);

                // Add specific classes here:
                editBtn.getStyleClass().addAll("icon-btn", "edit-btn");     // <--- Added edit-btn
                deleteBtn.getStyleClass().addAll("icon-btn", "delete-btn"); // <--- Added delete-btn

                editBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    openEditTask(task);
                });

                deleteBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    deleteTask(task);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void deleteTask(Task task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Task: " + task.getTitle());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    taskService.deleteTask(task.getId());
                    refreshTasks();
                } catch (Exception e) {
                    showError("Delete failed");
                }
            }
        });
    }

    private void openEditTask(Task task) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UpdateTask.fxml"));
            Parent root = loader.load();

            UpdateTaskController controller = loader.getController();
            controller.loadTaskForEdit(task);

            Stage stage = new Stage();
            stage.setTitle("Edit Task");
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHidden(e -> {
                if (controller.isUpdated()) {
                    refreshTasks();
                }
            });
        } catch (IOException e) {
            showError("Cannot open edit window");
        }
    }

    private Callback<TableColumn<Task, String>, TableCell<Task, String>> createRecurrenceCellFactory() {
        return col -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>(recurrenceOptions);

            {
                // 1. Add a specific Style Class to this CELL (not just the column)
                getStyleClass().add("recurrence-cell");

                comboBox.getStyleClass().add("recurrence-combo");
                comboBox.setMaxWidth(Double.MAX_VALUE);

                comboBox.setOnAction(e -> {
                    if (getTableView() != null && getIndex() < getTableView().getItems().size()) {
                        Task task = getTableView().getItems().get(getIndex());
                        String selectedString = comboBox.getValue();
                        if (selectedString == null) return;

                        updateStyle(selectedString);

                        try {
                            Recurrence r = Recurrence.valueOf(selectedString);
                            task.setRecurrence(r);

                            // UNCOMMENT TO SAVE TO DB
                            taskService.updateTaskRecurrence(task.getId(), r.name());

                        } catch (Exception ex) {
                            showError("Failed to update recurrence");
                            ex.printStackTrace();
                        }
                    }
                });
            }


            private void updateStyle(String rName) {
                comboBox.getStyleClass().removeAll("recurrence-none", "recurrence-daily", "recurrence-weekly", "recurrence-monthly");

                if (rName == null) return;

                switch (rName.toUpperCase()) {
                    case "NONE" -> comboBox.getStyleClass().add("recurrence-none");
                    case "DAILY" -> comboBox.getStyleClass().add("recurrence-daily");
                    case "WEEKLY" -> comboBox.getStyleClass().add("recurrence-weekly");
                    case "MONTHLY" -> comboBox.getStyleClass().add("recurrence-monthly");
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    String val = (item == null || item.isEmpty()) ? "NONE" : item;

                    // Prevent listener firing during update
                    var handler = comboBox.getOnAction();
                    comboBox.setOnAction(null);

                    comboBox.setValue(val);
                    updateStyle(val);

                    comboBox.setOnAction(handler);
                    setGraphic(comboBox);
                }
            }
        };
    }

    private void updateTabHeader(Tab tab, String title, int count) {
        // Create the Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Create the Badge (using your existing CSS class)
        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("count-badge");

        // Put them in a box with spacing
        HBox header = new HBox(8, titleLabel, countLabel);
        header.setAlignment(Pos.CENTER);

        // Set this box as the Tab's graphic
        tab.setGraphic(header);
        tab.setText(""); // Clear standard text so it doesn't duplicate
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupActionColumn();
        Label emptyLabel = new Label("No comments yet");
        emptyLabel.getStyleClass().add("placeholder-label"); // CSS class for styling

        // 2. Attach it to the ListView
        commentList.setPlaceholder(emptyLabel);
        setupMainTableColumns();
        setupSharedTableColumns(); // Updated method

        loadTasks();
        loadSharedTasks();
        setupDependencyColumn(dependenciesColumn, taskTable);

        // Note: For shared tasks, we might only know the ID, 
        // if the dependent task isn't in the list, we show the ID.
        setupDependencyColumn(sharedDependenciesColumn, sharedTasksTable);

        // Load Users for Comments
        List<User> users = userService.getUsers();
        for (User u : users) {
            userMap.put(u.getId(), u.getUsername());
        }
        commentList.setCellFactory(list -> new CommentCell(userMap));

        setupSelectionListeners();
        setupWebSocket();
    }


    private void setupDependencyColumn(TableColumn<Task, String> column, TableView<Task> sourceTable) {
        column.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            List<Long> depIds = task.getDependencyIds();

            if (depIds == null || depIds.isEmpty()) {
                return new SimpleStringProperty("None"); // or ""
            }

            // Convert List<Long> IDs to String Titles
            String names = depIds.stream().map(id -> getTaskTitleById(id, sourceTable)).collect(Collectors.joining(", "));

            return new SimpleStringProperty(names);
        });
    }

    private String getTaskTitleById(Long id, TableView<Task> table) {
        if (table.getItems() == null) return "ID: " + id;

        // Search in the current table items
        return table.getItems().stream().filter(t -> t.getId().equals(id)).map(Task::getTitle).findFirst().orElse("ID: " + id); // Fallback if task isn't loaded
    }

    // ========================= MAIN TABLE SETUP =========================
    private void setupMainTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        recurrenceColumn.setCellValueFactory(cell -> {
            Task t = cell.getValue();
            // Safely convert Enum to String for the column
            String val = (t.getRecurrence() != null) ? t.getRecurrence().name() : "NONE";
            return new SimpleStringProperty(val);
        });

        recurrenceColumn.setCellFactory(createRecurrenceCellFactory());
        // Category Logic
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"));

        // Use Helper for Status Badge
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        statusColumn.setCellFactory(createStatusCellFactory());

        // Use Helper for Subtask Button
        subTasksColumn.setCellFactory(createSubtaskButtonFactory());

        setupShareButtonColumn(); // Only main tasks are shareable usually
    }

    // ========================= SHARED TABLE SETUP =========================
    private void setupSharedTableColumns() {
        sharedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        sharedPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        sharedStartDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        sharedEndDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        sharedRecurrenceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRecurrence() != null ? cell.getValue().getRecurrence().toString() : "NONE"));
        sharedByColumn.setCellValueFactory(cell -> {
            Long ownerId = cell.getValue().getUserId(); // ✅ correct
            String username = userMap.getOrDefault(ownerId, "Unknown");
            return new SimpleStringProperty(username);
        });

        sharedByColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);

                if (empty || username == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();

                    // A. Create Avatar Circle
                    Label avatar = new Label(initial);
                    avatar.getStyleClass().add("user-avatar"); // Applies shape/size from CSS

                    // B. NEW: Get consistent color and apply inline style
                    String colorHex = getColorForUser(username);
                    // We override the background color and make text white for contrast
                    avatar.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white;");

                    // C. Create Name Label
                    Label name = new Label(username);
                    name.setStyle("-fx-text-fill: #2D3748;");

                    // D. Layout
                    HBox box = new HBox(8, avatar, name);
                    box.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(box);
                    setText(null);
                }
            }
        });
        sharedRecurrenceColumn.setCellFactory(createRecurrenceCellFactory());
        // 1. Category (Same logic as main)
        sharedCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"));

        // 2. Status (Now uses the same Fancy Badge Style!)
        sharedStatusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        sharedStatusColumn.setCellFactory(createStatusCellFactory());

        // 3. Subtasks (Now uses the same Button logic!)
        sharedSubTasksColumn.setCellFactory(createSubtaskButtonFactory());
    }

    // ========================= HELPER FACTORIES (Reuse logic) =========================

    /**
     * Creates the "View" button for subtasks.
     * Reused by both tables.
     */
    /**
     * Creates the "View Subtasks" icon button.
     */
    private Callback<TableColumn<Task, Void>, TableCell<Task, Void>> createSubtaskButtonFactory() {
        return col -> new TableCell<>() {
            // Use a List/Clipboard icon for Subtasks
            private final Button button = new Button("📋");

            {
                button.getStyleClass().add("icon-btn"); // Removes background, looks like icon
                button.setTooltip(new Tooltip("View Subtasks")); // Hover text

                button.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    openSubtasksWindow(task.getId(), task.getTitle());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        };
    }

    /**
     * Creates the Colored Badge Status Dropdown.
     * Reused by both tables.
     */

    private List<String> getUnfinishedDependencies(Task task) {
        List<Long> depIds = task.getDependencyIds();
        if (depIds == null || depIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Combine all loaded tasks to search through them
        java.util.List<Task> allLoadedTasks = new java.util.ArrayList<>();
        if (taskTable.getItems() != null) allLoadedTasks.addAll(taskTable.getItems());
        if (sharedTasksTable.getItems() != null) allLoadedTasks.addAll(sharedTasksTable.getItems());

        java.util.List<String> unfinishedNames = new java.util.ArrayList<>();

        for (Long id : depIds) {
            allLoadedTasks.stream().filter(t -> t.getId().equals(id)).findFirst().ifPresent(dep -> {
                String s = dep.getStatus();
                // Check if NOT done (adjust "COMPLETED" based on your specific Enum/String values)
                if (!"DONE".equalsIgnoreCase(s) && !"COMPLETED".equalsIgnoreCase(s)) {
                    unfinishedNames.add(dep.getTitle());
                }
            });
        }
        return unfinishedNames;
    }

    private Callback<TableColumn<Task, String>, TableCell<Task, String>> createStatusCellFactory() {
        return col -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>(statusOptions);

            {
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.getStyleClass().add("status-combo");

                comboBox.setOnAction(e -> {
                    if (getTableView() != null && getIndex() < getTableView().getItems().size()) {
                        Task task = getTableView().getItems().get(getIndex());
                        String newStatus = comboBox.getValue();
                        String oldStatus = task.getStatus(); // Keep track of old status

                        // 🔥 VALIDATION: If marking as DONE, check dependencies
                        if ("DONE".equals(newStatus) || "COMPLETED".equals(newStatus)) {
                            List<String> unfinishedDeps = getUnfinishedDependencies(task);

                            if (!unfinishedDeps.isEmpty()) {
                                // 1. Show Alert
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("Dependencies Not Met");
                                alert.setHeaderText("Cannot complete task");
                                alert.setContentText("The following dependencies are not finished:\n- " + String.join("\n- ", unfinishedDeps));
                                alert.showAndWait();

                                // 2. Revert UI immediately (runLater to ensure UI update cycle is consistent)
                                Platform.runLater(() -> {
                                    comboBox.setValue(oldStatus);
                                    updateStyle(oldStatus);
                                });
                                return; // 🛑 STOP HERE, do not call backend
                            }
                        }

                        // If validation passes, proceed as normal
                        task.setStatus(newStatus);
                        updateStyle(newStatus);

                        System.out.println("Updating status ID: " + task.getId() + " to " + newStatus);
                        try {
                            taskService.updateTaskStatus(task.getId(), newStatus);
                        } catch (Exception ex) {
                            showError("Failed to update status.");
                            comboBox.setValue(oldStatus); // Revert on backend error
                        }
                    }
                });
            }

            private void updateStyle(String status) {
                comboBox.getStyleClass().removeAll("status-todo", "status-inprogress", "status-done");
                if (status != null) {
                    switch (status) {
                        case "TODO":
                            comboBox.getStyleClass().add("status-todo");
                            break;
                        case "IN_PROGRESS":
                            comboBox.getStyleClass().add("status-inprogress");
                            break;
                        case "DONE":
                            comboBox.getStyleClass().add("status-done");
                            break;
                    }
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // Temporarily remove listener to prevent triggering logic when scrolling
                    javafx.event.EventHandler<ActionEvent> handler = comboBox.getOnAction();
                    comboBox.setOnAction(null);

                    comboBox.setValue(item);
                    updateStyle(item);

                    comboBox.setOnAction(handler); // Restore listener
                    setGraphic(comboBox);
                }
            }
        };
    }


    // ========================= SHARE BUTTON (Main Table Only) =========================

    /**
     * Creates the "Share" icon button.
     */
    private void setupShareButtonColumn() {
        shareColumn.setCellFactory(col -> new TableCell<>() {
            // Use a Share/Export icon
            private final Button button = new Button("📤");

            {
                button.getStyleClass().add("icon-btn"); // Removes background
                button.setTooltip(new Tooltip("Share Task")); // Hover text

                button.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    openShareTaskDialog(task);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    // ========================= DATA & EVENTS =========================
    private void loadTasks() {
        try {
            Long userId = UserSession.getUserId();
            List<Task> tasks = taskService.getTasksByUser(userId);
            taskTable.setItems(FXCollections.observableArrayList(tasks));

            // UPDATE TAB HEADER
            if (myTasksTab != null) {
                updateTabHeader(myTasksTab, "My Tasks", tasks.size());
            }

            // Update top header badge (if you still want it)
            if (totalTaskLabel != null) {
                totalTaskLabel.setText(String.valueOf(tasks.size()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSharedTasks() {
        try {
            Long userId = UserSession.getUserId();
            List<Task> sharedTasks = taskService.getSharedTasks(userId);
            sharedTasksTable.setItems(FXCollections.observableArrayList(sharedTasks));

            // UPDATE TAB HEADER
            if (sharedTasksTab != null) {
                updateTabHeader(sharedTasksTab, "Shared", sharedTasks.size());
            }

            // Update top header badge
            if (sharedTotalLabel != null) {
                sharedTotalLabel.setText(String.valueOf(sharedTasks.size()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSelectionListeners() {
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                sharedTasksTable.getSelectionModel().clearSelection();
                selectedTask = n;
                taskTitleLabel.setText("Task: " + n.getTitle());
                loadComments(n.getId());
            }
        });

        sharedTasksTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                taskTable.getSelectionModel().clearSelection();
                selectedTask = n;
                taskTitleLabel.setText("Task: " + n.getTitle());
                loadComments(n.getId());
            }
        });
    }


    private void setupWebSocket() {
        ws = new LiveCommentClient(comment -> {
            if (selectedTask != null && comment.getTaskId() == selectedTask.getId()) {
                javafx.application.Platform.runLater(() -> {
                    commentList.getItems().add(comment);
                    commentList.scrollTo(commentList.getItems().size() - 1);
                });
            }
        });
        ws.connect();
    }


    private void loadComments(Long taskId) {
        System.out.println("Loading comments for task: " + taskId);

        commentList.getItems().clear();

        List<Comment> comments = commentService.getComments(taskId);

        System.out.println("Comments found: " + comments.size());

        commentList.getItems().addAll(comments);
    }


    @FXML
    public void sendComment(ActionEvent e) {
        String text = commentField.getText();
        if (text == null || text.trim().isEmpty() || selectedTask == null) return;
        Comment c = new Comment();
        c.setTaskId(selectedTask.getId());
        c.setUserId(UserSession.getUserId());
        c.setContent(text);
        commentService.addComment(c);
        commentField.clear();
    }

    private void openSubtasksWindow(Long taskId, String taskTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Subtasks.fxml"));
            Parent root = loader.load();
            SubtaskController controller = loader.getController();
            controller.setTaskId(taskId);
            Stage stage = new Stage();
            stage.setTitle("Subtasks - " + taskTitle);
            stage.setScene(new Scene(root, 800, 500));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Unable to open Subtasks");
        }
    }

    private void openShareTaskDialog(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ShareTaskView.fxml"));
            Parent root = loader.load();
            ShareTaskController controller = loader.getController();
            controller.setTaskId(task.getId(), task.getTitle());
            Stage stage = new Stage();
            stage.setTitle("Share Task");
            stage.setScene(new Scene(root, 500, 500));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Unable to open Share dialog");
        }
    }

    @FXML
    private void createTask() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CreateTaskView.fxml"));
            Parent root = loader.load();

            CreateTaskController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Create Task");
            stage.setScene(new Scene(root));

            stage.setOnHidden(e -> {
                if (controller.isCreated()) {
                    refreshTasks();  // only refresh if task created
                }
            });

            stage.show();

        } catch (IOException e) {
            showError("Could not open create task window.");
        }
    }


    @FXML
    public void aiGenerate() {
        // 1. Create a generic Dialog instead of TextInputDialog for more control
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("AI Task Generator");
        dialog.initStyle(StageStyle.UTILITY); // Removes standard OS window decoration bloat

        // 2. Setup the Dialog Pane and CSS
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/ai-dialog.css").toExternalForm());
        dialogPane.getStyleClass().add("ai-dialog");

        // Remove the default graphic/header for a minimal look
        dialog.setHeaderText(null);
        dialog.setGraphic(null);

        // 3. Create Custom Content (Label + TextArea for multi-line prompts)
        Label headerLabel = new Label("Describe your task");
        headerLabel.getStyleClass().add("header-label");

        TextArea textArea = new TextArea();
        textArea.setPromptText("E.g., Create a marketing plan for the Q3 launch...");
        textArea.setWrapText(true);
        textArea.setPrefHeight(100); // taller input for AI prompts

        VBox content = new VBox(10, headerLabel, textArea);
        content.setPadding(new Insets(20));
        dialogPane.setContent(content);

        // 4. Add Buttons
        ButtonType generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);

        // 5. Convert result to the string input
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButtonType) {
                return textArea.getText();
            }
            return null;
        });

        // 6. Focus the text area by default
        Platform.runLater(textArea::requestFocus);

        // 7. Show and Handle Logic
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(input -> {
            if (input.trim().isEmpty()) return; // Validation

            // Show a loading state (optional UI enhancement)
            // mainLayout.setCursor(Cursor.WAIT);

            new Thread(() -> {
                try {
                    User user = UserSession.getUser();
                    Task task = AIClient.parseTask(input);

                    // Optional: category suggestion
                    CategoryDTO cat = AIClient.suggestCategory(task);
                    if (cat != null) task.setCategoryName(cat.getName());

                    taskService.createTask(task, user.getId());

                    Platform.runLater(() -> {
                        refreshTasks(); // Update your list
                        showSuccess("Task created successfully!"); // Show Valid Alert
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showError("AI generation failed: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null); // No header looks cleaner
        alert.setContentText(message);
        // Apply your CSS to the alert dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/ai-dialog.css").toExternalForm());
        alert.showAndWait();
    }


    private void refreshTasks() {
        loadTasks();       // reload main table
        loadSharedTasks(); // reload shared table (optional but good)
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }

    @FXML
    public void attachFile() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file");

        // optional filters
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"), new FileChooser.ExtensionFilter("PDF", "*.pdf"), new FileChooser.ExtensionFilter("All files", "*.*"));

        File file = chooser.showOpenDialog(commentField.getScene().getWindow());

        if (file == null) return;

        uploadAttachment(file);
    }

    private void uploadAttachment(File file) {

        if (selectedTask == null) {
            showError("Select a task first");
            return;
        }

        try {
            long userId = UserSession.getUserId();
            String text = commentField.getText(); // optional message

            System.out.println("Uploading: " + file.getName());

            commentService.uploadFile(file, selectedTask.getId(), userId, text);

            commentField.clear();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Upload failed");
        }
    }


    @FXML
    private void toggleComments() {

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), commentPanel);

        if (!commentsOpen) {
            commentPanel.setVisible(true);
            tt.setFromX(PANEL_WIDTH);
            tt.setToX(0);
        } else {
            tt.setFromX(0);
            tt.setToX(PANEL_WIDTH);
            tt.setOnFinished(e -> commentPanel.setVisible(false));
        }

        tt.play();
        commentsOpen = !commentsOpen;
    }
}