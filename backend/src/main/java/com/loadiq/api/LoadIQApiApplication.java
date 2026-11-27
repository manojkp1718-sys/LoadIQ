
package com.loadiq.api;

import com.loadiq.config.DatabaseConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.loadiq")
public class LoadIQApiApplication {

    public static void main(String[] args) {

        System.out.println("Starting LoadIQ REST API...");

        DatabaseConnection.initializeDatabase();

        SpringApplication.run(LoadIQApiApplication.class, args);
    }
}
