package com.changeimpact.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLogger {
    private final List<String> logs = new ArrayList<>();

    public synchronized void log(String event) {
        logs.add(System.currentTimeMillis() + ": " + event);
    }

    public synchronized List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
