package app.lexo.service;

import app.lexo.client.FinanceiroPortalClient;
import app.lexo.client.ProcessoPortalClient;
import app.lexo.controller.ApiException;
import app.lexo.domain.Client;
import app.lexo.dto.PortalDtos.HonorarioPortal;
import app.lexo.dto.PortalDtos.PortalResponse;
import app.lexo.dto.PortalDtos.ProcessoPortal;
import app.lexo.dto.PortalDtos.ResumoPortal;
import app.lexo.repository.ClientRepository;
import app.lexo.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Portal do cliente: gera o link (token) e monta a visao publica read-only,
 * agregando processos (processo-service) e honorarios (financeiro-service) via Feign.
 */
@Service
public class PortalService {

    private static final Logger log = LoggerFactory.getLogger(PortalService.class);
    private static final Set<String> EM_ABERTO = Set.of("PENDENTE", "ATRASADO");

    private final ClientRepository repo;
    private final ProcessoPortalClient processoClient;
    private final FinanceiroPortalClient financeiroClient;

    public PortalService(ClientRepository repo, ProcessoPortalClient processoClient,
                         FinanceiroPortalClient financeiroClient) {
        this.repo = repo;
        this.processoClient = processoClient;
        this.financeiroClient = financeiroClient;
    }

    /** Gera (ou rotaciona) o token do portal do cliente e devolve o token. */
    @Transactional
    public String gerarToken(AuthUser me, String clientId) {
        Client c = repo.findByIdAndOrganizationId(clientId, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        c.setPortalToken(UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""));
        repo.save(c);
        return c.getPortalToken();
    }

    /** Visao publica do portal a partir do token (sem autenticacao). */
    @Transactional(readOnly = true)
    public PortalResponse porToken(String token) {
        Client c = repo.findByPortalToken(token)
                .orElseThrow(() -> ApiException.notFound("Link inválido ou expirado"));
        String org = c.getOrganizationId();

        List<ProcessoPortal> processos = buscar(() -> processoClient.processosDoCliente(c.getId(), org), "processos");
        List<HonorarioPortal> honorarios = buscar(() -> financeiroClient.honorariosDoCliente(c.getId(), org), "honorarios");

        BigDecimal emAberto = honorarios.stream()
                .filter(h -> EM_ABERTO.contains(h.status()))
                .map(HonorarioPortal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortalResponse(c.getName(), processos, honorarios,
                new ResumoPortal(processos.size(), emAberto));
    }

    /** Degrada com elegancia: se o servico destino estiver fora, devolve lista vazia. */
    private <T> List<T> buscar(java.util.function.Supplier<List<T>> chamada, String nome) {
        try {
            return chamada.get();
        } catch (Exception e) {
            log.warn("Portal: falha ao buscar {} ({}). Retornando vazio.", nome, e.toString());
            return List.of();
        }
    }
}
