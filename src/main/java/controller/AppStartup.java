package controller;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import service.RecurringScheduler;

@WebListener
public class AppStartup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        System.out.println("Starting recurring scheduler...");

        Thread scheduler =
                new Thread(new RecurringScheduler());

        scheduler.setDaemon(true);
        scheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Server shutting down...");
    }
}
