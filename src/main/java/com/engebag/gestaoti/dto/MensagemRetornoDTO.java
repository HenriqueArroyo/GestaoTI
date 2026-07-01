package com.engebag.gestaoti.dto;

import java.time.LocalDateTime;

public class MensagemRetornoDTO {
    private Long id;
    private Long canalId;
    private String conteudo;
    private LocalDateTime enviadoEm;
    private UsuarioResumoDTO remetente;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCanalId() {
        return canalId;
    }

    public void setCanalId(Long canalId) {
        this.canalId = canalId;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public UsuarioResumoDTO getRemetente() {
        return remetente;
    }

    public void setRemetente(UsuarioResumoDTO remetente) {
        this.remetente = remetente;
    }
}