package org.springframework.core;

/**
 * A common delegate for detecting a GraalVM native image environment.
 *
 * <p>Requires using the {@code -H:+InlineBeforeAnalysis} native image compiler flag in order to allow code removal at
 * build time.
 *
 * @author Sebastien Deleuze
 * @since 5.3.4
 */
public abstract class NativeDetector {

    // See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
    private static final boolean imageCode = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);

    /**
     * Returns {@code true} if invoked in the context of image building or during image runtime, else {@code false}.
     */
    public static boolean inNativeImage() {
        return imageCode;
    }
}
