package app.lexo.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String H_INTERNAL_KEY = "X-Internal-Key";
    public static final String ROLE_INTERNAL = "INTERNAL";

    private static final String PREFIXO = "/internal/";

    private final byte[] chaveEsperada;
    private final boolean habilitado;

    public InternalKeyFilter(@Value("${lexo.internal.api-key:}") String chaveInterna) {
        this.habilitado = chaveInterna != null && !chaveInterna.isBlank();
        this.chaveEsperada = this.habilitado
                ? chaveInterna.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (habilitado && request.getRequestURI().startsWith(PREFIXO) && chaveConfere(request)) {
            var authority = new SimpleGrantedAuthority("ROLE_" + ROLE_INTERNAL);
            var authentication = new UsernamePasswordAuthenticationToken(
                    "servico-interno", null, List.of(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean chaveConfere(HttpServletRequest request) {
        String recebida = request.getHeader(H_INTERNAL_KEY);
        if (recebida == null) {
            return false;
        }
        return MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8));
    }
}
