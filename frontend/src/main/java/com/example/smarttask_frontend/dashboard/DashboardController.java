package com.example.smarttask_frontend.dashboard;
package com.example.smarttask_frontend.dashboard;
import com.example.smarttask_frontend.aiClient.AIClient;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.entity.User;
import com.example.smarttask_frontend.tasks.service.TaskService;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.entity.Notification;
import com.example.smarttask_frontend.tasks.service.NotificationService;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DashboardController implements Initializable {

    @FXML
    private Label dashboardLabel;
    @FXML
    private Label totalTasksLabel;
    @FXML
    private Label inProgressLabel;
    @FXML
    private Label completedLabel;
    private final java.util.Random random = new java.util.Random();

    // --- RECENT TASKS TABLE ---
    @FXML
    private TableView<Task> taskTable;
    @FXML
    private TableColumn<Task, String> titleColumn;
    @FXML
    private TableColumn<Task, String> statusColumn;
    @FXML
    private TableColumn<Task, String> priorityColumn;
    @FXML
    private TableColumn<Task, String> categoryColumn;
    @FXML
    private TableColumn<Task, String> dueDateColumn;

    // --- SHARED TASKS TABLE ---
    @FXML
    private TableView<Task> sharedTasksTable;
    @FXML
    private TableColumn<Task, String> sharedTitleColumn;
    @FXML
    private TableColumn<Task, String> sharedPriorityColumn;
    @FXML
    private TableColumn<Task, String> sharedCategoryColumn;
    @FXML
    private TableColumn<Task, String> sharedDueDateColumn;
    @FXML
    private TableColumn<Task, String> sharedStatusColumn;
    // === Analytics labels ===
    @FXML
    private Label timeSpentLabel;
    @FXML
    private Label avgTimeLabel;
    @FXML
    private LineChart<String, Number> trendChart;
    @FXML
    private Label aiInsightLabel;
    


    @FXML
    private Button aiButton;
    @FXML
    private Label navUserNameLabel;
    @FXML
    private Label navUserAvatarLabel;
    // Notification elements
    @FXML
    private StackPane notificationContainer;
    @FXML
    private Button notificationButton;
    @FXML
    private Label notificationBadge;
    @FXML
    private VBox notificationPanel;
    @FXML
    private StackPane mainContentPane;
    @FXML
    private ListView<Notification> notificationListView;

    private final TaskService taskService = new TaskService();
    private final NotificationService notificationService = new NotificationService();
    private ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private String fullAdvice = "";
    private List<String> adviceList = new ArrayList<>();
    private int adviceIndex = 0;
    private static final String[] AVATAR_COLORS = {
            "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb",
            "#64b5f6", "#4dd0e1", "#4db6ac", "#81c784", "#aed581",
            "#ffb74d", "#ff8a65", "#a1887f", "#90a4ae"
    };
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        loadDashboardData();
        setupNotificationPanel();

        // FIX 1: Set CellFactory ONLY ONCE here, not in loadNotifications
        setupNotificationListView();

        // Initial load
        loadNotifications();
        loadUserProfile();
        notificationService.setNotificationListener(notification -> {
            // FIX 2: WRAP IN PLATFORM.RUNLATER (Critical for WebSockets)
            Platform.runLater(() -> {
                System.out.println("WebSocket signal received. Refreshing list...");
                loadNotifications();
            });
        });
        

        notificationService.connectWebSocket();
    }
    

    

    private void setupTableColumns() {
        // --- RECENT TASKS ---
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"));

        // --- SHARED TASKS ---
        sharedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        sharedPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        sharedDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        sharedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        sharedCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategoryName() != null ? cell.getValue().getCategoryName() : "General"));

        setupStatusCellFactory(statusColumn);
        setupStatusCellFactory(sharedStatusColumn);
    }
    private String getColorForUser(String username) {
        if (username == null || username.isEmpty()) return "#4285f4";
        int index = Math.abs(username.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }
    private void loadUserProfile() {
        User user = UserSession.getUser();
        if (user != null) {
            // Set Username
            navUserNameLabel.setText(user.getUsername());

            // Set Avatar Initial
            String initial = "";
            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                initial = user.getUsername().substring(0, 1).toUpperCase();
            }
            navUserAvatarLabel.setText(initial);

            // Set Avatar Color
            String color = getColorForUser(user.getUsername());
            navUserAvatarLabel.setStyle("-fx-background-color: " + color + ";");
        }
    }
    private void setupStatusCellFactory(TableColumn<Task, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("status-badge");
                    switch (status.toUpperCase()) {
                        case "COMPLETED":
                        case "DONE":
                            badge.getStyleClass().add("status-done");
                            break;
                        case "IN_PROGRESS":
                        case "DOING":
                            badge.getStyleClass().add("status-progress");
                            break;
                        default:
                            badge.getStyleClass().add("status-todo");
                            break;
                    }
                    HBox box = new HBox(badge);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
    }

    private void setupNotificationPanel() {
        // Prepare animation start position
        notificationPanel.setTranslateX(notificationPanel.getPrefWidth());

        notificationButton.setOnAction(event -> toggleNotificationPanel());

        // FIX 3: Consume clicks on the panel so they don't click the dashboard behind it
        notificationPanel.setOnMouseClicked(MouseEvent::consume);
    }

    // FIX 4: Clean Cell Factory Logic
    private void setupNotificationListView() {
        notificationListView.setCellFactory(lv -> new ListCell<Notification>() {
            private final VBox container = new VBox(5);
            private final HBox titleBox = new HBox(10);
            private final Label titleLabel = new Label();
            private final Label timeLabel = new Label();
            private final HBox actionBox = new HBox();
            private final Button markAsReadBtn = new Button("Mark as read");
            private final StackPane unreadIndicator = new StackPane();

            {
                // Initialize Styles once
                container.setStyle("-fx-padding: 10;");
                titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
                timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                markAsReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366f1; -fx-font-size: 12px; -fx-padding: 2 5; -fx-cursor: hand;");
                markAsReadBtn.setMouseTransparent(false); // Ensure clickable

                unreadIndicator.setStyle("-fx-background-color: #6366f1; -fx-min-width: 8; -fx-min-height: 8; -fx-max-width: 8; -fx-max-height: 8; -fx-background-radius: 4;");

                // Static structure for title box
                titleBox.setAlignment(Pos.CENTER_LEFT);
                titleBox.getChildren().addAll(unreadIndicator, titleLabel);

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                actionBox.getChildren().add(markAsReadBtn);
            }

            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);

                if (empty || notification == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    titleLabel.setText(notification.getMessage());
                    timeLabel.setText(notification.getFormattedTime());

                    container.getChildren().clear(); // Reset container

                    if (!notification.isRead()) {
                        // UNREAD STATE
                        unreadIndicator.setVisible(true);
                        if (!titleLabel.getStyleClass().contains("unread-notification")) {
                            titleLabel.getStyleClass().add("unread-notification");
                        }

                        // Add elements including the Button
                        container.getChildren().addAll(titleBox, timeLabel, actionBox);

                        markAsReadBtn.setOnAction(e -> {
                            e.consume();
                            markNotificationAsRead(notification);
                        });
                    } else {
                        // READ STATE
                        unreadIndicator.setVisible(false);
                        titleLabel.getStyleClass().remove("unread-notification");

                        // Add elements WITHOUT the Button
                        container.getChildren().addAll(titleBox, timeLabel);
                    }
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void toggleNotificationPanel() {
        if (notificationPanel.isVisible()) {
            closeNotificationPanel();
        } else {
            openNotificationPanel();
        }
    }

    private void openNotificationPanel() {
        notificationPanel.setVisible(true);
        notificationPanel.setManaged(true);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notificationPanel);
        slideIn.setFromX(notificationPanel.getPrefWidth());
        slideIn.setToX(0);
        slideIn.play();
    }

    private void closeNotificationPanel() {
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notificationPanel);
        slideOut.setFromX(0);
        slideOut.setToX(notificationPanel.getPrefWidth());
        slideOut.setOnFinished(e -> {
            notificationPanel.setVisible(false);
            notificationPanel.setManaged(false);
        });
        slideOut.play();
    }

    private void loadNotifications() {
        try {
            User user = UserSession.getUser();
            if (user != null) {
                List<Notification> notificationList = notificationService.getNotificationsByUser(user.getId());
                // Update the existing observable list instead of replacing it (smoother)
                notifications.setAll(notificationList);
                notificationListView.setItems(notifications);
                updateNotificationBadge();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markNotificationAsRead(Notification notification) {
        User user = UserSession.getUser();
        if (user != null) {
            boolean success = notificationService.markAsRead(notification.getId(), user.getId());
            if (success) {
                notification.setRead(true);
                // Force list to redraw this specific item
                notificationListView.refresh();
                updateNotificationBadge();
            }
        }
    }

    @FXML
    private void markAllAsRead() {
        User user = UserSession.getUser();
        if (user != null && notifications != null) {
            notifications.forEach(notification -> {
                if (!notification.isRead()) {
                    boolean success = notificationService.markAsRead(notification.getId(), user.getId());
                    if (success) notification.setRead(true);
                }
            });
            notificationListView.refresh();
            updateNotificationBadge();
        }
    }

    @FXML
    private void clearAllNotifications() {
        if (notifications != null) {
            notifications.clear();
            updateNotificationBadge();
        }
    }

    @FXML
    private void viewAllNotifications() {
        closeNotificationPanel();
        System.out.println("View all notifications clicked");
    }

    private void updateNotificationBadge() {
        if (notifications != null) {
            long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
            if (unreadCount > 0) {
                notificationBadge.setText(String.valueOf(unreadCount));
                notificationBadge.setVisible(true);
                notificationBadge.setManaged(true);
            } else {
                notificationBadge.setVisible(false);
                notificationBadge.setManaged(false);
            }
        } else {
            notificationBadge.setVisible(false);
            notificationBadge.setManaged(false);
        }
    }

    private void loadDashboardData() {
        try {
            User user = UserSession.getUser();
            if (user == null) {
                dashboardLabel.setText("Session Expired");
                return;
            }
            dashboardLabel.setText("Overview");
            List<Task> tasks = taskService.getTasksByUser(user.getId());
            taskTable.setItems(FXCollections.observableArrayList(tasks));
            List<Task> sharedTasks = taskService.getSharedTasks(user.getId());
            sharedTasksTable.setItems(FXCollections.observableArrayList(sharedTasks));
            updateStatistics(tasks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
========================================
US-16 / US-17 / US-18 Analytics Engine
Random simulated productivity data
========================================
*/
    private void updateStatistics(List<Task> tasks) {

        int total = tasks.size();

        long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus()) || "DOING".equalsIgnoreCase(t.getStatus())).count();

        long completed = tasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()) || "DONE".equalsIgnoreCase(t.getStatus())).count();

        // === US-16: Simulated time tracking ===
        int totalMinutes = tasks.stream().mapToInt(t -> 10 + random.nextInt(120)) // 10–130 min per task
                .sum();

        // === US-17: Productivity metrics ===
        int avgMinutes = total == 0 ? 0 : totalMinutes / total;

        // === Update UI ===
        totalTasksLabel.setText(String.valueOf(total));
        inProgressLabel.setText(String.valueOf(inProgress));
        completedLabel.setText(String.valueOf(completed));

        timeSpentLabel.setText(totalMinutes + " min");
        avgTimeLabel.setText(avgMinutes + " min");

        // === US-18: Trend simulation ===
        simulateTrend(tasks);
    }

    private void simulateTrend(List<Task> tasks) {

        trendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Productivity");

        Random random = new Random();

        for (int day = 1; day <= 7; day++) {
            int minutes = 40 + random.nextInt(120);
            series.getData().add(new XYChart.Data<>("Day " + day, minutes));
        }

        trendChart.getData().add(series);
    }

    @FXML
    private void logout() {
        try {
            UserSession.clear();
            for (Window window : List.copyOf(Window.getWindows())) window.hide();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CalendarView.fxml"));
            Parent root = loader.load();
            Stage calendarStage = new Stage();
            calendarStage.setTitle("Task Calendar");
            calendarStage.setScene(new Scene(root));
            calendarStage.initOwner(dashboardLabel.getScene().getWindow());
            calendarStage.setResizable(true);
            calendarStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mytasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyTasks.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("My Tasks");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePanelClick(MouseEvent event) {
        event.consume();
    }

    @FXML
    private void getAIInsights() {

        // If we already have tips → show next one
        if (!adviceList.isEmpty()) {
            showNextAdvice();
            return;
        }

        aiInsightLabel.setText("Generating AI advice...");

        new Thread(() -> {
            try {
                User user = UserSession.getUser();
                String fullAdvice = AIClient.getInsights(user);

                // split into sentences or numbered tips
                adviceList = Arrays.stream(fullAdvice.split("\\n|\\."))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                adviceIndex = 0;

                Platform.runLater(this::showNextAdvice);

            } catch (Exception e) {
                Platform.runLater(() ->
                        aiInsightLabel.setText("Failed to load AI advice.")
                );
                e.printStackTrace();
            }
        }).start();
    }

    private void showFullAdvice() {
        if (fullAdvice == null || fullAdvice.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Productivity Insight");
        alert.setHeaderText("Full Advice");
        alert.setContentText(fullAdvice);
        alert.showAndWait();
    }

    @FXML
    private void showNextAdvice() {
        if (adviceList.isEmpty()) return;

        aiInsightLabel.setText(adviceList.get(adviceIndex));

        adviceIndex++;
        if (adviceIndex >= adviceList.size()) {
            adviceIndex = 0; // loop back
        }
    }


}