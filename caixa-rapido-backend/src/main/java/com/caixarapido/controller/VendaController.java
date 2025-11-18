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
            BigDecimal valor = new BigDecimal(payload.get("valorTotal").toString());
            String metodoPagamento = (String) payload.getOrDefault("metodoPagamento", "PIX");
            String descricao = "Pagamento via " + metodoPagamento;

            // chama o service existente que já faz POST /v1/payments
            String respostaJson = mercadoPagoService.criarPagamentoPix(valor, descricao);

            JsonNode jsonResponse = objectMapper.readTree(respostaJson);

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

            if ((ticketUrl == null || ticketUrl.isEmpty()) && (qrBase64 == null || qrBase64.isEmpty())) {
                return ResponseEntity.status(500).body(Map.of("error", "URL/QR Code do pagamento não encontrada."));
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

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pagamento criado e venda salva (PENDENTE)");
            response.put("urlPagamento", ticketUrl);
            response.put("qrCodeBase64", qrBase64);
            response.put("qrCodeTexto", qrText);
            response.put("paymentId", paymentId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao criar venda", "detalhes", e.getMessage()));
        }
    }

    // GET - consultar status armazenado no BD para um given paymentId
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> verificarStatus(@PathVariable String paymentId) {
        try {
            Optional<Venda> opt = vendaRepository.findByPagamentoId(paymentId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Venda não encontrada"));
            }
            Venda v = opt.get();
            return ResponseEntity.ok(Map.of("status", v.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao consultar status", "detalhes", e.getMessage()));
        }
    }

    // GET - listar vendas (mantive pra compatibilidade)
    @GetMapping
    public ResponseEntity<List<Venda>> listarVendas() {
        List<Venda> vendas = vendaRepository.findAll();
        return ResponseEntity.ok(vendas);
    }
}
