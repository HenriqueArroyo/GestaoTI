package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "canais_comunicacao")
public class CanalComunicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoCanal tipo;

    @ManyToMany
    @JoinTable(
        name = "canal_comunicacao_usuarios",
        joinColumns = @JoinColumn(name = "canal_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participantes = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoCanal getTipo() {
        return tipo;
    }

    public void setTipo(TipoCanal tipo) {
        this.tipo = tipo;
    }

    public Set<User> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(Set<User> participantes) {
        this.participantes = participantes;
    }
}