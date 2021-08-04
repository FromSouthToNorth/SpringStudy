package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Strategy API for extracting a value for an annotation attribute from a given
 * source object which is typically an {@link Annotation}, {@link Map}, or
 * {@link TypeMappedAnnotation}.
 *
 * @since 5.2.4
 * @author Sam Brannen
 */
@FunctionalInterface
public interface ValueExtractor {

    /**
     * Extract the annotation attribute represented by the supplied {@link Method}
     * from the supplied source {@link Object}.
     */
    @Nullable
    Object extract(Method attribute, @Nullable Object object);

}
