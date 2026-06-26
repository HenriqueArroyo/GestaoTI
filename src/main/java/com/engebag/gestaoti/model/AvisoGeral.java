package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "avisos_gerais")
public class AvisoGeral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "url_imagem", length = 255)
    private String urlImagem;

    @Column(name = "empresa_alvo", nullable = false, length = 50)
    private String empresaAlvo = "AMBAS";

    @ManyToOne
    @JoinColumn(name = "id_usuario_criador")
    private User usuarioCriador;

    @Column(name = "data_criacao", insertable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public String getUrlImagem() { return urlImagem; }
    public void setUrlImagem(String urlImagem) { this.urlImagem = urlImagem; }
    public String getEmpresaAlvo() { return empresaAlvo; }
    public void setEmpresaAlvo(String empresaAlvo) { this.empresaAlvo = empresaAlvo; }
    public User getUsuarioCriador() { return usuarioCriador; }
    public void setUsuarioCriador(User usuarioCriador) { this.usuarioCriador = usuarioCriador; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }
}