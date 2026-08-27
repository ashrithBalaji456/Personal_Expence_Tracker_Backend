package com.personalexpense.tracker.service;

import com.personalexpense.tracker.dto.AiChatResponse;
import java.util.List;

public interface GeminiService {
    AiChatResponse chatWithAdvisor(String userMessage);
    List<String> getPredictiveAlerts();
}
