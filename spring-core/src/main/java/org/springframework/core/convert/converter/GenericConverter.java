package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * Generic converter interface for converting between two or more types.
 *
 * <p>This is the most flexible of the Converter SPI interfaces, but also the most complex.
 * It is flexible in that a GenericConverter may support converting between multiple source/target
 * type pairs (see {@link #getConvertibleTypes()}. In addition, GenericConverter implementations
 * have access to source/target {@link TypeDescriptor field context} during the type conversion
 * process. This allows for resolving source and target field metadata such as annotations and
 * generics information, which can be used to influence the conversion logic.
 *
 * <p>This interface should generally not be used when the simpler {@link Converter} or
 * {@link ConverterFactory} interface is sufficient.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 */
public interface GenericConverter {

    /**
     * Return the source and target types that this converter can convert between.
     * <p>Each entry is a convertible source-to-target type pair.
     * <p>For {@link ConditionalConverter conditional converters} this method may return
     * {@code null} to indicate all source-to-target pairs should be considered.
     */
    @Nullable
    Set<ConvertiblePair> getConvertibleTypes();

    /**
     * Convert the source object to the targetType described by the {@code TypeDescriptor}.
     * @param source the source object to convert (may be {@code null})
     * @param sourceType the type descriptor of the field we are converting from
     * @param targetType the type descriptor of the field we are converting to
     * @return the converted object
     */
    @Nullable
    Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

    /**
     * Holder for a source-to-target class pair.
     */
    final class ConvertiblePair {

        private final Class<?> sourceType;

        private final Class<?> targetType;

        /**
         * Create a new source-to-target pair.
         * @param sourceType the source type
         * @param targetType the target type
         */
        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            Assert.notNull(sourceType, "Source type must not be null");
            Assert.notNull(targetType, "Target type must not be null");
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return sourceType;
        }

        public Class<?> getTargetType() {
            return targetType;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || other.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair otherPair = (ConvertiblePair) other;
            return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
        }

        @Override
        public int hashCode() {
            return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
        }

        @Override
        public String toString() {
            return (this.sourceType.getName() + " -> " + this.targetType.getName());
        }
    }

}
