package org.springframework.util;

import java.util.UUID;

/**
 * An {@link IdGenerator} that calls {@link java.util.UUID#randomUUID()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1.5
 */
public class JdkIdGenerator implements IdGenerator {

    @Override
    public UUID generateId() {
        return UUID.randomUUID();
    }

}
