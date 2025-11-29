package com.quantlabs.stockApp.data;


public class SimpleConsoleLogger implements ConsoleLogger {
    @Override
    public void log(String message) {
        System.out.println("[INDICATOR-MGR] " + message);
    }
}