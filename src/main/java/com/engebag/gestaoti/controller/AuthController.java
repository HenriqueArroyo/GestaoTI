package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.LoginRequestDTO;
import com.engebag.gestaoti.dto.LoginResponseDTO;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

   
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequestDTO data) {
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            var token = tokenService.generateToken((User) auth.getPrincipal());
            return ResponseEntity.ok(new LoginResponseDTO(token));

        } catch (Exception e) {
            // Isso vai imprimir no seu terminal exatamente O QUE deu errado (senha inválida, usuário inativo, etc)
            e.printStackTrace();
            return ResponseEntity.status(403).body("Falha no login: " + e.getMessage());
        }
    }


}