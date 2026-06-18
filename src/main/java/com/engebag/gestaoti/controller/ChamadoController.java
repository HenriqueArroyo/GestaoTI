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

    // Método utilitário para pegar o usuário logado atual através do JWT
    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}