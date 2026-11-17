package app.lexo.domain;

import java.util.List;

public final class PublicPaths {

    private static final List<String> EXACT = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/senha/esqueci",
            "/api/auth/senha/redefinir",
            "/api/convites/aceitar"
    );

    private static final List<String> PREFIXES = List.of(
            "/api/convites/info/",
            "/api/portal/",
            "/docs/"
    );

    private PublicPaths() {
    }

    public static boolean isPublic(String path) {
        return EXACT.contains(path) || PREFIXES.stream().anyMatch(path::startsWith);
    }
}
