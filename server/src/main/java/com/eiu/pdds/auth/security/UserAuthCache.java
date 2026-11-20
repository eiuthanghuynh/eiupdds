package com.eiu.pdds.auth.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Cache abstraction for live user authorization and account status used during JWT authentication.
 */
public interface UserAuthCache {

    Optional<CachedUserAuthState> get(UUID userId);

    void put(CachedUserAuthState state);

    void evict(UUID userId);
}
