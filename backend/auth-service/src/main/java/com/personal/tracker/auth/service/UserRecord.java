package com.personal.tracker.auth.service;

import java.util.List;

public record UserRecord(String username, String passwordHash, List<String> roles) {
}
