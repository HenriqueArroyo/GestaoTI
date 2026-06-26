package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL quando for notificação geral (broadcast) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Column(nullable = false)
    private Boolean geral = false;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id")
    private Chamado chamado;

    @Column(nullable = false)
    private Boolean lida = false;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                      { return id; }
    public User getUsuario()                 { return usuario; }
    public void setUsuario(User usuario)     { this.usuario = usuario; }
    public Boolean getGeral()                { return geral; }
    public void setGeral(Boolean geral)      { this.geral = geral; }
    public String getTitulo()                { return titulo; }
    public void setTitulo(String titulo)     { this.titulo = titulo; }
    public String getMensagem()              { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public Chamado getChamado()              { return chamado; }
    public void setChamado(Chamado chamado)  { this.chamado = chamado; }
    public Boolean getLida()                 { return lida; }
    public void setLida(Boolean lida)        { this.lida = lida; }
    public LocalDateTime getCriadaEm()       { return criadaEm; }
    public void setCriadaEm(LocalDateTime v) { this.criadaEm = v; }
}