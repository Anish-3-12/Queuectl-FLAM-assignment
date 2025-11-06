package com.example.queuectl.core;

public class ShutdownSignals {
    public static void addHook(Runnable r){ Runtime.getRuntime().addShutdownHook(new Thread(r)); }
}
