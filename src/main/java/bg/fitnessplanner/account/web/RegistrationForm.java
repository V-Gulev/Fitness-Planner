package bg.fitnessplanner.account.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RegistrationForm {
    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be at most 254 characters.")
    private String email;

    @NotBlank(message = "Enter a password.")
    @Size(min = 10, max = 64, message = "Use a password between 10 and 64 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    @AssertTrue(message = "The passwords must match.")
    public boolean isPasswordConfirmationMatching() {
        return password == null || confirmPassword == null || Objects.equals(password, confirmPassword);
    }

    @AssertTrue(message = "That password is too long. Try fewer characters.")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.strip(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
