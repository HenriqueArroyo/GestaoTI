package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.ChamadoRequestDTO;
import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoRepository chamadoRepository;

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
    public ResponseEntity<?> assumirChamado(@PathVariable Long idChamado) {
        try {
            User usuarioLogado = getUsuarioLogado();

            // 1. Trava de Papel (Role): Usuário comum não pode assumir chamados
            if (usuarioLogado.getRole().equals("USER")) {
                return ResponseEntity.status(403).body("Erro: Apenas Técnicos ou Administradores podem assumir chamados.");
            }

            // 2. Verifica se o chamado existe
            var chamadoOpt = chamadoRepository.findById(idChamado);
            if (chamadoOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Erro: Chamado não encontrado.");
            }
            Chamado chamado = chamadoOpt.get();

            // Regra de Negócio: Impede assumir um chamado que já tem dono (a não ser que seja um ADMIN forçando a troca)
            if (chamado.getTecnicoPrincipal() != null && !usuarioLogado.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Erro: Este chamado já está atribuído ao técnico " + chamado.getTecnicoPrincipal().getNome());
            }

            // 3. Trava de Empresa: Verifica se o técnico tem acesso à empresa deste chamado
            if (!usuarioLogado.getEmpresaAcesso().equals("AMBAS") && 
                !usuarioLogado.getEmpresaAcesso().equals(chamado.getEmpresa())) {
                return ResponseEntity.status(403).body("Erro: Você não tem acesso aos chamados da " + chamado.getEmpresa());
            }

            // 4. Regra de Negócio: Assumir e mudar status
            chamado.setTecnicoPrincipal(usuarioLogado);
            
            if (chamado.getStatus().equals("ABERTO")) {
                chamado.setStatus("EM_ANDAMENTO");
            }

            chamadoRepository.save(chamado);

            return ResponseEntity.ok("Chamado assumido com sucesso! Você agora é o responsável e o status mudou para EM_ANDAMENTO.");

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
                        m.getDataEnvio() != null ? m.getDataEnvio().toString() : ""
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
}

