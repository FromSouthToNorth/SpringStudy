package org.springframework.core.annotation;

import org.springframework.core.OrderComparator;

/**
 * {@code AnnotationAwareOrderComparator} is an extension of
 * {@link OrderComparator} that supports Spring's
 * {@link org.springframework.core.Ordered} interface as well as the
 * {@link Order @Order} and {@link javax.annotation.Priority @Priority}
 * annotations, with an order value provided by an {@code Ordered}
 * instance overriding a statically defined annotation value (if any).
 *
 * <p>Consult the Javadoc for {@link OrderComparator} for details on the
 * sort semantics for non-ordered objects.
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 */
public class AnnotationAwareOrderComparator extends OrderComparator {

    public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();

    protected Integer findOrder(Object obj) {
        Integer order = super.find
    }

}
