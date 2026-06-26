package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.ChamadoRequestDTO;
import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.engebag.gestaoti.model.MensagemChamado;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.engebag.gestaoti.repository.UserRepository userRepository;

    @Autowired
    private com.engebag.gestaoti.repository.ChamadoParticipanteRepository participanteRepository;

    @Autowired
    private com.engebag.gestaoti.repository.MensagemChamadoRepository mensagemChamadoRepository;

    // 1. LISTAR CHAMADOS
    @GetMapping
    public ResponseEntity<List<Chamado>> listarChamados() {
        User usuarioLogado = getUsuarioLogado();
        if (usuarioLogado.getEmpresaAcesso().equals("AMBAS")) {
            return ResponseEntity.ok(chamadoRepository.findAll());
        } else {
            return ResponseEntity.ok(chamadoRepository.findByEmpresa(usuarioLogado.getEmpresaAcesso()));
        }
    }

    // 2. ABRIR NOVO CHAMADO
    @PostMapping
    public ResponseEntity<?> criarChamado(@RequestBody ChamadoRequestDTO data) {
        User usuarioLogado = getUsuarioLogado();

        if (!usuarioLogado.getEmpresaAcesso().equals("AMBAS") &&
            !usuarioLogado.getEmpresaAcesso().equals(data.empresa())) {
            return ResponseEntity.status(403).body("Erro: Você não tem permissão para abrir chamados na empresa " + data.empresa());
        }

        Chamado chamado = new Chamado();
        chamado.setTitulo(data.titulo());
        chamado.setDescricao(data.descricao());
        chamado.setCategoria(data.categoria());
        chamado.setCriticidade(data.criticidade() != null ? data.criticidade() : "BAIXA");
        chamado.setEmpresa(data.empresa());
        chamado.setUsuarioAbriu(usuarioLogado);

        chamadoRepository.save(chamado);

        // ── NOVO: Notifica todos em tempo real que um chamado foi criado ──────
        messagingTemplate.convertAndSend("/topic/chamados", chamado);

        return ResponseEntity.ok("Chamado criado com sucesso! ID: " + chamado.getId());
    }

    // 3. ADICIONAR PARTICIPANTE AO CHAMADO
    @PostMapping("/{idChamado}/participantes")
    public ResponseEntity<?> adicionarParticipante(@PathVariable Long idChamado, @RequestBody com.engebag.gestaoti.dto.AddParticipanteDTO data) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            if (usuarioLogado.getEmpresaAcesso() == null) {
                return ResponseEntity.status(403).body("Erro: O seu usuário está com a empresa_acesso nula no banco de dados.");
            }

            if (!usuarioLogado.getEmpresaAcesso().equals("AMBAS") &&
                !usuarioLogado.getEmpresaAcesso().equals(chamado.getEmpresa())) {
                return ResponseEntity.status(403).body("Erro: Sem permissão para alterar chamados da " + chamado.getEmpresa());
            }

            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());

            if (!isEquipeTi && !isCriador) {
                return ResponseEntity.status(403).body("Acesso Negado: Você só pode convidar participantes para chamados criados por você.");
            }

            var usuarioOpt = userRepository.findById(data.idUsuario());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: O usuário que você está tentando adicionar não existe.");
            }
            User novoParticipante = usuarioOpt.get();

            if (data.papel() == null) {
                return ResponseEntity.badRequest().body("Erro: O campo 'papel' não foi enviado no JSON.");
            }

            if (data.papel().equals("TECNICO_AUXILIAR") && novoParticipante.getRole().equals("USER")) {
                return ResponseEntity.badRequest().body("Erro: Um usuário comum não pode ser adicionado como Técnico Auxiliar.");
            }

            if (chamado.getUsuarioAbriu().getId().equals(novoParticipante.getId())) {
                return ResponseEntity.badRequest().body("Erro: O criador do chamado não precisa ser adicionado como participante.");
            }
            if (chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(novoParticipante.getId())) {
                return ResponseEntity.badRequest().body("Erro: O técnico responsável já faz parte do chamado.");
            }

            if (participanteRepository.existsByChamadoAndUsuario(chamado, novoParticipante)) {
                return ResponseEntity.badRequest().body("Aviso: O usuário " + novoParticipante.getNome() + " já participa deste chamado.");
            }

            com.engebag.gestaoti.model.ChamadoParticipante cp = new com.engebag.gestaoti.model.ChamadoParticipante();
            cp.setChamado(chamado);
            cp.setUsuario(novoParticipante);
            cp.setPapel(data.papel());
            participanteRepository.save(cp);

            // ── NOVO: Publica chamado atualizado para todos verem em tempo real ──
            Chamado chamadoAtualizado = chamadoRepository.findById(idChamado).get();
            messagingTemplate.convertAndSend("/topic/chamados", chamadoAtualizado);

            return ResponseEntity.ok("Usuário " + novoParticipante.getNome() + " adicionado ao chamado como " + data.papel());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 4. LISTAR PARTICIPANTES DO CHAMADO
    @GetMapping("/{idChamado}/participantes")
    public ResponseEntity<?> listarParticipantes(@PathVariable Long idChamado) {
        try {
            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }

            List<com.engebag.gestaoti.model.ChamadoParticipante> participantes = participanteRepository.findByChamado(chamadoOpt.get());

            var resposta = participantes.stream().map(p -> java.util.Map.of(
                "usuario", java.util.Map.of(
                    "id", p.getUsuario().getId(),
                    "nome", p.getUsuario().getNome(),
                    "email", p.getUsuario().getEmail()
                ),
                "papel", p.getPapel()
            )).toList();

            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor ao buscar participantes: " + e.getMessage());
        }
    }

    // 5. ASSUMIR CHAMADO
    @PutMapping("/{idChamado}/assumir")
    @Transactional
    public ResponseEntity<?> assumirChamado(@PathVariable Long idChamado) {
        try {
            User usuarioLogadoPrincipal = getUsuarioLogado();

            if (usuarioLogadoPrincipal.getRole().equals("USER")) {
                return ResponseEntity.status(403).body("Erro: Apenas Técnicos ou Administradores podem assumir chamados.");
            }

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            if (chamado.getTecnicoPrincipal() != null && !usuarioLogadoPrincipal.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Erro: Este chamado já está atribuído ao técnico " + chamado.getTecnicoPrincipal().getNome());
            }

            if (!usuarioLogadoPrincipal.getEmpresaAcesso().equals("AMBAS") &&
                !usuarioLogadoPrincipal.getEmpresaAcesso().equals(chamado.getEmpresa())) {
                return ResponseEntity.status(403).body("Erro: Você não tem acesso aos chamados da " + chamado.getEmpresa());
            }

            User usuarioLogado = userRepository.findById(usuarioLogadoPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado na base de dados."));

            chamado.setTecnicoPrincipal(usuarioLogado);
            if ("ABERTO".equals(chamado.getStatus())) {
                chamado.setStatus("EM_ANDAMENTO");
            }

            Chamado chamadoSalvo = chamadoRepository.saveAndFlush(chamado);

            // ── NOVO: Notifica todos que o chamado foi assumido ───────────────
            messagingTemplate.convertAndSend("/topic/chamados", chamadoSalvo);

            return ResponseEntity.ok(chamadoSalvo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // Método utilitário
    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 6. CARREGAR HISTÓRICO DO CHAT
    @GetMapping("/{idChamado}/mensagens")
    public ResponseEntity<?> carregarHistoricoChat(@PathVariable Long idChamado) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isTecnico = chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(usuarioLogado.getId());
            boolean isParticipante = participanteRepository.existsByChamadoAndUsuario(chamado, usuarioLogado);

            if (!isAdmin && !isCriador && !isTecnico && !isParticipante) {
                return ResponseEntity.status(403).body("Acesso Negado: Você não tem permissão para ler este chat.");
            }

            var mensagens = mensagemChamadoRepository.findByChamadoIdOrderByDataEnvioAsc(idChamado).stream()
                .map(m -> new com.engebag.gestaoti.dto.MensagemResponseDTO(
                        m.getId(),
                        m.getUsuario().getNome(),
                        m.getMensagem(),
                        m.getDataEnvio() != null ? m.getDataEnvio().toString() : "",
                        m.getTipoMensagem(),
                        m.getUrlArquivo(),
                        m.getNomeOriginalArquivo()
                )).toList();

            return ResponseEntity.ok(mensagens);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno ao carregar histórico: " + e.getMessage());
        }
    }

    // 7. ATUALIZAR / FECHAR CHAMADO
    @PutMapping("/{idChamado}")
    public ResponseEntity<?> atualizarChamado(
            @PathVariable Long idChamado,
            @RequestBody com.engebag.gestaoti.dto.AtualizarChamadoDTO data) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");
            boolean isTecnicoPrincipal = chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(usuarioLogado.getId());
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());

            if (!isAdmin && !isTecnicoPrincipal && !isCriador) {
                return ResponseEntity.status(403).body("Erro: Você não tem permissão para alterar este chamado.");
            }

            if (isAdmin) {
                if (data.empresa() != null) chamado.setEmpresa(data.empresa());
            }

            if (isAdmin || isTecnicoPrincipal) {
                if (data.categoria() != null) chamado.setCategoria(data.categoria());
                if (data.criticidade() != null) chamado.setCriticidade(data.criticidade());
                if (data.slaCumprido() != null) chamado.setSlaCumprido(data.slaCumprido());
            }

            if (data.status() != null && !data.status().equals(chamado.getStatus())) {
                if (!isAdmin && !isTecnicoPrincipal && isCriador && !data.status().equals("CANCELADO")) {
                    return ResponseEntity.status(403).body("Erro: Como usuário comum, você só possui permissão para mudar o status para 'CANCELADO'.");
                }
                chamado.setStatus(data.status());
                if (data.status().equals("RESOLVIDO") || data.status().equals("FECHADO") || data.status().equals("CANCELADO")) {
                    if (chamado.getDataFechamento() == null) {
                        chamado.setDataFechamento(java.time.LocalDateTime.now());
                    }
                } else {
                    chamado.setDataFechamento(null);
                }
            }

            if (data.descricao() != null) {
                chamado.setDescricao(data.descricao());
            }

            Chamado chamadoSalvo = chamadoRepository.save(chamado);

            // ── NOVO: Notifica todos sobre a atualização de status ────────────
            messagingTemplate.convertAndSend("/topic/chamados", chamadoSalvo);

            return ResponseEntity.ok("Chamado atualizado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 8. EXCLUIR CHAMADO
    @DeleteMapping("/{idChamado}")
    public ResponseEntity<?> excluirChamado(@PathVariable Long idChamado) {
        try {
            User usuarioLogado = getUsuarioLogado();

            if (!usuarioLogado.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Acesso Negado: Apenas Administradores possuem permissão para excluir chamados fisicamente do banco de dados.");
            }

            if (!chamadoRepository.existsById(idChamado)) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }

            chamadoRepository.deleteById(idChamado);

            // ── NOVO: Notifica todos que o chamado foi removido ───────────────
            messagingTemplate.convertAndSend("/topic/chamados", java.util.Map.of("deletedId", idChamado));

            return ResponseEntity.ok("Chamado excluído com sucesso e todo o seu histórico foi apagado.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 9. REMOVER PARTICIPANTE DO CHAMADO
    @DeleteMapping("/{idChamado}/participantes/{idUsuario}")
    public ResponseEntity<?> removerParticipante(@PathVariable Long idChamado, @PathVariable Long idUsuario) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            var usuarioOpt = userRepository.findById(idUsuario);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Usuário não encontrado no sistema.");
            }
            User usuarioRemover = usuarioOpt.get();

            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isProprioUsuario = usuarioLogado.getId().equals(idUsuario);

            if (!isEquipeTi && !isCriador && !isProprioUsuario) {
                return ResponseEntity.status(403).body("Acesso Negado: Você não tem permissão para gerenciar os participantes deste chamado.");
            }

            var relacaoOpt = participanteRepository.findByChamadoAndUsuario(chamado, usuarioRemover);
            if (relacaoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: O usuário selecionado não é um participante deste chamado.");
            }

            participanteRepository.delete(relacaoOpt.get());

            // ── NOVO: Publica chamado atualizado (sem o participante removido) ─
            Chamado chamadoAtualizado = chamadoRepository.findById(idChamado).get();
            messagingTemplate.convertAndSend("/topic/chamados", chamadoAtualizado);

            return ResponseEntity.ok("O usuário " + usuarioRemover.getNome() + " foi removido do chamado.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 10. ANEXAR ARQUIVO NO CHAMADO
    @PostMapping("/{idChamado}/anexos")
    public ResponseEntity<?> enviarAnexo(
            @PathVariable Long idChamado,
            @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isParticipante = participanteRepository.existsByChamadoAndUsuario(chamado, usuarioLogado);

            if (!isEquipeTi && !isCriador && !isParticipante) {
                return ResponseEntity.status(403).body("Erro: Você não tem permissão para enviar anexos neste chamado.");
            }

            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Nenhum arquivo enviado.");
            }

            Path diretorioDestino = Paths.get(System.getProperty("user.dir"), "uploads", "chamados");
            if (!Files.exists(diretorioDestino)) {
                Files.createDirectories(diretorioDestino);
            }

            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = nomeOriginal != null && nomeOriginal.contains(".")
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf(".")) : "";
            String nomeArquivoSalvo = UUID.randomUUID().toString() + extensao;

            Path caminhoFinal = diretorioDestino.resolve(nomeArquivoSalvo);
            Files.copy(arquivo.getInputStream(), caminhoFinal, StandardCopyOption.REPLACE_EXISTING);

            String urlArquivo = "http://localhost:7000/uploads/chamados/" + nomeArquivoSalvo;

            MensagemChamado mensagem = new MensagemChamado();
            mensagem.setChamado(chamado);
            mensagem.setUsuario(usuarioLogado);
            mensagem.setMensagem("Enviou um anexo");
            mensagem.setTipoMensagem("ARQUIVO");
            mensagem.setUrlArquivo(urlArquivo);
            mensagem.setNomeOriginalArquivo(nomeOriginal);

            mensagemChamadoRepository.save(mensagem);

            com.engebag.gestaoti.dto.MensagemResponseDTO dto = new com.engebag.gestaoti.dto.MensagemResponseDTO(
                    mensagem.getId(),
                    usuarioLogado.getNome(),
                    mensagem.getMensagem(),
                    java.time.LocalDateTime.now().toString(),
                    mensagem.getTipoMensagem(),
                    mensagem.getUrlArquivo(),
                    mensagem.getNomeOriginalArquivo()
            );

            messagingTemplate.convertAndSend("/topic/chamado/" + idChamado, dto);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao processar o upload: " + e.getMessage());
        }
    }
}