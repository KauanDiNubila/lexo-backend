package app.lexo.application.service;

import app.lexo.application.port.in.ConsultarPortalQuery;
import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.in.ResumoPortal;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.CarregarHonorariosDoClientePort;
import app.lexo.application.port.out.CarregarProcessosDoClientePort;
import app.lexo.common.erro.RecursoNaoEncontradoException;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
class ConsultarPortalService implements ConsultarPortalQuery {

    private static final Logger log = LoggerFactory.getLogger(ConsultarPortalService.class);
    private static final Set<String> EM_ABERTO = Set.of("PENDENTE", "ATRASADO");

    private final CarregarClientePort carregarClientePort;
    private final CarregarProcessosDoClientePort carregarProcessosPort;
    private final CarregarHonorariosDoClientePort carregarHonorariosPort;
    private final Clock clock;

    ConsultarPortalService(CarregarClientePort carregarClientePort,
                           CarregarProcessosDoClientePort carregarProcessosPort,
                           CarregarHonorariosDoClientePort carregarHonorariosPort, Clock clock) {
        this.carregarClientePort = carregarClientePort;
        this.carregarProcessosPort = carregarProcessosPort;
        this.carregarHonorariosPort = carregarHonorariosPort;
        this.clock = clock;
    }

    @Override
    public PortalResponse consultar(String token) {
        Instant agora = clock.instant();
        Cliente cliente = carregarClientePort.porLinkDoPortal(token)
                .filter(c -> c.portalAtivoEm(agora))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link inválido ou expirado"));

        List<ProcessoPortal> processos = ouVazio(
                () -> carregarProcessosPort.carregar(cliente.escritorioId(), cliente.id()), "processos");
        List<HonorarioPortal> honorarios = ouVazio(
                () -> carregarHonorariosPort.carregar(cliente.escritorioId(), cliente.id()), "honorarios");

        BigDecimal emAberto = honorarios.stream()
                .filter(h -> EM_ABERTO.contains(h.status()))
                .map(HonorarioPortal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortalResponse(cliente.nome(), processos, honorarios,
                new ResumoPortal(processos.size(), emAberto));
    }

    private <T> List<T> ouVazio(Supplier<List<T>> chamada, String nome) {
        try {
            return chamada.get();
        } catch (Exception e) {
            log.warn("Portal: falha ao buscar {} ({}). Retornando vazio.", nome, e.toString());
            return List.of();
        }
    }
}
