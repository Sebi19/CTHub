package org.cthub.backend.auth.stepup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint (or every endpoint of a controller) as a sensitive account mutation: the
 * current session must have re-proven its identity within {@link StepUpInterceptor#MAX_AGE},
 * otherwise the request fails with 403 {@code STEP_UP_REQUIRED} and the frontend runs a step-up.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresStepUp {
}
