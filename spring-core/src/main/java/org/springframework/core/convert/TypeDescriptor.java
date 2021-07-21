package org.springframework.core.convert;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Contextual descriptor about a type to convert from or to.
 * Capable of representing arrays and generic collection types.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

    private static final Class<?>[] CACHED_COMMON_TYPES = {
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            double.class, Double.class, float.class, Float.class, int.class, Integer.class,
            long.class, Long.class, short.class, Short.class, String.class, Object.class
    };

    static {
        for (Class<?> perCachedClass : CACHED_COMMON_TYPES) {
            commonTypesCache.put(perCachedClass, valueOf(perCachedClass));
        }
    }

    private final Class<?> type;

    private final ResolvableType resolvableType;

    private final AnnotatedElementAdapter annotatedElement;

    /**
     * Create a new type descriptor from a {@link MethodParameter}.
     * <p>Use this constructor when a source or target conversion point is a
     * constructor parameter, method parameter, or method return value.
     * @param methodParameter the method parameter
     */
    public TypeDescriptor(MethodParameter methodParameter) {
        this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
        this.type = this.resolvableType.resolve(methodParameter.getParameterType());
        this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
                methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
    }

    /**
     * Create a new type descriptor from a {@link Field}.
     * <p>Use this constructor when a source or target conversion point is a field.
     * @param field the field
     */
    public TypeDescriptor(Field field) {
        this.resolvableType = ResolvableType.forField(field);
        this.type = this.resolvableType.resolve(field.getType());
        this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
    }

    /**
     * Create a new type descriptor from a {@link Property}.
     * <p>Use this constructor when a source or target conversion point is a
     * property on a Java class.
     * @param property the property
     */
    public TypeDescriptor(Property property) {
        Assert.notNull(property, "Property must not be null");
        this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
        this.type = this.resolvableType.resolve(property.getType());
        this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
    }

    /**
     * Create a new type descriptor from a {@link ResolvableType}.
     * <p>This constructor is used internally and may also be used by subclasses
     * that support non-Java languages with extended type systems. It is public
     * as of 5.1.4 whereas it was protected before.
     * @param resolvableType the resolvable type
     * @param type the backing type (or {@code null} if it should get resolved)
     * @param annotations the type annotations
     * @since 4.0
     */
    public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
        this.resolvableType = resolvableType;
        this.type = (type != null ? type : resolvableType.toClass());
        this.annotatedElement = new AnnotatedElementAdapter(annotations);
    }

    /**
     * Variation of {@link #getType()} that accounts for a primitive type by
     * returning its object wrapper type.
     * <p>This is useful for conversion service implementations that wish to
     * normalize to object-based types and not work with primitive types directly.
     */
    public Class<?> getObjectType() {
        return ClassUtils.resolvePrimitiveIfNecessary(getType());
    }

    /**
     * The type of the backing class, method parameter, field, or property
     * described by this TypeDescriptor.
     * <p>Returns primitive types as-is. See {@link #getObjectType()} for a
     * variation of this operation that resolves primitive types to their
     * corresponding Object types if necessary.
     * @see #getObjectType()
     */
    public Class<?> getType() {
        return this.type;
    }

    /**
     * Return the underlying {@link ResolvableType}.
     * @since 4.0
     */
    public ResolvableType getResolvableType() {
        return this.resolvableType;
    }

    /**
     * Return the underlying source of the descriptor. Will return a {@link Field},
     * {@link MethodParameter} or {@link Type} depending on how the {@link TypeDescriptor}
     * was constructed. This method is primarily to provide access to additional
     * type information or meta-data that alternative JVM languages may provide.
     * @since 4.0
     */
    public Object getSource() {
        return this.resolvableType.getSource();
    }

    /**
     * Narrows this {@link TypeDescriptor} by setting its type to the class of the
     * provided value.
     * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
     * is returned unchanged.
     * <p>Designed to be called by binding frameworks when they read property, field,
     * or method return values. Allows such frameworks to narrow a TypeDescriptor built
     * from a declared property, field, or method return value type. For example, a field
     * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
     * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
     * can then be used to convert the HashMap to some other type. Annotation and nested
     * type context is preserved by the narrowed copy.
     * @param value the value to use for narrowing this type descriptor
     * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
     * class of the provided value)
     */
    public TypeDescriptor narrow(@Nullable Object value) {
        if (value == null) {
            return this;
        }
        ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
        return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
    }

    /**
     * Cast this {@link TypeDescriptor} to a superclass or implemented interface
     * preserving annotations and nested type context.
     * @param superType the super type to cast to (can be {@code null})
     * @return a new TypeDescriptor for the up-cast type
     * @throws IllegalArgumentException if this type is not assignable to the super-type
     * @since 3.2
     */
    @Nullable
    public TypeDescriptor upcast(@Nullable Class<?> superType) {
        if (superType == null) {
            return null;
        }
        Assert.isAssignable(superType, getType());
        return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
    }

    /**
     * Return the name of this type: the fully qualified class name.
     */
    public String getName() {
        return ClassUtils.getQualifiedName(getType());
    }

    /**
     * Is this type a primitive type?
     */
    public boolean isPrimitive() {
        return getType().isPrimitive();
    }

    /**
     * Return the annotations associated with this type descriptor, if any.
     * @return the annotations, or an empty array if none
     */
    public Annotation[] getAnnotations() {
        return this.annotatedElement.getAnnotations();
    }

//    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
//        if (this.annotatedElement.isEmpty()) {
//            return false;
//        }
//        return AnnotatedElementUtils.isA
//    }

    /**
     * Create a new type descriptor from the given type.
     * <p>Use this to instruct the conversion system to convert an object to a
     * specific target type, when no type location such as a method parameter or
     * field is available to provide additional conversion context.
     * <p>Generally prefer use of {@link #forObject(Object)} for constructing type
     * descriptors from source objects, as it handles the {@code null} object case.
     * @param type the class (may be {@code null} to indicate {@code Object.class})
     * @return the corresponding type descriptor
     */
    public static TypeDescriptor valueOf(@Nullable Class<?> type) {
        if (type == null) {
            type = Object.class;
        }
        TypeDescriptor desc = commonTypesCache.get(type);
        return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
    }

    /**
     * Adapter class for exposing a {@code TypeDescriptor}'s annotations as an
     * {@link AnnotatedElement}, in particular to {@link AnnotatedElementUtils}.
     * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
     * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
     */
    private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

        @Nullable
        private final Annotation[] annotations;

        public AnnotatedElementAdapter(@Nullable Annotation[] annotations) {
            this.annotations = annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }

        public boolean isEmpty() {
            return ObjectUtils.isEmpty(this.annotations);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return (this == other || (other instanceof AnnotatedElementAdapter &&
                    Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.annotations);
        }

        @Override
        public String toString() {
            return TypeDescriptor.this.toString();
        }

    }

}
