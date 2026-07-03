package app.lexo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protege as rotas /api/ia/**: exige o header X-Org-Id (injetado pelo gateway apos autenticar,
 * garantindo que a requisicao passou pelo fluxo autenticado) e aplica rate limit por organizacao,
 * evitando abuso e estouro da cota/gasto do provedor de IA.
 */
@Component
public class IaRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 20;
    private static final long JANELA_MS = 60_000;

    private final Map<String, Deque<Long>> historico = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!req.getRequestURI().startsWith("/api/ia/")) {
            chain.doFilter(req, res);
            return;
        }
        String org = req.getHeader("X-Org-Id");
        if (org == null || org.isBlank()) {
            responder(res, 401, "Acesso permitido apenas pelo gateway autenticado.");
            return;
        }
        if (excedeuLimite(org)) {
            responder(res, 429, "Muitas requisicoes de IA. Tente novamente em instantes.");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean excedeuLimite(String org) {
        long agora = System.currentTimeMillis();
        Deque<Long> janela = historico.computeIfAbsent(org, k -> new ArrayDeque<>());
        synchronized (janela) {
            while (!janela.isEmpty() && agora - janela.peekFirst() > JANELA_MS) {
                janela.pollFirst();
            }
            if (janela.size() >= LIMITE) {
                return true;
            }
            janela.addLast(agora);
            return false;
        }
    }

    private void responder(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"" + msg + "\"}");
    }
}
