package service;

import dao.UserDao;
import model.User;

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
        userDao.save(user);
        user.setPassword(null); // hide password in response
        return user;
    }

    public User login(String email, String password) {

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid email or password");
        }

        user.setPassword(null); // hide password
        return user;
    }
}
