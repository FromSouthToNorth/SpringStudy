package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * A basic {@link ConfigurablePropertyAccessor} that provides the necessary
 * infrastructure for all typical use cases.
 *
 * <p>This accessor will convert collection and array values to the corresponding
 * target collections or arrays, if necessary. Custom property editors that deal
 * with collections or arrays can either be written via PropertyEditor's
 * {@code setValue}, or against a comma-delimited String via {@code setAsText},
 * as String arrays are converted in such a format if the array itself is not
 * assignable.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 4.2
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {

    private static final Log logger = LogFactory.getLog(AbstractNestablePropertyAccessor.class);

    private int autoGrowCollectionLimit = Integer.MAX_VALUE;

    @Nullable
    Object wrappedObject;

    private String nestedPath = "";

    @Nullable
    Object rootObject;

    /** Map with cached nested Accessors: nested path -> Accessor instance. */
    @Nullable
    private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessorMap;

    /**
     * Create a new empty accessor. Wrapped instance needs to be set afterwards.
     * Registers default editors.
     * @see #setWrappedInstance
     */
    protected AbstractNestablePropertyAccessor() {
        this(true);
    }

    protected AbstractNestablePropertyAccessor(boolean registerDefaultEditors) {
//        if (registerDefaultEditors) {
//            registerDefaultEditors();
//        }
//        this.ty
    }
}
