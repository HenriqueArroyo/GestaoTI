package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chamados")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String categoria;

    @Column(nullable = false, length = 50)
    private String empresa; // ENGEBAG ou BAG_CLEANER

    @Column(nullable = false, length = 20)
    private String criticidade = "BAIXA"; // BAIXA, MEDIA, ALTA, CRITICA

    @Column(nullable = false, length = 20)
    private String status = "ABERTO";

    @ManyToOne
    @JoinColumn(name = "id_usuario_abriu", nullable = false)
    private User usuarioAbriu;

    @ManyToOne
    @JoinColumn(name = "id_tecnico_atribuido")
    private User tecnicoPrincipal;

    @Column(name = "data_abertura", insertable = false, updatable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataFechamento;

    private Boolean slaCumprido;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCriticidade() {
        return criticidade;
    }

    public void setCriticidade(String criticidade) {
        this.criticidade = criticidade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUsuarioAbriu() {
        return usuarioAbriu;
    }

    public void setUsuarioAbriu(User usuarioAbriu) {
        this.usuarioAbriu = usuarioAbriu;
    }

    public User getTecnicoPrincipal() {
        return tecnicoPrincipal;
    }

    public void setTecnicoPrincipal(User tecnicoPrincipal) {
        this.tecnicoPrincipal = tecnicoPrincipal;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public Boolean getSlaCumprido() {
        return slaCumprido;
    }

    public void setSlaCumprido(Boolean slaCumprido) {
        this.slaCumprido = slaCumprido;
    }

    
}