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

    // 1. LISTAR CHAMADOS (Filtra de acordo com a empresa do usuário)
    @GetMapping
    public ResponseEntity<List<Chamado>> listarChamados() {
        User usuarioLogado = getUsuarioLogado();

        if (usuarioLogado.getEmpresaAcesso().equals("AMBAS")) {
            // Técnicos e Admins veem tudo
            return ResponseEntity.ok(chamadoRepository.findAll());
        } else {
            // Usuários comuns veem apenas os da sua empresa
            return ResponseEntity.ok(chamadoRepository.findByEmpresa(usuarioLogado.getEmpresaAcesso()));
        }
    }

    // 2. ABRIR NOVO CHAMADO
    @PostMapping
    public ResponseEntity<?> criarChamado(@RequestBody ChamadoRequestDTO data) {
        User usuarioLogado = getUsuarioLogado();

        // VALIDAÇÃO: O usuário só pode abrir chamado para a própria empresa ou se for AMBAS
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

        return ResponseEntity.ok("Chamado criado com sucesso! ID: " + chamado.getId());
    }

    // 3. ADICIONAR PARTICIPANTE AO CHAMADO
   @PostMapping("/{idChamado}/participantes")
    public ResponseEntity<?> adicionarParticipante(@PathVariable Long idChamado, @RequestBody com.engebag.gestaoti.dto.AddParticipanteDTO data) {
        try {
            User usuarioLogado = getUsuarioLogado();

            // 1. Verifica se o chamado existe
            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            // 2. Trava de Empresa
            if (usuarioLogado.getEmpresaAcesso() == null) {
                return ResponseEntity.status(403).body("Erro: O seu usuário está com a empresa_acesso nula no banco de dados.");
            }

            if (!usuarioLogado.getEmpresaAcesso().equals("AMBAS") && 
                !usuarioLogado.getEmpresaAcesso().equals(chamado.getEmpresa())) {
                return ResponseEntity.status(403).body("Erro: Sem permissão para alterar chamados da " + chamado.getEmpresa());
            }

            // 2.5 Trava de Autoria (Regra: Comum só altera o próprio chamado, T.I. altera qualquer um)
            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());

            if (!isEquipeTi && !isCriador) {
                return ResponseEntity.status(403).body("Acesso Negado: Você só pode convidar participantes para chamados criados por você.");
            }

            // 3. Busca o usuário que vai ser convidado
            var usuarioOpt = userRepository.findById(data.idUsuario());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: O usuário que você está tentando adicionar não existe.");
            }
            User novoParticipante = usuarioOpt.get();

            // Prevenção de NullPointer no JSON enviado
            if (data.papel() == null) {
                return ResponseEntity.badRequest().body("Erro: O campo 'papel' não foi enviado no JSON.");
            }

            // 4. Regra de Negócio: Impede colocar um "Usuário Comum" como Técnico Auxiliar
            if (data.papel().equals("TECNICO_AUXILIAR") && novoParticipante.getRole().equals("USER")) {
                return ResponseEntity.badRequest().body("Erro: Um usuário comum não pode ser adicionado como Técnico Auxiliar.");
            }

            // Regra de Negócio: O técnico principal e o criador do chamado não podem ser adicionados como participantes
            if (chamado.getUsuarioAbriu().getId().equals(novoParticipante.getId())) {
                return ResponseEntity.badRequest().body("Erro: O criador do chamado não precisa ser adicionado como participante.");
            }
            if (chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(novoParticipante.getId())) {
                return ResponseEntity.badRequest().body("Erro: O técnico responsável já faz parte do chamado.");
            }

            // 5. Verifica duplicidade
            if (participanteRepository.existsByChamadoAndUsuario(chamado, novoParticipante)) {
                return ResponseEntity.badRequest().body("Aviso: O usuário " + novoParticipante.getNome() + " já participa deste chamado.");
            }

            // 6. Salva o vínculo no banco
            com.engebag.gestaoti.model.ChamadoParticipante cp = new com.engebag.gestaoti.model.ChamadoParticipante();
            cp.setChamado(chamado);
            cp.setUsuario(novoParticipante);
            cp.setPapel(data.papel());
            
            participanteRepository.save(cp);

            return ResponseEntity.ok("Usuário " + novoParticipante.getNome() + " adicionado ao chamado como " + data.papel());

        } catch (Exception e) {
            e.printStackTrace(); // Imprime o rastro do erro no console do VSCode / Terminal
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 4. ASSUMIR CHAMADO (Apenas para Técnicos ou Admins)
@PutMapping("/{idChamado}/assumir")
@Transactional
public ResponseEntity<?> assumirChamado(@PathVariable Long idChamado) {
    try {
        User usuarioLogadoPrincipal = getUsuarioLogado();

        // 1. Trava de Papel (Role): Usuário comum não pode assumir chamados
        if (usuarioLogadoPrincipal.getRole().equals("USER")) {
            return ResponseEntity.status(403).body("Erro: Apenas Técnicos ou Administradores podem assumir chamados.");
        }

        // 2. Verifica se o chamado existe
        var chamadoOpt = chamadoRepository.findById(idChamado);
        if (chamadoOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
        }
        Chamado chamado = chamadoOpt.get();

        // Regra de Negócio: Impede assumir um chamado que já tem dono (a não ser que seja um ADMIN forçando a troca)
        if (chamado.getTecnicoPrincipal() != null && !usuarioLogadoPrincipal.getRole().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Erro: Este chamado já está atribuído ao técnico " + chamado.getTecnicoPrincipal().getNome());
        }

        // 3. Trava de Empresa: Verifica se o técnico tem acesso à empresa deste chamado
        if (!usuarioLogadoPrincipal.getEmpresaAcesso().equals("AMBAS") && 
            !usuarioLogadoPrincipal.getEmpresaAcesso().equals(chamado.getEmpresa())) {
            return ResponseEntity.status(403).body("Erro: Você não tem acesso aos chamados da " + chamado.getEmpresa());
        }

        // Buscar o usuário pelo banco para assegurar que ele é uma "Managed Entity"
        User usuarioLogado = userRepository.findById(usuarioLogadoPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado na base de dados."));

        // 4. Regra de Negócio: Assumir e mudar status
        chamado.setTecnicoPrincipal(usuarioLogado);

        if ("ABERTO".equals(chamado.getStatus())) {
            chamado.setStatus("EM_ANDAMENTO");
        }

        // Força a escrita (flush) imediata das alterações no banco de dados
        Chamado chamadoSalvo = chamadoRepository.saveAndFlush(chamado);

        return ResponseEntity.ok(chamadoSalvo);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
    }
}

    // Método utilitário para pegar o usuário logado atual através do JWT
    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ROTA PARA O FRONTEND CARREGAR O HISTÓRICO QUANDO ABRIR A TELA
    @GetMapping("/{idChamado}/mensagens")
    public ResponseEntity<?> carregarHistoricoChat(@PathVariable Long idChamado) {
        try {
            User usuarioLogado = getUsuarioLogado();

            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            // Validação de segurança para ler o chat
            boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isTecnico = chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(usuarioLogado.getId());
            boolean isParticipante = participanteRepository.existsByChamadoAndUsuario(chamado, usuarioLogado);

            if (!isAdmin && !isCriador && !isTecnico && !isParticipante) {
                return ResponseEntity.status(403).body("Acesso Negado: Você não tem permissão para ler este chat.");
            }

         // Busca as mensagens usando o seu repository
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

    // 7. ATUALIZAR INFORMAÇÕES E FECHAR CHAMADO
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

            // Identificação de papéis no ticket
            boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");
            boolean isTecnicoPrincipal = chamado.getTecnicoPrincipal() != null && chamado.getTecnicoPrincipal().getId().equals(usuarioLogado.getId());
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());

            // Se o usuário não tem nenhuma relação com o ticket (e não é Admin), bloqueia
            if (!isAdmin && !isTecnicoPrincipal && !isCriador) {
                return ResponseEntity.status(403).body("Erro: Você não tem permissão para alterar este chamado.");
            }

            // --- REGRA DE NEGÓCIO: SUPER PODERES DO ADMIN ---
            if (isAdmin) {
                if (data.empresa() != null) chamado.setEmpresa(data.empresa());
            }

            // --- REGRA DE NEGÓCIO: PODERES DA T.I. (Admin e Técnico) ---
            if (isAdmin || isTecnicoPrincipal) {
                if (data.categoria() != null) chamado.setCategoria(data.categoria());
                if (data.criticidade() != null) chamado.setCriticidade(data.criticidade());
                if (data.slaCumprido() != null) chamado.setSlaCumprido(data.slaCumprido());
            }

            // --- REGRA DE STATUS E FECHAMENTO ---
            if (data.status() != null && !data.status().equals(chamado.getStatus())) {
                
                // Trava: Usuário comum (criador) só pode alterar o status se for para CANCELAR o ticket dele
                if (!isAdmin && !isTecnicoPrincipal && isCriador && !data.status().equals("CANCELADO")) {
                     return ResponseEntity.status(403).body("Erro: Como usuário comum, você só possui permissão para mudar o status para 'CANCELADO'.");
                }

                chamado.setStatus(data.status());

                // Se o status for de encerramento, crava a data final
                if (data.status().equals("RESOLVIDO") || data.status().equals("FECHADO") || data.status().equals("CANCELADO")) {
                    if (chamado.getDataFechamento() == null) {
                        chamado.setDataFechamento(java.time.LocalDateTime.now());
                    }
                } else {
                    // Se o chamado for reaberto, limpamos a data de fechamento
                    chamado.setDataFechamento(null);
                }
            }

            // Descrição: Qualquer um com acesso ao ticket pode atualizar/complementar a descrição
            if (data.descricao() != null) {
                chamado.setDescricao(data.descricao());
            }

            chamadoRepository.save(chamado);
            return ResponseEntity.ok("Chamado atualizado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro Interno no Servidor: " + e.getMessage());
        }
    }

    // 8. EXCLUIR CHAMADO (Apenas para o ADMIN)
    @DeleteMapping("/{idChamado}")
    public ResponseEntity<?> excluirChamado(@PathVariable Long idChamado) {
        try {
            User usuarioLogado = getUsuarioLogado();

            // Regra de Negócio: Apenas Admin pode deletar registros físicos
            if (!usuarioLogado.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Acesso Negado: Apenas Administradores possuem permissão para excluir chamados fisicamente do banco de dados.");
            }

            if (!chamadoRepository.existsById(idChamado)) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }

            // Graças ao ON DELETE CASCADE no seu Flyway, as mensagens e participantes atrelados serão apagados automaticamente!
            chamadoRepository.deleteById(idChamado);
            
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

            // Identificação de papéis para permissão
            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isProprioUsuario = usuarioLogado.getId().equals(idUsuario); // Permite que a pessoa "saia" do chamado

            if (!isEquipeTi && !isCriador && !isProprioUsuario) {
                return ResponseEntity.status(403).body("Acesso Negado: Você não tem permissão para gerenciar os participantes deste chamado.");
            }

            // Busca a relação do participante com o chamado
            var relacaoOpt = participanteRepository.existsByChamadoAndUsuario(chamado, usuarioRemover);
            if (relacaoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: O usuário selecionado não é um participante deste chamado.");
            }

            // Remove o vínculo
            participanteRepository.delete(relacaoOpt.get());
            
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

            // Validação de segurança: apenas quem tem acesso ao chamado pode enviar arquivos
            boolean isEquipeTi = usuarioLogado.getRole().equals("ADMIN") || usuarioLogado.getRole().equals("TECNICO");
            boolean isCriador = chamado.getUsuarioAbriu().getId().equals(usuarioLogado.getId());
            boolean isParticipante = participanteRepository.existsByChamadoAndUsuario(chamado, usuarioLogado);

            if (!isEquipeTi && !isCriador && !isParticipante) {
                return ResponseEntity.status(403).body("Erro: Você não tem permissão para enviar anexos neste chamado.");
            }

            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: Nenhum arquivo enviado.");
            }

            // 1. Cria a pasta uploads/chamados se ela não existir
            Path diretorioDestino = Paths.get(System.getProperty("user.dir"), "uploads", "chamados");
            if (!Files.exists(diretorioDestino)) {
                Files.createDirectories(diretorioDestino);
            }

            // 2. Gera um nome único para o arquivo
            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = nomeOriginal != null && nomeOriginal.contains(".") 
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf(".")) : "";
            String nomeArquivoSalvo = UUID.randomUUID().toString() + extensao;

            // 3. Salva no disco rígido
            Path caminhoFinal = diretorioDestino.resolve(nomeArquivoSalvo);
            Files.copy(arquivo.getInputStream(), caminhoFinal, StandardCopyOption.REPLACE_EXISTING);

            // 4. Monta a URL pública que o frontend vai usar para exibir/baixar
            String urlArquivo = "http://localhost:7000/uploads/chamados/" + nomeArquivoSalvo;

            // 5. Salva o registro da mensagem no banco de dados
            MensagemChamado mensagem = new MensagemChamado();
            mensagem.setChamado(chamado);
            mensagem.setUsuario(usuarioLogado);
            mensagem.setMensagem("Enviou um anexo"); // Texto padrão
            mensagem.setTipoMensagem("ARQUIVO");
            mensagem.setUrlArquivo(urlArquivo);
            mensagem.setNomeOriginalArquivo(nomeOriginal);
            
            mensagemChamadoRepository.save(mensagem);

            // 6. Monta o DTO que será enviado aos clientes
            com.engebag.gestaoti.dto.MensagemResponseDTO dto = new com.engebag.gestaoti.dto.MensagemResponseDTO(
                    mensagem.getId(),
                    usuarioLogado.getNome(),
                    mensagem.getMensagem(),
                    java.time.LocalDateTime.now().toString(),
                    mensagem.getTipoMensagem(),
                    mensagem.getUrlArquivo(),
                    mensagem.getNomeOriginalArquivo()
            );

            // MÁGICA: Dispara o anexo pelo WebSocket para aparecer na tela de quem estiver olhando o chat!
            messagingTemplate.convertAndSend("/topic/chamado/" + idChamado, dto);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao processar o upload: " + e.getMessage());
        }
    }
}

