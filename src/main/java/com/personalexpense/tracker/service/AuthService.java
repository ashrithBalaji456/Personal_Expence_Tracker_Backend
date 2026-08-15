package com.personalexpense.tracker.service;

import com.personalexpense.tracker.dto.AuthResponse;
import com.personalexpense.tracker.dto.LoginRequest;
import com.personalexpense.tracker.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
