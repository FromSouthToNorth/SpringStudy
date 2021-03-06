package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Holder for a key-value style attribute that is part of a bean definition.
 * Keeps track of the definition source in addition to the key-value pair.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class BeanMetadataAttribute implements BeanMetadataElement {

    private final String name;

    @Nullable
    private final Object value;

    @Nullable
    private Object source;

    public BeanMetadataAttribute(String name, @Nullable Object value) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
    }

    /**
     * Return the name of the attribute.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the value of the attribute.
     */
    @Nullable
    public Object getValue() {
        return this.value;
    }

    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource(@Nullable Object source) {
        this.source = source;
    }

    @Nullable
    @Override
    public Object getSource() {
        return this.source;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanMetadataAttribute)) {
            return false;
        }
        BeanMetadataAttribute otherMa = (BeanMetadataAttribute) other;
        return (this.name.equals(otherMa.name) &&
                ObjectUtils.nullSafeEquals(this.value, ((BeanMetadataAttribute) other).value) &&
                ObjectUtils.nullSafeEquals(this.source, otherMa.source));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "metadata attribute '" + this.name + "'";
    }

}
