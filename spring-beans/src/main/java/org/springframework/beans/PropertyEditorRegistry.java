package org.springframework.beans;

import java.beans.PropertyEditor;

/**
 * Encapsulates methods for registering JavaBeans {@link PropertyEditor PropertyEditors}.
 * This is the central interface that a {@link PropertyEditorRegistrar} operates on.
 *
 * <p>Extended by {@link BeanWrapper}; implemented by {@link BeanWrapperImpl}
 * and {@link org.springframework.validation.DataBinder}.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 */
public interface PropertyEditorRegistry {
}
