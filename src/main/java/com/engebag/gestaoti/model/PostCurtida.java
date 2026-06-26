package com.engebag.gestaoti.model;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
 
@Entity @Table(name = "post_curtidas")
public class PostCurtida {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aviso", nullable = false)
    private AvisoGeral aviso;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;
 
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
 
    public Long getId()                      { return id; }
    public AvisoGeral getAviso()             { return aviso; }
    public void setAviso(AvisoGeral aviso)   { this.aviso = aviso; }
    public User getUsuario()                 { return usuario; }
    public void setUsuario(User usuario)     { this.usuario = usuario; }
    public LocalDateTime getCriadoEm()       { return criadoEm; }
}