// =============================================================================
// MODELO DE DOMÍNIO
// =============================================================================

/**
 * Representa um pedido no sistema de delivery.
 * @param id Identificador único do pedido
 * @param status Status atual do pedido (enum para type-safety)
 * @param itens Lista de itens do pedido
 */
data class Pedido(
    val id: String,
    val status: StatusPedido,
    val itens: List<String> = emptyList()
)

/**
 * Enum de status com type-safety, eliminando strings mágicas.
 */
enum class StatusPedido {
    NOVO,
    PROCESSADO,
    CANCELADO
}

// =============================================================================
// PADRÃO STRATEGY — Interface + implementações por status
// =============================================================================

/**
 * Contrato do Strategy: cada status tem sua própria estratégia de processamento.
 * SRP: cada classe faz uma única coisa.
 */
interface EstrategiaProcessamento {
    fun processar(pedido: Pedido)
}

/**
 * Strategy para pedidos novos: executa o fluxo de processamento real.
 * Responsabilidade única: processar um pedido novo.
 */
class EstrategiaPedidoNovo(
    private val logger: Logger,
    private val simuladorProcessamento: SimuladorProcessamento
) : EstrategiaProcessamento {

    override fun processar(pedido: Pedido) {
        logger.info("Pedido em processamento: ${pedido.id}")
        simuladorProcessamento.executar()
        logger.info("Pedido ${pedido.id} processado com sucesso.")
    }
}

/**
 * Strategy para pedidos já processados: apenas notifica e não reprocessa.
 */
class EstrategiaPedidoProcessado(
    private val logger: Logger
) : EstrategiaProcessamento {

    override fun processar(pedido: Pedido) {
        logger.aviso("Pedido já foi processado anteriormente: ${pedido.id}")
    }
}

/**
 * Strategy para pedidos cancelados.
 */
class EstrategiaPedidoCancelado(
    private val logger: Logger
) : EstrategiaProcessamento {

    override fun processar(pedido: Pedido) {
        logger.aviso("Pedido cancelado, nenhuma ação necessária: ${pedido.id}")
    }
}

// =============================================================================
// PADRÃO FACTORY — Criação das estratégias
// =============================================================================

/**
 * Factory que mapeia cada StatusPedido à sua estratégia correta.
 * Isola a lógica de criação; o PedidoProcessor não precisa saber
 * como cada estratégia é construída.
 */
class EstrategiaFactory(private val logger: Logger) {

    fun criar(status: StatusPedido): EstrategiaProcessamento = when (status) {
        StatusPedido.NOVO       -> EstrategiaPedidoNovo(logger, SimuladorProcessamento())
        StatusPedido.PROCESSADO -> EstrategiaPedidoProcessado(logger)
        StatusPedido.CANCELADO  -> EstrategiaPedidoCancelado(logger)
    }
}

// =============================================================================
// SIMULADOR DE PROCESSAMENTO (dependência isolada)
// =============================================================================

/**
 * Encapsula a simulação de latência, permitindo substituição fácil por
 * implementação real ou mock em testes.
 */
class SimuladorProcessamento(private val duracaoMs: Long = 2000L) {
    fun executar() = Thread.sleep(duracaoMs)
}

// =============================================================================
// LOGGER — abstração de output (SRP)
// =============================================================================

/**
 * Interface de log desacoplada, facilmente substituível por
 * qualquer framework (SLF4J, Logback, etc.).
 */
interface Logger {
    fun info(mensagem: String)
    fun aviso(mensagem: String)
    fun erro(mensagem: String)
}

class ConsoleLogger : Logger {
    override fun info(mensagem: String)   = println("[INFO]  $mensagem")
    override fun aviso(mensagem: String)  = println("[AVISO] $mensagem")
    override fun erro(mensagem: String)   = println("[ERRO]  $mensagem")
}

// =============================================================================
// PROCESSOR — orquestrador limpo (SRP + OCP)
// =============================================================================

/**
 * Orquestra o processamento delegando para a estratégia correta.
 *
 * Aberto para extensão (novos status → nova strategy),
 * fechado para modificação (este arquivo não muda).
 */
class PedidoProcessor(
    private val factory: EstrategiaFactory,
    private val logger: Logger
) {
    fun processarPedido(pedido: Pedido) {
        logger.info("Iniciando processamento do pedido: ${pedido.id}")
        val estrategia = factory.criar(pedido.status)
        estrategia.processar(pedido)
    }
}

// =============================================================================
// PONTO DE ENTRADA
// =============================================================================

fun main() {
    val logger   = ConsoleLogger()
    val factory  = EstrategiaFactory(logger)
    val processor = PedidoProcessor(factory, logger)

    val pedidos = listOf(
        Pedido(id = "001", status = StatusPedido.NOVO),
        Pedido(id = "002", status = StatusPedido.PROCESSADO),
        Pedido(id = "003", status = StatusPedido.CANCELADO)
    )

    pedidos.forEach { processor.processarPedido(it) }
}
