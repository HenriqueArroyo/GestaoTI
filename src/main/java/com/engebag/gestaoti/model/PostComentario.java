package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_comentarios")
public class PostComentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aviso", nullable = false)
    private AvisoGeral aviso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    /** Null = comentário raiz. Preenchido = resposta a outro comentário. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pai")
    private PostComentario pai;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "editado_em")
    private LocalDateTime editadoEm;

    public Long getId()                          { return id; }
    public AvisoGeral getAviso()                 { return aviso; }
    public void setAviso(AvisoGeral aviso)       { this.aviso = aviso; }
    public User getUsuario()                     { return usuario; }
    public void setUsuario(User usuario)         { this.usuario = usuario; }
    public String getConteudo()                  { return conteudo; }
    public void setConteudo(String conteudo)     { this.conteudo = conteudo; }
    public PostComentario getPai()               { return pai; }
    public void setPai(PostComentario pai)       { this.pai = pai; }
    public LocalDateTime getCriadoEm()           { return criadoEm; }
    public LocalDateTime getEditadoEm()          { return editadoEm; }
    public void setEditadoEm(LocalDateTime v)    { this.editadoEm = v; }
}