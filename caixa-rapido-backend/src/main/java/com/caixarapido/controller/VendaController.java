package com.caixarapido.controller;

import com.caixarapido.model.Venda;
import com.caixarapido.repository.VendaRepository;
import com.caixarapido.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/vendas")
@CrossOrigin(origins = "*")
public class VendaController {

    private final MercadoPagoService mercadoPagoService;
    private final VendaRepository vendaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VendaController(MercadoPagoService mercadoPagoService, VendaRepository vendaRepository) {
        this.mercadoPagoService = mercadoPagoService;
        this.vendaRepository = vendaRepository;
    }

    // POST - cria pagamento PIX no Mercado Pago e salva venda PENDENTE
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("Payload recebido: " + payload);
            
            BigDecimal valor = new BigDecimal(payload.get("valorTotal").toString());
            String metodoPagamento = (String) payload.getOrDefault("metodoPagamento", "PIX");
            String descricao = "Pagamento via " + metodoPagamento;

            System.out.println("Criando pagamento PIX - Valor: " + valor + ", Descrição: " + descricao);

            // chama o service existente que já faz POST /v1/payments
            String respostaJson = mercadoPagoService.criarPagamentoPix(valor, descricao);

            System.out.println("Resposta do Mercado Pago: " + respostaJson);

            JsonNode jsonResponse = objectMapper.readTree(respostaJson);

            // Verifica se houve erro na resposta
            if (jsonResponse.has("error")) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Erro no Mercado Pago", 
                    "detalhes", jsonResponse.get("error").asText()
                ));
            }

            // Extrai dados relevantes do retorno do Mercado Pago
            String ticketUrl = jsonResponse.path("point_of_interaction")
                                          .path("transaction_data")
                                          .path("ticket_url")
                                          .asText();

            String qrBase64 = jsonResponse.path("point_of_interaction")
                                         .path("transaction_data")
                                         .path("qr_code_base64")
                                         .asText();

            String qrText = jsonResponse.path("point_of_interaction")
                                       .path("transaction_data")
                                       .path("qr_code")
                                       .asText();

            String paymentId = jsonResponse.path("id").asText();

            System.out.println("Payment ID: " + paymentId);
            System.out.println("QR Code disponível: " + (qrBase64 != null && !qrBase64.isEmpty()));

            if ((ticketUrl == null || ticketUrl.isEmpty()) && (qrBase64 == null || qrBase64.isEmpty())) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "URL/QR Code do pagamento não encontrada.",
                    "resposta_completa", respostaJson
                ));
            }

            // Salva venda PENDENTE no banco com pagamentoId
            Venda venda = new Venda();
            venda.setValorTotal(valor.doubleValue());
            venda.setMetodoPagamento(metodoPagamento);
            venda.setPagamentoId(paymentId);
            venda.setStatus("PENDENTE");
            venda.setQrCodeBase64(qrBase64);
            venda.setData(new Date());

            vendaRepository.save(venda);

            System.out.println("Venda salva com ID: " + venda.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pagamento criado e venda salva (PENDENTE)");
            response.put("urlPagamento", ticketUrl);
            response.put("qrCodeBase64", qrBase64);
            response.put("qrCodeTexto", qrText);
            response.put("paymentId", paymentId);
            response.put("id", venda.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao criar venda", 
                "detalhes", e.getMessage()
            ));
        }
    }

    // GET - consultar status armazenado no BD para um given paymentId
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> verificarStatus(@PathVariable String paymentId) {
        try {
            System.out.println("Consultando status para paymentId: " + paymentId);
            
            Optional<Venda> opt = vendaRepository.findByPagamentoId(paymentId);
            if (opt.isEmpty()) {
                System.out.println("Venda não encontrada para paymentId: " + paymentId);
                return ResponseEntity.status(404).body(Map.of("error", "Venda não encontrada"));
            }
            Venda v = opt.get();
            System.out.println("Status encontrado: " + v.getStatus());
            
            return ResponseEntity.ok(Map.of("status", v.getStatus()));
        } catch (Exception e) {
            System.err.println("Erro ao consultar status: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao consultar status", 
                "detalhes", e.getMessage()
            ));
        }
    }

    // GET - listar vendas (mantive pra compatibilidade)
    @GetMapping
    public ResponseEntity<List<Venda>> listarVendas() {
        List<Venda> vendas = vendaRepository.findAll();
        return ResponseEntity.ok(vendas);
    }

    // Health check para testar
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Caixa Rápido API");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }

    // Teste do banco
    @GetMapping("/test-db")
    public ResponseEntity<?> testDatabase() {
        try {
            Venda venda = new Venda();
            venda.setData(new Date());
            venda.setValorTotal(1.0);
            venda.setMetodoPagamento("TEST");
            venda.setStatus("TEST");
            venda.setPagamentoId("TEST_" + System.currentTimeMillis());
            
            Venda saved = vendaRepository.save(venda);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Conexão com banco OK!");
            response.put("vendaId", saved.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "Erro na conexão com o banco");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}