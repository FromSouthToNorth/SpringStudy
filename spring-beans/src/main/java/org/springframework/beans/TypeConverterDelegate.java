package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.beans.PropertyEditor;

/**
 * Internal helper class for converting property values to target types.
 *
 * <p>Works on a given {@link PropertyEditorRegistrySupport} instance.
 * Used as a delegate by {@link BeanWrapperImpl} and {@link SimpleTypeConverter}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @since 2.0
 * @see BeanWrapperImpl
 * @see SimpleTypeConverter
 */
class TypeConverterDelegate {

    private static final Log logger = LogFactory.getLog(TypeConverterDelegate.class);

    private final PropertyEditorRegistrySupport propertyEditorRegistry;

    @Nullable
    private final Object targetObject;

    /**
     * Create a new TypeConverterDelegate for the given editor registry.
     * @param propertyEditorRegistry the editor registry to use
     */
    public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry) {
        this(propertyEditorRegistry, null);
    }

    /**
     * Create a new TypeConverterDelegate for the given editor registry and bean instance.
     * @param propertyEditorRegistry the editor registry to use
     * @param targetObject the target object to work on (as context that can be passed to editors)
     */
    public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry, @Nullable Object targetObject) {
        this.propertyEditorRegistry = propertyEditorRegistry;
        this.targetObject = targetObject;
    }

    /**
     * Convert the value to the required type for the specified property.
     * @param propertyName name of the property
     * @param oldValue the previous value, if available (may be {@code null})
     * @param newValue the proposed new value
     * @param requiredType the type we must convert to
     * (or {@code null} if not known, for example in case of a collection element)
     * @return the new value, possibly the result of type conversion
     * @throws IllegalArgumentException if type conversion failed
     */
    @Nullable
    public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
            Object newValue, @Nullable Class<T> requiredType) throws IllegalArgumentException {

        return convertIfNecessary(propertyName, oldValue, newValue, requiredType, TypeDescriptor.valueOf(requiredType));
    }

    /**
     * Convert the value to the required type (if necessary from a String),
     * for the specified property.
     * @param propertyName name of the property
     * @param oldValue the previous value, if available (may be {@code null})
     * @param newValue the proposed new value
     * @param requiredType the type we must convert to
     * (or {@code null} if not known, for example in case of a collection element)
     * @param typeDescriptor the descriptor for the target property or field
     * @return the new value, possibly the result of type conversion
     * @throws IllegalArgumentException if type conversion failed
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue, @Nullable Object newValue,
            @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws IllegalArgumentException {

        // Custom editor for this type?
//        PropertyEditor editor = this.propertyEditorRegistry.
        return null;
    }
}
