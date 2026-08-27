package com.personalexpense.tracker.service.impl;

import com.personalexpense.tracker.dto.AuthResponse;
import com.personalexpense.tracker.dto.LoginRequest;
import com.personalexpense.tracker.dto.RegisterRequest;
import com.personalexpense.tracker.entity.User;
import com.personalexpense.tracker.exception.DuplicateResourceException;
import com.personalexpense.tracker.repository.UserRepository;
import com.personalexpense.tracker.security.JwtTokenProvider;
import com.personalexpense.tracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.personalexpense.tracker.repository.BudgetCategoryRepository;
import com.personalexpense.tracker.entity.BudgetCategory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final BudgetCategoryRepository budgetCategoryRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered.");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        seedDefaultCategories(user);

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }

    private void seedDefaultCategories(User user) {
        List<BudgetCategory> defaults = Arrays.asList(
            BudgetCategory.builder().user(user).name("Rent").percentage(BigDecimal.valueOf(22)).color("#EC4899").icon("Home").build(),
            BudgetCategory.builder().user(user).name("Groceries").percentage(BigDecimal.valueOf(13)).color("#10B981").icon("ShoppingCart").build(),
            BudgetCategory.builder().user(user).name("Electricity + Wi-Fi").percentage(BigDecimal.valueOf(3)).color("#F59E0B").icon("Lightbulb").build(),
            BudgetCategory.builder().user(user).name("Term Insurance").percentage(BigDecimal.valueOf(2)).color("#EF4444").icon("Shield").build(),
            BudgetCategory.builder().user(user).name("SIP Investment").percentage(BigDecimal.valueOf(18)).color("#8B5CF6").icon("TrendingUp").build(),
            BudgetCategory.builder().user(user).name("Gold Saving").percentage(BigDecimal.valueOf(4)).color("#EAB308").icon("Coins").build(),
            BudgetCategory.builder().user(user).name("Parents Support").percentage(BigDecimal.valueOf(17)).color("#6366F1").icon("Heart").build(),
            BudgetCategory.builder().user(user).name("FD/Emergency").percentage(BigDecimal.valueOf(4)).color("#14B8A6").icon("PiggyBank").build(),
            BudgetCategory.builder().user(user).name("Travel & Commute").percentage(BigDecimal.valueOf(7)).color("#3B82F6").icon("Train").build(),
            BudgetCategory.builder().user(user).name("Other Expenses").percentage(BigDecimal.valueOf(10)).color("#64748B").icon("Wallet").build()
        );
        budgetCategoryRepository.saveAll(defaults);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsername().trim();

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username/email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username/email or password.");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }
}
