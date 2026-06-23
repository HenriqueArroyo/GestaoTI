package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.RegisterRequestDTO;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.engebag.gestaoti.service.EmailService emailService;

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

    // Método utilitário para pegar o usuário do Token JWT
    private User getUsuarioLogadoContexto() {
        return (User) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 3. VISUALIZAR O PRÓPRIO PERFIL
    @GetMapping("/me")
    public ResponseEntity<com.engebag.gestaoti.dto.UsuarioResponseDTO> getMeuPerfil() {
        User usuarioLogado = getUsuarioLogadoContexto();
        
        // Busca do banco para garantir que estamos enviando os dados mais recentes
        var userOpt = userRepository.findById(usuarioLogado.getId());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Retorna os dados blindados usando o nosso novo DTO
        return ResponseEntity.ok(new com.engebag.gestaoti.dto.UsuarioResponseDTO(userOpt.get()));
    }

  // 4. ATUALIZAR O PRÓPRIO PERFIL
    @PutMapping("/me")
    public ResponseEntity<?> atualizarMeuPerfil(@RequestBody com.engebag.gestaoti.dto.AtualizarPerfilDTO data) {
        User usuarioLogado = getUsuarioLogadoContexto();
        
        var userOpt = userRepository.findById(usuarioLogado.getId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuário não encontrado.");
        }

        User user = userOpt.get();


        if (data.email() != null && !data.email().isBlank() && !data.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(data.email()).isPresent()) {
                return ResponseEntity.badRequest().body("Erro: O e-mail informado já está em uso por outra conta.");
            }
            user.setEmail(data.email());
        }

        if (data.senha() != null && !data.senha().isBlank()) {
            if (data.senha().length() < 6) {
                return ResponseEntity.badRequest().body("Erro: A nova senha deve ter no mínimo 6 caracteres.");
            }
            user.setSenha(passwordEncoder.encode(data.senha()));
        }

        if (data.usuarioRm() != null) {
            user.setUsuarioRm(data.usuarioRm());
        }
        
        if (data.utilizaOmaxprensa() != null) {
            user.setUtilizaOmaxprensa(data.utilizaOmaxprensa());
        }

        if (data.fotoPerfil() != null) {
            user.setFotoPerfil(data.fotoPerfil());
        }

        userRepository.save(user);

        return ResponseEntity.ok(new com.engebag.gestaoti.dto.UsuarioResponseDTO(user));
    }

    // ==============================================================================
    // BLOCO D: GESTÃO DA EQUIPE (Apenas ADMIN e TECNICO)
    // ==============================================================================

    // 5. LISTAR TODOS OS USUÁRIOS
    @GetMapping
    public ResponseEntity<java.util.List<com.engebag.gestaoti.dto.UsuarioResponseDTO>> listarUsuarios() {
        // Converte a lista de entidades User para a lista de DTOs blindados
        var usuarios = userRepository.findAll().stream()
                .map(com.engebag.gestaoti.dto.UsuarioResponseDTO::new)
                .toList();
        
        return ResponseEntity.ok(usuarios);
    }

    // 6. EDITAR QUALQUER USUÁRIO
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarUsuarioPeloAdmin(
            @PathVariable Long id, 
            @RequestBody com.engebag.gestaoti.dto.AdminAtualizarUsuarioDTO data) {
        
        User tiLogado = getUsuarioLogadoContexto();
        
        var targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Erro: Usuário não encontrado.");
        }
        
        User targetUser = targetOpt.get();

        // REGRA DE OURO: Técnico não edita Administrador
        if (tiLogado.getRole().equals("TECNICO") && targetUser.getRole().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Acesso Negado: Técnicos não possuem permissão para alterar dados de Administradores.");
        }

        // Atualiza os campos se eles foram enviados no JSON
        if (data.nome() != null) targetUser.setNome(data.nome());
        
        if (data.email() != null && !data.email().isBlank() && !data.email().equals(targetUser.getEmail())) {
            if (userRepository.findByEmail(data.email()).isPresent()) {
                return ResponseEntity.badRequest().body("Erro: O e-mail informado já está em uso.");
            }
            targetUser.setEmail(data.email());
        }

        if (data.cargo() != null) targetUser.setCargo(data.cargo());
        if (data.role() != null) targetUser.setRole(data.role());
        if (data.empresaAcesso() != null) targetUser.setEmpresaAcesso(data.empresaAcesso());
        if (data.idDepartamento() != null) targetUser.setIdDepartamento(data.idDepartamento());
        if (data.ativo() != null) targetUser.setAtivo(data.ativo());

        userRepository.save(targetUser);

        return ResponseEntity.ok(new com.engebag.gestaoti.dto.UsuarioResponseDTO(targetUser));
    }

    // 7. FORÇAR REDEFINIÇÃO DE SENHA
    @PostMapping("/{id}/forcar-redefinicao")
    public ResponseEntity<?> forcarRedefinicaoSenha(@PathVariable Long id) {
        User tiLogado = getUsuarioLogadoContexto();
        
        var targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Erro: Usuário não encontrado.");
        }
        
        User targetUser = targetOpt.get();

        // REGRA DE OURO: Técnico não reseta senha de Administrador
        if (tiLogado.getRole().equals("TECNICO") && targetUser.getRole().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Acesso Negado: Técnicos não possuem permissão para redefinir senhas de Administradores.");
        }

        // Gera uma senha temporária forte (Ex: Gestao@4912)
        String senhaTemporaria = "Gestao@" + String.format("%04d", new java.util.Random().nextInt(10000));
        
        // Aplica a punição (obriga o usuário a passar pela tela de Primeiro Acesso novamente)
        targetUser.setSenha(passwordEncoder.encode(senhaTemporaria));
        targetUser.setPrimeiroAcesso(true);
        userRepository.save(targetUser);

        // Dispara o E-mail de Aviso
        String mensagem = "Olá, " + targetUser.getNome() + ".\n\n" +
                          "A sua senha do portal Gestão T.I. foi redefinida pela nossa equipe.\n" +
                          "Sua nova senha temporária é: " + senhaTemporaria + "\n\n" +
                          "No seu próximo login, o sistema exigirá que você cadastre uma nova senha pessoal definitiva.";
        try {
            emailService.enviarEmailTexto(targetUser.getEmail(), "Sua senha foi redefinida - Gestão T.I.", mensagem);
        } catch (Exception e) {
            return ResponseEntity.ok("Senha alterada para: " + senhaTemporaria + " (Aviso: Falha ao enviar o e-mail).");
        }

        return ResponseEntity.ok("Senha redefinida com sucesso. A senha temporária (" + senhaTemporaria + ") foi enviada ao e-mail do usuário.");
    }

    // 8. UPLOAD DE FOTO DE PERFIL
    @PostMapping("/me/foto")
    public ResponseEntity<?> uploadFotoPerfil(@RequestParam("foto") MultipartFile arquivo) {
        User usuarioLogado = getUsuarioLogadoContexto();

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Nenhum arquivo enviado.");
        }

        try {
            // 1. Cria a pasta "uploads/perfis" se ela não existir
            Path diretorioDestino = Paths.get(System.getProperty("user.dir"), "uploads", "perfis");
            if (!Files.exists(diretorioDestino)) {
                Files.createDirectories(diretorioDestino);
            }

            // 2. Gera um nome único para o arquivo (evita que um usuário sobrescreva a foto do outro)
            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
            String nomeArquivoSalvo = UUID.randomUUID().toString() + extensao;

            // 3. Salva o arquivo fisicamente na pasta
            Path caminhoFinal = diretorioDestino.resolve(nomeArquivoSalvo);
            Files.copy(arquivo.getInputStream(), caminhoFinal, StandardCopyOption.REPLACE_EXISTING);

            // 4. Salva a URL no banco de dados do usuário
            var userOpt = userRepository.findById(usuarioLogado.getId());
            User user = userOpt.get();
            
            // A URL que o frontend vai usar para exibir a imagem
            String urlImagem = "http://localhost:7000/uploads/perfis/" + nomeArquivoSalvo;
            user.setFotoPerfil(urlImagem);
            userRepository.save(user);

            return ResponseEntity.ok(new com.engebag.gestaoti.dto.UsuarioResponseDTO(user));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno ao tentar salvar a imagem.");
        }
    }

    // 9. LISTAR PARTICIPANTES PARA CHAMADOS (Visível para qualquer usuário logado)
    @GetMapping("/participantes")
    public ResponseEntity<java.util.List<com.engebag.gestaoti.dto.UsuarioResumoDTO>> listarParticipantes() {
        
        // Busca apenas os usuários ativos no banco
        var participantesAtivos = userRepository.findByAtivoTrue().stream()
                .map(com.engebag.gestaoti.dto.UsuarioResumoDTO::new)
                .toList();
        
        return ResponseEntity.ok(participantesAtivos);
    }
}