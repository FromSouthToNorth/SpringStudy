package org.springframework.core.annotation;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Set;

/**
 * General utility methods for finding annotations, meta-annotations, and
 * repeatable annotations on {@link AnnotatedElement AnnotatedElements}.
 *
 * <p>{@code AnnotatedElementUtils} defines the public API for Spring's
 * meta-annotation programming model with support for <em>annotation attribute
 * overrides</em>. If you do not need support for annotation attribute
 * overrides, consider using {@link AnnotationUtils} instead.
 *
 * <p>Note that the features of this class are not provided by the JDK's
 * introspection facilities themselves.
 *
 * <h3>Annotation Attribute Overrides</h3>
 * <p>Support for meta-annotations with <em>attribute overrides</em> in
 * <em>composed annotations</em> is provided by all variants of the
 * {@code getMergedAnnotationAttributes()}, {@code getMergedAnnotation()},
 * {@code getAllMergedAnnotations()}, {@code getMergedRepeatableAnnotations()},
 * {@code findMergedAnnotationAttributes()}, {@code findMergedAnnotation()},
 * {@code findAllMergedAnnotations()}, and {@code findMergedRepeatableAnnotations()}
 * methods.
 *
 * <h3>Find vs. Get Semantics</h3>
 * <p>The search algorithms used by methods in this class follow either
 * <em>find</em> or <em>get</em> semantics. Consult the javadocs for each
 * individual method for details on which search algorithm is used.
 *
 * <p><strong>Get semantics</strong> are limited to searching for annotations
 * that are either <em>present</em> on an {@code AnnotatedElement} (i.e. declared
 * locally or {@linkplain java.lang.annotation.Inherited inherited}) or declared
 * within the annotation hierarchy <em>above</em> the {@code AnnotatedElement}.
 *
 * <p><strong>Find semantics</strong> are much more exhaustive, providing
 * <em>get semantics</em> plus support for the following:
 *
 * <ul>
 * <li>Searching on interfaces, if the annotated element is a class
 * <li>Searching on superclasses, if the annotated element is a class
 * <li>Resolving bridged methods, if the annotated element is a method
 * <li>Searching on methods in interfaces, if the annotated element is a method
 * <li>Searching on methods in superclasses, if the annotated element is a method
 * </ul>
 *
 * <h3>Support for {@code @Inherited}</h3>
 * <p>Methods following <em>get semantics</em> will honor the contract of Java's
 * {@link java.lang.annotation.Inherited @Inherited} annotation except that locally
 * declared annotations (including custom composed annotations) will be favored over
 * inherited annotations. In contrast, methods following <em>find semantics</em>
 * will completely ignore the presence of {@code @Inherited} since the <em>find</em>
 * search algorithm manually traverses type and method hierarchies and thereby
 * implicitly supports annotation inheritance without a need for {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotationUtils
 * @see BridgeMethodResolver
 */
public abstract class AnnotatedElementUtils {

    public static AnnotatedElement forAnnotations(Annotation... annotations) {
        return new AnnotatedElementEorAnnotations(annotations);
    }

    /**
     * Get the fully qualified class names of all meta-annotation types
     * <em>present</em> on the annotation (of the specified {@code annotationType})
     * on the supplied {@link AnnotatedElement}.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level javadoc}.
     * @param element the annotated element
     * @param annotationType the annotation type on which to find meta-annotations
     * @return the names of all meta-annotations present on the annotation,
     * or an empty set if not found
     * @since 4.2
     * @see #getMetaAnnotationTypes(AnnotatedElement, String)
     * @see #hasMetaAnnotationTypes
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element,
            Class<? extends Annotation> annotationType) {
        return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
    }

    /**
     * Get the fully qualified class names of all meta-annotation
     * types <em>present</em> on the annotation (of the specified
     * {@code annotationName}) on the supplied {@link AnnotatedElement}.
     * <p>This method follows <em>get semantics</em> as described in the
     * {@linkplain AnnotatedElementUtils class-level javadoc}.
     * @param element the annotated element
     * @param annotationName the fully qualified class name of the annotation
     * type on which to find meta-annotations
     * @return the names of all meta-annotations present on the annotation,
     * or an empty set if none found
     * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
     * @see #hasMetaAnnotationTypes
     */
    public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return getMetaAnnotationTypes(element, annotation);
            }
        }
        return Collections.emptySet();
    }

    private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, @Nullable Annotation annotation) {
        if (annotation == null) {
            return Collections.emptySet();
        }
//        return getAnnotations(annotation.annotationType())
    }

//    public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
//        // Shortcut: directly present on the element, with no merging needed?
//        AnnotationFilter.PLAIN.matches(annotationType) ||
//                A
//    }

//    private static MergedA

    /**
     * Adapted {@link AnnotatedElement} that hold specific annotations.
     */
    private static class AnnotatedElementEorAnnotations implements AnnotatedElement {

        private final Annotation[] annotations;

        AnnotatedElementEorAnnotations(Annotation... annotations) {
            this.annotations = annotations;
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nullable
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : this.annotations) {
                if (annotation.annotationType() == annotationClass) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return this.annotations.clone();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return this.annotations.clone();
        }
    }

}
