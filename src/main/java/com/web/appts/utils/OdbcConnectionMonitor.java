package com.web.appts.utils;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class OdbcConnectionMonitor {

    private final List<Connection> activeConnections = new ArrayList<>();

    public void registerConnection(Connection connection) {
        synchronized (activeConnections) {
            activeConnections.add(connection);
        }
    }

    private void cleanConnections() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        System.out.println("Used memory: " + usedMemory / 1024 / 1024 + " MB");
        System.out.println("Max memory: " + maxMemory / 1024 / 1024 + " MB");
        System.out.println("Free memory: " + runtime.freeMemory() / 1024 / 1024 + " MB");

        synchronized (activeConnections) {
            activeConnections.removeIf(connection -> {
                try {
                    return connection.isClosed();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return true;
                }
            });
        }
    }

    @Scheduled(fixedRate = 1800000)
    public void monitorConnections() {
        System.out.println("Monitoring ODBC connections...");
        cleanConnections();

        System.out.println("Active connections: " + activeConnections.size());
        activeConnections.forEach(connection -> {
            try {
                if (connection.isClosed()) {
                    System.out.println("Closed connection found. Cleaning up.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
