package org.springframework.lang;

import java.lang.annotation.*;

/**
 * Indicates that the annotated element uses an API from the {@code sun.misc}
 * package.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Documented
public @interface UsesSunMisc {
}
