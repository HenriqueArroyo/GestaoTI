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
        // 1. Verifica se o e-mail já existe no banco
        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: E-mail já cadastrado no sistema!");
        }

        // 2. Cria o novo usuário
        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        
        // 3. A MÁGICA PROFISSIONAL: Criptografa a senha antes de salvar
        newUser.setSenha(passwordEncoder.encode(data.senha()));
        
        newUser.setRole(data.role());
 
        // Mapeia o acesso à empresa (ENGEBAG, BAG_CLEANER ou AMBAS)
        newUser.setEmpresaAcesso(data.empresaAcesso());

        newUser.setAtivo(true);
        // 4. Salva no banco de dados
        userRepository.save(newUser);

        return ResponseEntity.ok("Usuário " + data.nome() + " criado com sucesso!");
    }
}