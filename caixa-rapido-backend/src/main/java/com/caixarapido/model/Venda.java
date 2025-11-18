package com.caixarapido.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date data;

    private double valorTotal;

    private String metodoPagamento;

    private String pagamentoId;

    private String status;

    @Column(columnDefinition = "TEXT") // corrigido para PostgreSQL
    private String qrCodeBase64;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getData() { return data; }
    public void setData(Date data) { this.data = data; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public String getPagamentoId() { return pagamentoId; }
    public void setPagamentoId(String pagamentoId) { this.pagamentoId = pagamentoId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }
}
