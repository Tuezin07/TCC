package com.caixarapido.service;

import com.caixarapido.model.Venda;
import com.caixarapido.repository.VendaRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class VendaService {
    
    @Autowired
    private VendaRepository vendaRepository;

    public Venda salvarVenda(Venda venda) {
        try {
            return vendaRepository.save(venda);
        } catch (Exception e) {
            System.err.println("Erro ao salvar venda: " + e.getMessage());
            throw e;
        }
    }
}