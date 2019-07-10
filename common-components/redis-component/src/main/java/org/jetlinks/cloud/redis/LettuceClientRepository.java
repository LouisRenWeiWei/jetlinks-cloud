package org.jetlinks.cloud.redis;

import org.jetlinks.lettuce.LettucePlus;

import java.util.Optional;

public interface LettuceClientRepository {

    Optional<LettucePlus> getClient(String name);

    default LettucePlus getClientOrDefault(String name) {

        return getClient(name).orElseGet(this::getDefaultClient);
    }

    default LettucePlus getDefaultClient() {
        return getClient("default").orElseThrow(NullPointerException::new);
    }
}
