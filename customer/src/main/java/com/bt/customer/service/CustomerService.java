package com.bt.customer.service;

import com.bt.customer.dto.UpdateProfileRequest;
import com.bt.customer.dto.UserProfileResponse;
import com.bt.customer.entity.User;
import com.bt.customer.exception.UserNotFoundException;
import com.bt.customer.repository.UserRepository;
import com.bt.customer.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private UserRepository userRepository;

    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return UserProfileResponse.fromUser(user);
    }

    public List<UserProfileResponse> getAllCustomers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!user.getEmail().equals(request.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);
        return UserProfileResponse.fromUser(updatedUser);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }

    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findByUsername(userPrincipal.getUsername())
                .map(user -> user.getRole().name())
                .orElse("UNKNOWN");
    }

    public UserProfileResponse getCustomerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer with ID " + id + " not found"));
        return UserProfileResponse.fromUser(user);
    }
}
