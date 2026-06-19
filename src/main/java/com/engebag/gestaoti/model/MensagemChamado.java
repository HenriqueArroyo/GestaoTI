package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_chamado")
public class MensagemChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_chamado", nullable = false)
    private Chamado chamado;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "tipo_mensagem", nullable = false, length = 20)
    private String tipoMensagem = "TEXTO"; // TEXTO ou ARQUIVO

    @Column(name = "data_envio", insertable = false, updatable = false)
    private LocalDateTime dataEnvio;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Chamado getChamado() { return chamado; }
    public void setChamado(Chamado chamado) { this.chamado = chamado; }
    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public String getTipoMensagem() { return tipoMensagem; }
    public void setTipoMensagem(String tipoMensagem) { this.tipoMensagem = tipoMensagem; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }
}