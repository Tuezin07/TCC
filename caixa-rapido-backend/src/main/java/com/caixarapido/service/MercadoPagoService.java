package com.caixarapido.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
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

    // Construtor para injeção de dependência do RestTemplate (singleton)
    public MercadoPagoService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Cria um pagamento via PIX no MercadoPago.
     *
     * @param valor     Valor do pagamento.
     * @param descricao Descrição do pagamento.
     * @return Resposta do MercadoPago ou erro.
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
            requestBody.put("payer", Map.of("email", "comprador@email.com")); // Exemplo de pagador

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Envia a requisição
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Pagamento PIX criado com sucesso: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("Erro ao criar pagamento PIX. Status: {} - Response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Falha ao criar pagamento PIX. Detalhes: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento PIX", e);
            throw new RuntimeException("Erro interno ao processar pagamento PIX.", e);
        }
    }

    /**
     * Consulta um pagamento PIX no Mercado Pago pelo ID.
     *
     * @param paymentId ID do pagamento.
     * @return JSON da resposta do Mercado Pago.
     */
    public String consultarPagamento(Long paymentId) {
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

    /**
     * Cria uma preferência de pagamento para checkout do MercadoPago.
     *
     * @return Objeto Preference contendo os detalhes do pagamento.
     * @throws Exception Se ocorrer um erro na criação da preferência.
     */
    public Preference criarPreferenciaDePagamento() throws Exception {
        MercadoPagoConfig.setAccessToken(accessToken);
        PreferenceClient client = new PreferenceClient();

        PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                .id("1234")
                .title("Produto Teste")
                .description("Descrição do Produto")
                .pictureUrl("https://www.meusite.com/imagem.jpg")
                .categoryId("electronics")
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(new BigDecimal("100.00"))
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(java.util.Collections.singletonList(itemRequest))
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success("https://meusite.com/sucesso")
                        .failure("https://meusite.com/erro")
                        .pending("https://meusite.com/pendente")
                        .build())
                .autoReturn("approved")
                .build();

        return client.create(preferenceRequest);
    }
}
