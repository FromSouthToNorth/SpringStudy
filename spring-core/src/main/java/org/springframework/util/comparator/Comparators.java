package org.springframework.util.comparator;

import java.util.Comparator;

/**
 * Convenient entry point with generically typed factory methods
 * for common Spring {@link Comparator} variants.
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class Comparators {

    /**
     * Return a {@link Comparable} adapter.
     * @see ComparableComparator#INSTANCE
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> comparable() {
        return ComparableComparator.INSTANCE;
    }

    /**
     * Return a {@link Comparable} adapter which accepts
     * null values and sorts them lower than non-null values.
     * @see NullSafeComparator#NULLS_LOW
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsLow() {
        return NullSafeComparator.NULLS_LOW;
    }

    /**
     * Return a decorator for the given comparator which accepts
     * null values and sorts them lower than non-null values.
     * @see NullSafeComparator#NullSafeComparator(boolean)
     */
    public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, true);
    }

    /**
     * Return a {@link Comparable} adapter which accepts
     * null values and sorts them higher than non-null values.
     * @see NullSafeComparator#NULLS_HIGH
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> nullsHigh() {
        return NullSafeComparator.NULLS_HIGH;
    }

    /**
     * Return a decorator for the given comparator which accepts
     * null values and sorts them higher than non-null values.
     * @see NullSafeComparator#NullSafeComparator(boolean)
     */
    public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
        return new NullSafeComparator<>(comparator, false);
    }

}
