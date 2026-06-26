package com.example;

public class Auth {
    // This variable can be seen by any controller in your app
    public static boolean isLoggedIn = false;

    // Tracks whether the Report Tab has been unlocked via login
    public static boolean reportUnlocked = false;

    // Stores the user's role (Student or Faculty)
    public static String userRole = null;
}