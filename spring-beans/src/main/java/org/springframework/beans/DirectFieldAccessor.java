package org.springframework.beans;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * {@link ConfigurablePropertyAccessor} implementation that directly accesses
 * instance fields. Allows for direct binding to fields instead of going through
 * JavaBean setters.
 *
 * <p>As of Spring 4.2, the vast majority of the {@link BeanWrapper} features have
 * been merged to {@link AbstractPropertyAccessor}, which means that property
 * traversal as well as collections and map access is now supported here as well.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.springframework.validation.DirectFieldBindingResult
 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

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
