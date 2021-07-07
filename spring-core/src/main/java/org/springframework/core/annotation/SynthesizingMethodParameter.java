package org.springframework.core.annotation;

import org.springframework.core.MethodParameter;

import java.lang.reflect.Constructor;

/**
 * A {@link MethodParameter} variant which synthesizes annotations that
 * declare attribute aliases via {@link AliasFor @AliasFor}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 * @see AnnotationUtils#synthesizeAnnotation
 * @see AnnotationUtils#synthesizeAnnotationArray
 */
public class SynthesizingMethodParameter extends MethodParameter {

    /**
     * Create a new {@code SynthesizingMethodParameter} for the given constructor,
     * with nesting level 1.
     * @param constructor the Constructor to specify a parameter for
     * @param parameterIndex the index of the parameter
     */
    public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
        super(constructor, parameterIndex);
    }
}
