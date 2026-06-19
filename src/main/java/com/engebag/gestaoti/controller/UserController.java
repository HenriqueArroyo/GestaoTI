package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.RegisterRequestDTO;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/usuarios")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

   @PostMapping
    public ResponseEntity registrarUsuario(@RequestBody RegisterRequestDTO data) {
        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: E-mail já cadastrado no sistema!");
        }

        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        newUser.setSenha(passwordEncoder.encode(data.senha()));
        newUser.setRole(data.role());
        newUser.setEmpresaAcesso(data.empresaAcesso()); 
        
        // --- NOVOS CAMPOS ---
        newUser.setCargo(data.cargo());
        newUser.setIdDepartamento(data.idDepartamento());
        newUser.setUsuarioRm(data.usuarioRm());
        
        // Se vier nulo no JSON, define como false
        newUser.setUtilizaOmaxprensa(data.utilizaOmaxprensa() != null ? data.utilizaOmaxprensa() : false);
        
        // Todo novo usuário precisa trocar a senha no primeiro acesso
        newUser.setPrimeiroAcesso(true); 
        
        newUser.setAtivo(true);

        userRepository.save(newUser);

        return ResponseEntity.ok("Usuário " + data.nome() + " criado com sucesso!");
    }

    // 2. CONFIGURAR O PRIMEIRO ACESSO (Troca de senha obrigatória)
    @PutMapping("/me/configurar-primeiro-acesso")
    public ResponseEntity<?> configurarPrimeiroAcesso(@RequestBody com.engebag.gestaoti.dto.PrimeiroAcessoDTO data) {
        
        // Pega quem é o usuário que está fazendo a requisição pelo Token JWT
        User usuarioLogadoContexto = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Busca o usuário atualizado direto do banco de dados
        var userOpt = userRepository.findById(usuarioLogadoContexto.getId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Erro: Usuário não encontrado.");
        }

        User user = userOpt.get();

        // Trava de segurança: impede que alguém que já configurou acesse essa rota de novo
        if (user.getPrimeiroAcesso() == null || !user.getPrimeiroAcesso()) {
            return ResponseEntity.badRequest().body("Erro: O primeiro acesso deste usuário já foi configurado anteriormente.");
        }

        // Se a nova senha for nula ou muito curta
        if (data.novaSenha() == null || data.novaSenha().length() < 6) {
            return ResponseEntity.badRequest().body("Erro: A nova senha deve ter no mínimo 6 caracteres.");
        }

        // Criptografa a nova senha e muda a flag para false
        user.setSenha(passwordEncoder.encode(data.novaSenha()));
        user.setPrimeiroAcesso(false);
        
        userRepository.save(user);

        return ResponseEntity.ok("Sua senha foi configurada com sucesso! Bem-vindo(a) ao Gestão T.I.");
    }
}