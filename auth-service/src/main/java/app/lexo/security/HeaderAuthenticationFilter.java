package app.lexo.security;

import app.lexo.domain.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.List;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String H_USER_ID = "X-User-Id";
    private static final String H_ORG_ID = "X-Org-Id";
    private static final String H_ROLE = "X-User-Role";
    private static final String H_NAME = "X-User-Name";
    private static final String H_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(H_USER_ID);
        String orgId = request.getHeader(H_ORG_ID);
        String role = request.getHeader(H_ROLE);

        if (userId != null && orgId != null && role != null) {
            try {
                String name = request.getHeader(H_NAME);
                name = name == null ? null : URLDecoder.decode(name, StandardCharsets.UTF_8);
                String email = request.getHeader(H_EMAIL);

                AuthUser user = new AuthUser(userId, orgId, name, email, Role.valueOf(role));
                var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
