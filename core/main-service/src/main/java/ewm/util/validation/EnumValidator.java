package ewm.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> acceptedValues;
    private boolean ignoreCase;
    private String messageTemplate;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.ignoreCase = constraintAnnotation.ignoreCase();
        this.messageTemplate = constraintAnnotation.message();

        Set<String> allNames = Arrays.stream(constraintAnnotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());

        String[] subset = constraintAnnotation.values();
        if (subset.length > 0) {
            acceptedValues = Arrays.stream(subset)
                    .filter(allNames::contains)
                    .collect(Collectors.toSet());
        } else {
            acceptedValues = allNames;
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        boolean ok = ignoreCase
                ? acceptedValues.stream().anyMatch(e -> e.equalsIgnoreCase(value))
                : acceptedValues.contains(value);

        if (!ok) {
            String acceptedList = String.join(", ", acceptedValues);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    messageTemplate.replace("{accepted}", acceptedList)
            ).addConstraintViolation();
        }

        return ok;
    }
}