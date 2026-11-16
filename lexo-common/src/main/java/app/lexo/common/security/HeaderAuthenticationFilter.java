package app.lexo.common.security;

import app.lexo.common.signing.GatewaySignature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_ORG_ID = "X-Org-Id";
    public static final String H_ROLE = "X-User-Role";
    public static final String H_NAME = "X-User-Name";
    public static final String H_EMAIL = "X-User-Email";

    private final String signingKey;
    private final long toleranceSeconds;

    public HeaderAuthenticationFilter(
            @Value("${lexo.gateway.signing-key}") String signingKey,
            @Value("${lexo.gateway.signature-tolerance-seconds:60}") long toleranceSeconds,
            Environment env) {
        var profiles = Arrays.asList(env.getActiveProfiles());
        GatewaySignature.requireSecureKey(signingKey,
                profiles.contains("dev") || profiles.contains("test"));
        this.signingKey = signingKey;
        this.toleranceSeconds = toleranceSeconds;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(H_USER_ID);
        String orgId = request.getHeader(H_ORG_ID);
        String role = request.getHeader(H_ROLE);

        if (userId == null || orgId == null || role == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String name = request.getHeader(H_NAME);
        String email = request.getHeader(H_EMAIL);

        if (!hasValidSignature(request, userId, orgId, role, name, email)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String decodedName = name == null ? null : URLDecoder.decode(name, StandardCharsets.UTF_8);
            AuthUser user = new AuthUser(userId, orgId, decodedName, email, Role.valueOf(role));
            var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
            var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasValidSignature(HttpServletRequest request, String userId, String orgId,
                                     String role, String name, String email) {
        String rawTimestamp = request.getHeader(GatewaySignature.H_TIMESTAMP);
        String signature = request.getHeader(GatewaySignature.H_SIGNATURE);
        if (rawTimestamp == null || signature == null) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(rawTimestamp.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        if (!GatewaySignature.withinWindow(timestamp, Instant.now().getEpochSecond(), toleranceSeconds)) {
            return false;
        }

        String payload = GatewaySignature.payload(userId, orgId, role, name, email, timestamp);
        return GatewaySignature.verify(signingKey, payload, signature);
    }
}
