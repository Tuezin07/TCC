package com.caixarapido.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    @Value("${mercadopago.access.token}")
    private String accessToken;

    private static final String API_URL = "https://api.mercadopago.com/v1/payments";

    private final RestTemplate restTemplate;

    public MercadoPagoService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Cria um pagamento via PIX no MercadoPago.
     */
    public String criarPagamentoPix(BigDecimal valor, String descricao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        }

        try {
            // Configuração do cabeçalho
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            // Gerando um ID único para evitar duplicação de pagamentos
            headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

            // Montagem do JSON corretamente
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transaction_amount", valor);
            requestBody.put("payment_method_id", "pix");
            requestBody.put("description", descricao);
            requestBody.put("payer", Map.of(
                "email", "cliente@caixarapido.com",
                "first_name", "Cliente", 
                "last_name", "Caixa Rápido"
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Envia a requisição
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("Pagamento PIX criado com sucesso: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("Erro ao criar pagamento PIX. Status: {} - Response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Falha ao criar pagamento PIX. Status: " + response.getStatusCode() + " - Detalhes: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento PIX", e);
            throw new RuntimeException("Erro interno ao processar pagamento PIX: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta um pagamento PIX no Mercado Pago pelo ID.
     * AGORA ACEITA STRING E LONG
     */
    public String consultarPagamento(String paymentId) {
        try {
            String url = API_URL + "/" + paymentId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            log.info("Consulta de pagamento PIX: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Erro ao consultar pagamento PIX: {}", e.getMessage());
            throw new RuntimeException("Erro ao consultar pagamento PIX.");
        }
    }

    // Overload para manter compatibilidade
    public String consultarPagamento(Long paymentId) {
        return consultarPagamento(paymentId.toString());
    }
}