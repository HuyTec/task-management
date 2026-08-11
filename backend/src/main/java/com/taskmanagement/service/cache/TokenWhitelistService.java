package com.taskmanagement.service.cache;

import org.springframework.stereotype.Service;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenWhitelistService {
    private static final String KEY_PREFIX = "whitelist:";
    
    private static final long TTL = Duration.ofDays(7).toMillis(); // 7 days in milliseconds

    public void addTokenToWhitelist(String tokenId, String username) {
        tokenCacheService.addTokenToWhitelist(tokenId, username);
    }

    public boolean isTokenWhitelisted(String tokenId) {
        return tokenCacheService.isTokenWhitelisted(tokenId);
    }

    public void removeTokenFromWhitelist(String tokenId) {
        tokenCacheService.removeTokenFromWhitelist(tokenId);
    }
}
