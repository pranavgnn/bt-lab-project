package com.bt.customer.controller;

import com.bt.customer.dto.StatusResponse;
import com.bt.customer.dto.UpdateProfileRequest;
import com.bt.customer.dto.UserProfileResponse;
import com.bt.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Customer", description = "Secured customer management endpoints")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns authenticated user's profile details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getProfile() {
        UserProfileResponse profile = customerService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")
    @Operation(summary = "Get all customers", description = "Returns list of all registered customers. Accessible only by ADMIN and BANKOFFICER roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<List<UserProfileResponse>> getAllCustomers() {
        List<UserProfileResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/update")
    @Operation(summary = "Update user profile", description = "Updates authenticated user's profile information. Reserved for dashboard integrations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updatedProfile = customerService.updateProfile(request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/status")
    @Operation(summary = "Get service status", description = "Returns authenticated user role and service connectivity health check")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service status retrieved successfully", content = @Content(schema = @Schema(implementation = StatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    public ResponseEntity<StatusResponse> getStatus() {
        String role = customerService.getCurrentUserRole();
        String username = customerService.getCurrentUser().getUsername();

        StatusResponse status = StatusResponse.builder()
                .status("OPERATIONAL")
                .role(role)
                .username(username)
                .timestamp(LocalDateTime.now())
                .message("Customer service is running")
                .build();

        return ResponseEntity.ok(status);
    }
}
