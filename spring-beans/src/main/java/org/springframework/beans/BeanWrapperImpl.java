package org.springframework.beans;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Default {@link BeanWrapper} implementation that should be sufficient
 * for all typical use cases. Caches introspection results for efficiency.
 *
 * <p>Note: Auto-registers default property editors from the
 * {@code org.springframework.beans.propertyeditors} package, which apply
 * in addition to the JDK's standard PropertyEditors. Applications can call
 * the {@link #registerCustomEditor(Class, java.beans.PropertyEditor)} method
 * to register an editor for a particular instance (i.e. they are not shared
 * across the application). See the base class
 * {@link PropertyEditorRegistrySupport} for details.
 *
 * <p><b>NOTE: As of Spring 2.5, this is - for almost all purposes - an
 * internal class.</b> It is just public in order to allow for access from
 * other framework packages. For standard application access purposes, use the
 * {@link PropertyAccessorFactory#forBeanPropertyAccess} factory method instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @since 15 April 2001
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

    @Override
    public boolean isReadableProperty(String propertyName) {
        return false;
    }

    @Override
    public boolean isWritableProperty(String propertyName) {
        return false;
    }

    @Nullable
    @Override
    public Class<?> getPropertyType(String propertyName) throws BeansException {
        return null;
    }

    @Nullable
    @Override
    public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
        return null;
    }

    @Nullable
    @Override
    public Object getPropertyValue(String propertyName) throws BeansException {
        return null;
    }

    @Override
    public void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException {

    }

    @Override
    public void setPropertyValues(Map<?, ?> map) throws BeansException {

    }

    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {

    }

    @Nullable
    @Override
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
        return null;
    }

    @Nullable
    @Override
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable MethodParameter methodParam) throws TypeMismatchException {
        return null;
    }

    @Nullable
    @Override
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {
        return null;
    }

}
