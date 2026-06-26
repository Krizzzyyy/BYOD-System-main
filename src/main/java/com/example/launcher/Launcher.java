package com.example.launcher;

import com.example.login.LoginApp;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Launches LoginApp, which now opens the dashboard immediately!
        Application.launch(LoginApp.class, args);
    }
}
