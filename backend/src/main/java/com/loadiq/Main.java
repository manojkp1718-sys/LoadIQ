package com.loadiq;

import com.loadiq.config.DatabaseConnection;
import com.loadiq.console.LoadIQMenu;

public class Main {

    public static void main(String[] args) {

        System.out.println("Starting LoadIQ...");

        DatabaseConnection.initializeDatabase();

        System.out.println("Database ready!");

        LoadIQMenu menu = new LoadIQMenu();
        menu.start();
    }
}
