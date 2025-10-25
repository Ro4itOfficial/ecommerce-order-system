package com.ecommerce.order.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User registration request")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    @Schema(description = "Unique username", example = "testuser")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, one special character, and no whitespace"
    )
    @Schema(description = "Password (min 8 chars, must include uppercase, lowercase, number, and special character)", 
            example = "Password123!")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Password confirmation", example = "Password123!")
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    @Schema(description = "First name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Phone number (E.164 format)", example = "+1234567890")
    private String phoneNumber;

    @AssertTrue(message = "Terms and conditions must be accepted")
    @Schema(description = "Terms and conditions acceptance", example = "true")
    @Builder.Default
    private boolean termsAccepted = false;

    @Schema(description = "Marketing emails opt-in", example = "false", defaultValue = "false")
    @Builder.Default
    private boolean marketingOptIn = false;

    @AssertTrue(message = "Passwords do not match")
    @Schema(hidden = true)
    public boolean isPasswordsMatching() {
        return password != null && password.equals(confirmPassword);
    }
}