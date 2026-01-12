package ewm.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class EventDateValidator implements ConstraintValidator<ValidEventDate, LocalDateTime> {

    private long minHours;

    @Override
    public void initialize(ValidEventDate constraintAnnotation) {
        this.minHours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minimumValidDateTime = now.plusHours(minHours);

        return value.isAfter(minimumValidDateTime) || value.isEqual(minimumValidDateTime);
    }
}