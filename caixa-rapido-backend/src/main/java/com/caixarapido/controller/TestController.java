package com.caixarapido.controller;

import com.caixarapido.model.Venda;
import com.caixarapido.service.VendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private VendaService vendaService;

    @GetMapping("/database")
    public ResponseEntity<?> testDatabase() {
        try {
            Venda venda = new Venda();
            venda.setData(new Date());
            venda.setValorTotal(1.0);
            venda.setMetodoPagamento("TEST");
            venda.setStatus("TEST");
            venda.setPagamentoId("TEST_" + System.currentTimeMillis());
            
            Venda saved = vendaService.salvarVenda(venda);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Conexão com banco OK!");
            response.put("vendaId", saved.getId());
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "Erro na conexão com o banco");
            error.put("details", e.getMessage());
            error.put("timestamp", new Date());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Caixa Rápido Backend");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}