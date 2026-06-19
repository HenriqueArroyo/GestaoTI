package com.engebag.gestaoti.controller;


import com.engebag.gestaoti.repository.UserRepository;
import com.engebag.gestaoti.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.engebag.gestaoti.service.EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    // 1. SOLICITAR CÓDIGO DE RECUPERAÇÃO
    @PostMapping("/esqueci-senha")
    public ResponseEntity<?> esqueciSenha(@RequestBody com.engebag.gestaoti.dto.EsqueciSenhaDTO data) {
        var userOpt = userRepository.findByEmail(data.email());
        
        if (userOpt.isEmpty()) {
            // Por segurança, não confirmamos se o e-mail existe ou não para evitar escaneamento de contas
            return ResponseEntity.ok("Se o e-mail existir em nossa base, um código de recuperação foi enviado.");
        }

        User user = userOpt.get();

        // Gera um código de 6 dígitos aleatório
        String codigo = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Define a validade para 15 minutos a partir de agora
        user.setCodigoRecuperacao(codigo);
        user.setValidadeCodigoRecuperacao(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Dispara o E-mail
        String mensagem = "Olá, " + user.getNome() + "!\n\n" +
                          "Você solicitou a redefinição da sua senha no sistema Gestão T.I.\n" +
                          "Seu código de verificação é: " + codigo + "\n\n" +
                          "Este código é válido por 15 minutos.\n" +
                          "Se você não solicitou isso, ignore este e-mail.";

        // Envolve em um try-catch caso as credenciais do Gmail no application.yml estejam erradas
        try {
            emailService.enviarEmailTexto(user.getEmail(), "Código de Recuperação - Gestão T.I.", mensagem);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao tentar enviar o e-mail. Verifique as configurações do servidor SMTP.");
        }

        return ResponseEntity.ok("Se o e-mail existir em nossa base, um código de recuperação foi enviado.");
    }

    // 2. CADASTRAR NOVA SENHA COM O CÓDIGO
    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(@RequestBody com.engebag.gestaoti.dto.RedefinirSenhaDTO data) {
        var userOpt = userRepository.findByEmail(data.email());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("E-mail ou código inválidos.");
        }

        User user = userOpt.get();

        // Verifica se tem um código ativo, se é igual ao digitado e se não venceu
        if (user.getCodigoRecuperacao() == null || 
            !user.getCodigoRecuperacao().equals(data.codigo()) || 
            user.getValidadeCodigoRecuperacao().isBefore(java.time.LocalDateTime.now())) {
            
            return ResponseEntity.badRequest().body("Código de recuperação inválido ou expirado.");
        }

        // Tudo certo! Criptografa a nova senha
        user.setSenha(passwordEncoder.encode(data.novaSenha()));
        
        // Limpa os campos de recuperação para o código não ser usado duas vezes
        user.setCodigoRecuperacao(null);
        user.setValidadeCodigoRecuperacao(null);
        
        userRepository.save(user);

        return ResponseEntity.ok("Sua senha foi redefinida com sucesso! Você já pode fazer login.");
    }

   
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