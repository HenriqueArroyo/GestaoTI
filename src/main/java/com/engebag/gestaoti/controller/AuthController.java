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

   // 4. AUTO-CADASTRO DE USUÁRIOS (Sign Up)
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarUsuarioPublico(@RequestBody com.engebag.gestaoti.dto.RegistroPublicoDTO data) {
        
        // 1. Verifica se o e-mail já existe
        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Este e-mail já está cadastrado no sistema.");
        }

        // 2. Valida o tamanho da senha
        if (data.senha() == null || data.senha().length() < 6) {
            return ResponseEntity.badRequest().body("Erro: A senha deve ter no mínimo 6 caracteres.");
        }

        // 3. VALIDAÇÃO DOS NOVOS CAMPOS OBRIGATÓRIOS (Cargo e Setor)
        if (data.cargo() == null || data.cargo().isBlank()) {
            return ResponseEntity.badRequest().body("Erro: O preenchimento do cargo é obrigatório.");
        }
        
        if (data.idDepartamento() == null) {
            return ResponseEntity.badRequest().body("Erro: O preenchimento do setor (departamento) é obrigatório.");
        }

        User newUser = new User();
        newUser.setNome(data.nome());
        newUser.setEmail(data.email());
        newUser.setSenha(passwordEncoder.encode(data.senha()));
        newUser.setEmpresaAcesso(data.empresaAcesso() != null ? data.empresaAcesso() : "AMBAS");
        
        // --- ATRIBUIÇÃO DOS NOVOS CAMPOS ---
        newUser.setCargo(data.cargo());
        newUser.setIdDepartamento(data.idDepartamento());
        
        // --- REGRAS DE SEGURANÇA FORÇADAS ---
        newUser.setRole("USER"); 
        newUser.setPrimeiroAcesso(false); 
        newUser.setUtilizaOmaxprensa(false); 
        newUser.setAtivo(true); 

        userRepository.save(newUser);

        return ResponseEntity.ok("Cadastro realizado com sucesso! Você já pode fazer login no sistema.");
    }



}