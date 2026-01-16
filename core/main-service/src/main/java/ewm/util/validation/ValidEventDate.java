package ewm.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = EventDateValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventDate {
    String message() default "Дата события должна быть не раньше чем через 2 часа от текущего момента";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int hours() default 2;
}