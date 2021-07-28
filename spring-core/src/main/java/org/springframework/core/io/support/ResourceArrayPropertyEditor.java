package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditorSupport;

/**
 * Editor for {@link org.springframework.core.io.Resource} arrays, to
 * automatically convert {@code String} location patterns
 * (e.g. {@code "file:C:/my*.txt"} or {@code "classpath*:myfile.txt"})
 * to {@code Resource} array properties. Can also translate a collection
 * or array of location patterns into a merged Resource array.
 *
 * <p>A path may contain {@code ${...}} placeholders, to be
 * resolved as {@link org.springframework.core.env.Environment} properties:
 * e.g. {@code ${user.dir}}. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourcePatternResolver},
 * by default using a {@link PathMatchingResourcePatternResolver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.2
 * @see org.springframework.core.io.Resource
 * @see ResourcePatternResolver
 * @see PathMatchingResourcePatternResolver
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

    private static final Log logger = LogFactory.getLog(ResourceArrayPropertyEditor.class);

    private final ResourcePatternResolver resourcePatternResolver;

    @NonNull
    private PropertyResolver propertyResolver;

    private final boolean ignoreUnresolvablePlaceholders;

    public ResourceArrayPropertyEditor() {
        this(new PathMatchingResourcePatternResolver(), null, true);
    }

    /**
     * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
     * and {@link PropertyResolver} (typically an {@link Environment}).
     * @param resourcePatternResolver the ResourcePatternResolver to use
     * @param propertyResolver the PropertyResolver to use
     */
    public ResourceArrayPropertyEditor(
            ResourcePatternResolver resourcePatternResolver, @Nullable PropertyResolver propertyResolver) {

        this(resourcePatternResolver, propertyResolver, true);
    }

    /**
     * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
     * and {@link PropertyResolver} (typically an {@link Environment}).
     * @param resourcePatternResolver the ResourcePatternResolver to use
     * @param propertyResolver the PropertyResolver to use
     * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
     * if no corresponding system property could be found
     */
    public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
            @Nullable PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

        Assert.notNull(resourcePatternResolver, "ResourcePatternResolver must not be null");
        this.resourcePatternResolver = resourcePatternResolver;
        this.propertyResolver = propertyResolver;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

}
