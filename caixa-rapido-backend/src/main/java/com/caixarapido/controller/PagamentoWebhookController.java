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

    @PostMapping
    public ResponseEntity<?> receberWebhook(@RequestBody String payload) {
        try {
            JsonNode json = mapper.readTree(payload);

            String type = json.path("type").asText(); // geralmente "payment"
            if (!"payment".equals(type) && !"charge".equals(type)) {
                return ResponseEntity.ok().build();
            }

            // ID do pagamento enviado pelo Mercado Pago
            String paymentIdStr = json.path("data").path("id").asText();

            if (paymentIdStr == null || paymentIdStr.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            // 🔥 CONVERSÃO OBRIGATÓRIA
            Long paymentId = Long.parseLong(paymentIdStr);

            // Consulta no Mercado Pago
            String mpResponse = mercadoPagoService.consultarPagamento(paymentId);
            JsonNode mpJson = mapper.readTree(mpResponse);
            String status = mpJson.path("status").asText(); // approved, rejected, pending, etc.

            // Atualiza venda no BD
            Optional<Venda> opt = vendaRepository.findByPagamentoId(paymentIdStr);
            if (opt.isPresent()) {
                Venda venda = opt.get();
                venda.setStatus(status.toUpperCase());
                vendaRepository.save(venda);
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("erro");
        }
    }
}
