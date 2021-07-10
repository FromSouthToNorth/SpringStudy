package org.springframework.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Simple utility methods for dealing with streams. The copy methods of this class are
 * similar to those defined in {@link FileCopyUtils} except that all affected streams are
 * left open when done. All copy methods use a block size of 4096 bytes.
 *
 * <p>Mainly for use within the framework, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 3.2.2
 * @see FileCopyUtils
 */
public abstract class StreamUtils {

    /**
     * Copy the contents of the given {@link ByteArrayOutputStream} into a {@link String}.
     * <p>This is a more effective equivalent of {@code new String(baos.toByteArray(), charset)}.
     * @param baos the {@code ByteArrayOutputStream} to be copied into a String
     * @param charset the {@link Charset} to use to decode the bytes
     * @return the String that has been copied to (possibly empty)
     * @since 5.2.6
     */
    public static String copyToString(ByteArrayOutputStream baos, Charset charset) {
        Assert.notNull(baos, "No ByteArrayOutputStream specified");
        Assert.notNull(charset, "No Charset specified");
        try {
            // Can be replaced with toString(Charset) call in Java 10+
            return baos.toString(charset.name());
        }
        catch (UnsupportedEncodingException ex) {
            // Should never happen
            throw new IllegalArgumentException("Invalid charset name: " + charset, ex);
        }
    }
}
