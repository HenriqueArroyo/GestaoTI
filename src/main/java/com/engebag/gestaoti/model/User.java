package com.engebag.gestaoti.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(length = 100)
    private String cargo;

    @Column(nullable = false, length = 20)
    private String role; 

    @Column(name = "empresa_acesso", nullable = false, length = 50)
    private String empresaAcesso;

    @Column(name = "id_departamento")
    private Long idDepartamento;

    @Column(name = "usuario_rm", length = 100)
    private String usuarioRm;

    @Column(name = "utiliza_omaxprensa")
    private Boolean utilizaOmaxprensa;

    @Column(name = "foto_perfil", length = 255)
    private String fotoPerfil;

    @Column(name = "primeiro_acesso")
    private Boolean primeiroAcesso;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "data_cadastro", insertable = false, updatable = false)
    private LocalDateTime dataCadastro;

    private Boolean ativo;

    // --- VARIÁVEIS DE RECUPERAÇÃO DE SENHA FALTANTES ---
    @Column(name = "codigo_recuperacao", length = 6)
    private String codigoRecuperacao;

    @Column(name = "validade_codigo_recuperacao")
    private LocalDateTime validadeCodigoRecuperacao;

    // --- Métodos Obrigatórios do UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getPassword() { return this.senha; }

    @Override
    public String getUsername() { return this.email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { 
        return this.ativo != null && this.ativo; 
    }

    // --- GETTERS E SETTERS PADRÃO ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmpresaAcesso() { return empresaAcesso; }
    public void setEmpresaAcesso(String empresaAcesso) { this.empresaAcesso = empresaAcesso; }

    public Long getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Long idDepartamento) { this.idDepartamento = idDepartamento; }

    public String getUsuarioRm() { return usuarioRm; }
    public void setUsuarioRm(String usuarioRm) { this.usuarioRm = usuarioRm; }

    public Boolean getUtilizaOmaxprensa() { return utilizaOmaxprensa; }
    public void setUtilizaOmaxprensa(Boolean utilizaOmaxprensa) { this.utilizaOmaxprensa = utilizaOmaxprensa; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public Boolean getPrimeiroAcesso() { return primeiroAcesso; }
    public void setPrimeiroAcesso(Boolean primeiroAcesso) { this.primeiroAcesso = primeiroAcesso; }

    public LocalDateTime getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(LocalDateTime ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    // --- GETTERS E SETTERS DE RECUPERAÇÃO DE SENHA ---

    public String getCodigoRecuperacao() { return codigoRecuperacao; }
    public void setCodigoRecuperacao(String codigoRecuperacao) { this.codigoRecuperacao = codigoRecuperacao; }

    public LocalDateTime getValidadeCodigoRecuperacao() { return validadeCodigoRecuperacao; }
    public void setValidadeCodigoRecuperacao(LocalDateTime validadeCodigoRecuperacao) { this.validadeCodigoRecuperacao = validadeCodigoRecuperacao; }
}