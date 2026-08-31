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

    private final String chaveAssinatura;
    private final long toleranciaSegundos;

    public HeaderAuthenticationFilter(
            @Value("${lexo.gateway.signing-key}") String chaveAssinatura,
            @Value("${lexo.gateway.signature-tolerance-seconds:60}") long toleranciaSegundos,
            Environment env) {
        var perfis = Arrays.asList(env.getActiveProfiles());
        GatewaySignature.exigirChaveSegura(chaveAssinatura,
                perfis.contains("dev") || perfis.contains("test"));
        this.chaveAssinatura = chaveAssinatura;
        this.toleranciaSegundos = toleranciaSegundos;
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

        String nome = request.getHeader(H_NAME);
        String email = request.getHeader(H_EMAIL);

        if (!assinaturaValida(request, userId, orgId, role, nome, email)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String nomeDecodificado = nome == null ? null : URLDecoder.decode(nome, StandardCharsets.UTF_8);
            AuthUser user = new AuthUser(userId, orgId, nomeDecodificado, email, Role.valueOf(role));
            var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
            var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean assinaturaValida(HttpServletRequest request, String userId, String orgId,
                                     String role, String nome, String email) {
        String timestampBruto = request.getHeader(GatewaySignature.H_TIMESTAMP);
        String assinatura = request.getHeader(GatewaySignature.H_ASSINATURA);
        if (timestampBruto == null || assinatura == null) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampBruto.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        if (!GatewaySignature.dentroDaJanela(timestamp, Instant.now().getEpochSecond(), toleranciaSegundos)) {
            return false;
        }

        String payload = GatewaySignature.payload(userId, orgId, role, nome, email, timestamp);
        return GatewaySignature.verify(chaveAssinatura, payload, assinatura);
    }
}
