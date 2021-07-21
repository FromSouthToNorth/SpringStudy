package org.springframework;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link org.springframework.core.AttributeAccessorSupport},
 * holding attributes as {@link BeanMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {

    @Nullable
    private Object source;

    @Nullable
    @Override
    public Object getSource() {
        return BeanMetadataElement.super.getSource();
    }

    @Override
    public boolean hasAttribute(String name) {
        return false;
    }

}
