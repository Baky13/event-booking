package com.example.eventbooking.service;

import com.example.eventbooking.dto.AuthResponse;
import com.example.eventbooking.dto.LoginRequest;
import com.example.eventbooking.dto.RegisterRequest;
import com.example.eventbooking.exception.UnauthorizedException;
import com.example.eventbooking.exception.ValidationException;
import com.example.eventbooking.model.User;
import com.example.eventbooking.repository.UserRepository;
import com.example.eventbooking.security.JwtBlacklist;
import com.example.eventbooking.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Dummy hash для timing attack prevention
    private static final String DUMMY_HASH = "$2a$10$dummyhashforpreventingtimingattacksXXXXXXXXXXXXXXXXXXX";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklist jwtBlacklist;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, JwtBlacklist jwtBlacklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklist = jwtBlacklist;
    }

    public AuthResponse register(RegisterRequest request) {
        // Баг 4: не раскрываем существование email — одинаковое сообщение
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Registration failed");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getId());
        String token = jwtTokenProvider.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getName());
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        // Баг 9: timing attack fix — всегда вызываем BCrypt независимо от того найден ли пользователь
        String hashToCheck = userOpt.map(User::getPassword).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (userOpt.isEmpty() || !passwordMatches) {
            log.warn("Failed login attempt for email: {}", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userOpt.get();
        log.info("User {} logged in", user.getId());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName());
    }

    // Баг 10: logout — инвалидируем токен
    public void logout(String token) {
        jwtBlacklist.blacklist(token);
        log.info("Token blacklisted on logout");
    }
}
