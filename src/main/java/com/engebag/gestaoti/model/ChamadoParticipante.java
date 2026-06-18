package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chamado_participantes")
public class ChamadoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_chamado", nullable = false)
    private Chamado chamado;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(nullable = false, length = 50)
    private String papel; // Ex: 'SOLICITANTE_EXTRA' ou 'TECNICO_AUXILIAR'

    @Column(name = "data_adicao", insertable = false, updatable = false)
    private LocalDateTime dataAdicao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Chamado getChamado() { return chamado; }
    public void setChamado(Chamado chamado) { this.chamado = chamado; }
    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public LocalDateTime getDataAdicao() { return dataAdicao; }
    public void setDataAdicao(LocalDateTime dataAdicao) { this.dataAdicao = dataAdicao; }
}