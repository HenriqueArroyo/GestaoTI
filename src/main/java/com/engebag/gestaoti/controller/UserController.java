package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.RegisterRequestDTO;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
}