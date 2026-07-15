package com.turnero.security;

public record AuthPrincipal(Long userId, Long tenantId, String role) {}
