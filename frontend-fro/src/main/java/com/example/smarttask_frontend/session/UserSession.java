package com.example.smarttask_frontend.session;

import com.example.smarttask_frontend.entity.User;

public class UserSession {

    private static User currentUser;

    private UserSession() {} // prevent instantiation

    public static void setUser(User user) {
        currentUser = user;
    }

    public static User getUser() {
        return currentUser;
    }

    public static Long getUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public static void clear() {
        currentUser = null;
    }
}
