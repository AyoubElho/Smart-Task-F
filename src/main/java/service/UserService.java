package service;

import dao.UserDao;
import mail.CodeGenerator;
import model.User;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

public class UserService {

    private final UserDao userDao = new UserDao();

    public List<User> findAllUsers() {
        return userDao.findAll();
    }

    public User findById(Long id) {
        return userDao.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public User register(User user) {

        user.setCreatedAt(LocalDateTime.now());

        String code = CodeGenerator.generate();
        user.setVerificationCode(code);
        user.setVerified(false);

        // 1️⃣ Save first
        userDao.save(user);

        // 2️⃣ Send email safely
        try {
            mail.MailSender.send(user.getEmail(), code);
        } catch (Exception e) {
            System.out.println("⚠ Email failed but user created");
            e.printStackTrace();
        }

        user.setPassword(null);
        return user;
    }




    public User login(String email, String password) {

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid email or password");
        }

        // 🔥 ADD THIS CHECK
        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified");
        }

        user.setPassword(null);
        return user;
    }
    // Add this method to UserService.java
    public User updateUser(User updatedData) {

        // 1. Check if user exists
        User existingUser = userDao.findById(updatedData.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Update fields only if they are provided (not null/empty)
        if (updatedData.getUsername() != null && !updatedData.getUsername().isEmpty()) {
            existingUser.setUsername(updatedData.getUsername());
        }

        if (updatedData.getEmail() != null && !updatedData.getEmail().isEmpty()) {
            existingUser.setEmail(updatedData.getEmail());
        }

        // Only update password if a new one is sent
        if (updatedData.getPassword() != null && !updatedData.getPassword().isEmpty()) {
            existingUser.setPassword(updatedData.getPassword());
        }

        // 3. Save changes to Database
        userDao.update(existingUser);

        // 4. clear password before returning
        existingUser.setPassword(null);

        return existingUser;
    }
    public void verifyEmail(String email, String code) {

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            throw new RuntimeException("Already verified");
        }

        if (!code.equals(user.getVerificationCode())) {
            throw new RuntimeException("Invalid verification code");
        }

        user.setVerified(true);
        user.setVerificationCode(null);

        userDao.updateVerification(user); // ✅ UPDATE
    }



}
