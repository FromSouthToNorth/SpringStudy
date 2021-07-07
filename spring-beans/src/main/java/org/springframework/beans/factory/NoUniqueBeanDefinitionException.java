package org.springframework.beans.factory;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * Exception thrown when a {@code BeanFactory} is asked for a bean instance for which
 * multiple matching candidates have been found when only one matching bean was expected.
 *
 * @author Juergen Hoeller
 * @since 3.2.1
 * @see BeanFactory#getBean(Class)
 */
@SuppressWarnings("serial")
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    private final int numberOfBeansFound;

    @Nullable
    private final Collection<String> beanNamesFound;

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param numberOfBeansFound the number of matching beans
     * @param message detailed message describing the problem
     */
    public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
        super(type, message);
        this.numberOfBeansFound = numberOfBeansFound;
        this.beanNamesFound = null;
    }

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param beanNamesFound the names of all matching beans (as a Collection)
     */
    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
                StringUtils.collectionToCommaDelimitedString(beanNamesFound));
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }
}
