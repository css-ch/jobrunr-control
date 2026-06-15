package ch.css.jobrunr.control.jobs.complex;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PoliceNrListValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PoliceNrListValidation {
    String message() default "Format der PoliceNr-Liste nicht gueltig";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}