package com.bt.customer.service;

import com.bt.customer.dto.*;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.exception.UserAlreadyExistsException;
import com.bt.customer.repository.UserRepository;
import com.bt.customer.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(user);

        String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());

            return new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getRole().name(),
                    "Authentication successful");
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
}
