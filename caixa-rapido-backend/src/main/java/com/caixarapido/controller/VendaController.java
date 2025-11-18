package com.caixarapido.controller;

import com.caixarapido.model.Venda;
import com.caixarapido.service.VendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vendas")
@CrossOrigin(origins = "*")
public class VendaController {

    @Autowired
    private VendaService vendaService;

    @PostMapping
    @Transactional
    public ResponseEntity<?> criarVenda(@RequestBody VendaRequest request) {
        try {
            System.out.println("Recebendo requisição para criar venda: " + request.getValorTotal());
            
            Venda venda = new Venda();
            venda.setData(new Date());
            venda.setValorTotal(request.getValorTotal());
            venda.setMetodoPagamento(request.getMetodoPagamento());
            venda.setStatus("PENDENTE");
            venda.setPagamentoId("PIX_" + System.currentTimeMillis());
            
            // QR Code simulado para teste
            venda.setQrCodeBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
            
            Venda vendaSalva = vendaService.salvarVenda(venda);
            System.out.println("Venda salva com ID: " + vendaSalva.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("id", vendaSalva.getId());
            response.put("paymentId", vendaSalva.getPagamentoId());
            response.put("status", vendaSalva.getStatus());
            response.put("qrCodeBase64", vendaSalva.getQrCodeBase64());
            response.put("valorTotal", vendaSalva.getValorTotal());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao criar venda");
            error.put("detalhes", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> consultarStatusPagamento(@PathVariable String paymentId) {
        try {
            System.out.println("Consultando status para: " + paymentId);
            
            // Simulação - sempre retorna pendente por enquanto
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("status", "PENDENTE");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Erro ao consultar status: " + e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao consultar status");
            error.put("detalhes", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    public static class VendaRequest {
        private double valorTotal;
        private String metodoPagamento;

        public double getValorTotal() { return valorTotal; }
        public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }
        
        public String getMetodoPagamento() { return metodoPagamento; }
        public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    }
}