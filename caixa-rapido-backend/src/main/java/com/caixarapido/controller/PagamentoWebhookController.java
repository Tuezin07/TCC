package com.caixarapido.controller;

import com.caixarapido.model.Venda;
import com.caixarapido.repository.VendaRepository;
import com.caixarapido.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class PagamentoWebhookController {

    private final VendaRepository vendaRepository;
    private final MercadoPagoService mercadoPagoService;
    private final ObjectMapper mapper = new ObjectMapper();

    public PagamentoWebhookController(VendaRepository vendaRepository, MercadoPagoService mercadoPagoService) {
        this.vendaRepository = vendaRepository;
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> receberWebhook(@RequestBody String payload) {
        try {
            System.out.println("🔔 Webhook recebido: " + payload);

            JsonNode json = mapper.readTree(payload);
            String action = json.path("action").asText();
            JsonNode dataNode = json.path("data");

            if (!action.startsWith("payment")) {
                System.out.println("⚠️ Notificação ignorada: " + action);
                return ResponseEntity.ok().build();
            }

            // ID do pagamento - MANTÉM COMO STRING
            String paymentId = dataNode.path("id").asText();
            if (paymentId == null || paymentId.isEmpty()) {
                System.out.println("⚠️ Webhook sem paymentId");
                return ResponseEntity.ok().build();
            }

            System.out.println("💰 Payment ID: " + paymentId);

            // Consulta o pagamento no Mercado Pago - USA STRING
            String mpResponse = mercadoPagoService.consultarPagamento(paymentId);
            JsonNode mpJson = mapper.readTree(mpResponse);

            // STATUS ATUAL
            String status = mpJson.path("status").asText();
            System.out.println("ℹ️ Status recebido do MP: " + status);

            // Localiza a venda
            Optional<Venda> opt = vendaRepository.findByPagamentoId(paymentId);

            if (opt.isPresent()) {
                Venda venda = opt.get();
                venda.setStatus(status.toUpperCase());
                vendaRepository.save(venda);
                System.out.println("✅ Status atualizado para: " + status.toUpperCase());
            } else {
                System.out.println("❌ Venda não encontrada para paymentId " + paymentId);
            }

            return ResponseEntity.ok("ok");

        } catch (Exception e) {
            System.out.println("❌ ERRO no webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("erro no webhook: " + e.getMessage());
        }
    }

}
