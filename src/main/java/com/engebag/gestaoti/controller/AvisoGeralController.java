package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.AvisoRequestDTO;
import com.engebag.gestaoti.dto.AvisoResponseDTO;
import com.engebag.gestaoti.model.AvisoGeral;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.AvisoGeralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/avisos")
public class AvisoGeralController {

    @Autowired
    private AvisoGeralRepository avisoGeralRepository;

    @Autowired
    private com.engebag.gestaoti.repository.DepartamentoRepository departamentoRepository;

    // Utilitário para pegar o usuário logado via JWT
    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

   // 1. LISTAR FEED DE AVISOS
    @GetMapping
    public ResponseEntity<List<AvisoResponseDTO>> listarFeed() {
        User usuarioLogado = getUsuarioLogado();
        
        List<String> empresasPermitidas = new ArrayList<>();
        empresasPermitidas.add("AMBAS");
        
        if (!usuarioLogado.getEmpresaAcesso().equals("AMBAS")) {
            empresasPermitidas.add(usuarioLogado.getEmpresaAcesso());
        } else {
            empresasPermitidas.add("ENGEBAG");
            empresasPermitidas.add("BAG_CLEANER");
        }

        List<AvisoGeral> avisos = avisoGeralRepository.findAvisosAtivosParaEmpresas(empresasPermitidas);

        List<AvisoResponseDTO> resposta = avisos.stream().map(a -> {
            User criador = a.getUsuarioCriador();
            String nomeSetor = "";

            // Busca o nome do departamento se o usuário tiver um ID vinculado
            if (criador != null && criador.getIdDepartamento() != null) {
                nomeSetor = departamentoRepository.findById(criador.getIdDepartamento())
                        .map(d -> d.getNome())
                        .orElse("");
            }

            return new AvisoResponseDTO(
                    a.getId(),
                    a.getTitulo(),
                    a.getConteudo(),
                    a.getUrlImagem(),
                    a.getEmpresaAlvo(),
                    criador != null ? criador.getNome() : "Sistema",
                    criador != null && criador.getCargo() != null ? criador.getCargo() : "",
                    nomeSetor, // <--- Passando a variável que acabamos de buscar
                    criador != null ? criador.getFotoPerfil() : null,
                    a.getDataCriacao() != null ? a.getDataCriacao().toString() : LocalDateTime.now().toString()
            );
        }).toList();

        return ResponseEntity.ok(resposta);
    }

    // 2. CRIAR NOVO AVISO (Apenas TI e RH, por exemplo - ajuste conforme sua regra)
    @PostMapping
    public ResponseEntity<?> criarAviso(@RequestBody AvisoRequestDTO data) {
        try {
            User usuarioLogado = getUsuarioLogado();

            // Opcional: Trava para apenas ADMIN ou cargos específicos postarem
            // if (usuarioLogado.getRole().equals("USER")) {
            //     return ResponseEntity.status(403).body("Acesso negado: Apenas a equipe autorizada pode criar avisos globais.");
            // }

            AvisoGeral aviso = new AvisoGeral();
            aviso.setTitulo(data.titulo());
            aviso.setConteudo(data.conteudo());
            aviso.setUrlImagem(data.urlImagem()); // Neste primeiro momento, recebe a URL pronta
            aviso.setEmpresaAlvo(data.empresaAlvo() != null ? data.empresaAlvo() : "AMBAS");
            aviso.setDataExpiracao(data.dataExpiracao());
            aviso.setUsuarioCriador(usuarioLogado);

            AvisoGeral avisoSalvo = avisoGeralRepository.save(aviso);

            return ResponseEntity.ok(avisoSalvo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno no servidor ao criar aviso: " + e.getMessage());
        }
    }

    // 3. EXCLUIR AVISO
    @DeleteMapping("/{idAviso}")
    public ResponseEntity<?> excluirAviso(@PathVariable Long idAviso) {
        User usuarioLogado = getUsuarioLogado();
        
        var avisoOpt = avisoGeralRepository.findById(idAviso);
        if (avisoOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Erro: Aviso não encontrado.");
        }
        
        AvisoGeral aviso = avisoOpt.get();

        // Regra: Só o criador do aviso ou um ADMIN pode apagar
        boolean isCriador = aviso.getUsuarioCriador() != null && aviso.getUsuarioCriador().getId().equals(usuarioLogado.getId());
        boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");

        if (!isCriador && !isAdmin) {
            return ResponseEntity.status(403).body("Erro: Você não tem permissão para excluir este aviso.");
        }

        avisoGeralRepository.deleteById(idAviso);
        return ResponseEntity.ok("Aviso excluído com sucesso.");
    }
}