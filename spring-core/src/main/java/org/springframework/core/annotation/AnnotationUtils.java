package org.springframework.core.annotation;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * General utility methods for working with annotations, handling meta-annotations,
 * bridge methods (which the compiler generates for generic declarations) as well
 * as super methods (for optional <em>annotation inheritance</em>).
 *
 * <p>Note that most of the features of this class are not provided by the
 * JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained application annotations (e.g. for
 * transaction control, authorization, or service exposure), always use the
 * lookup methods on this class (e.g. {@link #findAnnotation(Method, Class)} or
 * {@link #getAnnotation(Method, Class)}) instead of the plain annotation lookup
 * methods in the JDK. You can still explicitly choose between a <em>get</em>
 * lookup on the given class level only ({@link #getAnnotation(Method, Class)})
 * and a <em>find</em> lookup in the entire inheritance hierarchy of the given
 * method ({@link #findAnnotation(Method, Class)}).
 *
 * <h3>Terminology</h3>
 * The terms <em>directly present</em>, <em>indirectly present</em>, and
 * <em>present</em> have the same meanings as defined in the class-level
 * javadoc for {@link AnnotatedElement} (in Java 8).
 *
 * <p>An annotation is <em>meta-present</em> on an element if the annotation
 * is declared as a meta-annotation on some other annotation which is
 * <em>present</em> on the element. Annotation {@code A} is <em>meta-present</em>
 * on another annotation if {@code A} is either <em>directly present</em> or
 * <em>meta-present</em> on the other annotation.
 *
 * <h3>Meta-annotation Support</h3>
 * <p>Most {@code find*()} methods and some {@code get*()} methods in this class
 * provide support for finding annotations used as meta-annotations. Consult the
 * javadoc for each method in this class for details. For fine-grained support for
 * meta-annotations with <em>attribute overrides</em> in <em>composed annotations</em>,
 * consider using {@link AnnotatedElementUtils}'s more specific methods instead.
 *
 * <h3>Attribute Aliases</h3>
 * <p>All public methods in this class that return annotations, arrays of
 * annotations, or {@link AnnotationAttributes} transparently support attribute
 * aliases configured via {@link AliasFor @AliasFor}. Consult the various
 * {@code synthesizeAnnotation*(..)} methods for details.
 *
 * <h3>Search Scope</h3>
 * <p>The search algorithms used by methods in this class stop searching for
 * an annotation once the first annotation of the specified type has been
 * found. As a consequence, additional annotations of the specified type will
 * be silently ignored.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 * @since 2.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {

    /**
     * If the supplied throwable is an {@link AnnotationConfigurationException},
     * it will be cast to an {@code AnnotationConfigurationException} and thrown,
     * allowing it to propagate to the caller.
     * <p>Otherwise, this method does nothing.
     * @param ex the throwable to inspect
     */
    static void rethrowAnnotationConfigurationException(Throwable ex) {
        if (ex instanceof AnnotationConfigurationException) {
            throw (AnnotationConfigurationException) ex;
        }
    }

    /**
     * Handle the supplied annotation introspection exception.
     * <p>If the supplied exception is an {@link AnnotationConfigurationException},
     * it will simply be thrown, allowing it to propagate to the caller, and
     * nothing will be logged.
     * <p>Otherwise, this method logs an introspection failure (in particular for
     * a {@link TypeNotPresentException}) before moving on, assuming nested
     * {@code Class} values were not resolvable within annotation attributes and
     * thereby effectively pretending there were no annotations on the specified
     * element.
     * @param element the element that we tried to introspect annotations on
     * @param ex the exception that we encountered
     * @see #rethrowAnnotationConfigurationException
     * @see IntrospectionFailureLogger
     */
    static void handleIntrospectionFailure(@Nullable AnnotatedElement element, Throwable ex) {
        rethrowAnnotationConfigurationException(ex);
        IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
        boolean meta = false;
        if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
            // Meta-annotation or (default) value lookup on an annotation type
            logger = IntrospectionFailureLogger.DEBUG;
            meta = true;
        }
        if (logger.isEnabled()) {
            String message = meta ?
                    "Failed to meta-introspect annotation " :
                    "Failed to introspect annotations on ";
            logger.log(message + element + ": " + ex);
        }
    }

}
