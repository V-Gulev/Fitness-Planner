package bg.fitnessplanner.account.web;

import bg.fitnessplanner.account.application.EmailPolicy;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationFormTest {
    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidation() { VALIDATION.close(); }

    @Test
    void acceptsMatchingValidPasswordAndNormalizesEmailWithoutDefaultLocale() {
        var form = form("valid-password-123");
        form.setEmail("  Person@Example.TEST  ");
        assertThat(VALIDATION.getValidator().validate(form)).isEmpty();
        assertThat(EmailPolicy.normalize(form.getEmail())).isEqualTo("person@example.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "123456789"})
    void rejectsMissingOrShortPasswords(String password) {
        assertThat(VALIDATION.getValidator().validate(form(password))).isNotEmpty();
    }

    @Test
    void rejectsPasswordsThatWouldExceedBcryptUtf8ByteLimit() {
        assertThat(VALIDATION.getValidator().validate(form("界".repeat(25))))
                .anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("passwordWithinByteLimit"));
    }

    @Test
    void rejectsMismatchedConfirmation() {
        var form = form("valid-password-123");
        form.setConfirmPassword("a-different-password");
        assertThat(VALIDATION.getValidator().validate(form)).isNotEmpty();
    }

    @Test
    void rejectsInvalidEmail() {
        var form = form("valid-password-123");
        form.setEmail("not-an-email");
        assertThat(VALIDATION.getValidator().validate(form)).isNotEmpty();
    }

    private RegistrationForm form(String password) {
        var form = new RegistrationForm();
        form.setEmail("person@example.test");
        form.setPassword(password);
        form.setConfirmPassword(password);
        return form;
    }
}
