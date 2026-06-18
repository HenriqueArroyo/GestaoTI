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
    
    // Método utilitário para pegar o usuário logado atual através do JWT
    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}