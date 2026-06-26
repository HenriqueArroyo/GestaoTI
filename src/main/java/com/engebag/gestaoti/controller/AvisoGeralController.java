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

    @Autowired
    private com.engebag.gestaoti.repository.UserRepository userRepository;

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
                    criador != null ? criador.getId() : null,
                    criador != null ? criador.getNome() : "Sistema",
                    criador != null && criador.getCargo() != null ? criador.getCargo() : "",
                    nomeSetor, // <--- Passando a variável que acabamos de buscar
                    criador != null ? criador.getFotoPerfil() : null,
                    a.getDataCriacao() != null ? a.getDataCriacao().toString() : LocalDateTime.now().toString()
            );
        }).toList();

        return ResponseEntity.ok(resposta);
    }

   // 2. CRIAR NOVO AVISO
    @PostMapping
    public ResponseEntity<?> criarAviso(@RequestBody AvisoRequestDTO data) {
        try {
            // Pega o usuário do JWT (desconectado do Hibernate)
            User usuarioAutenticado = getUsuarioLogado();

            // Re-busca o usuário no banco para atrelá-lo à sessão atual do Hibernate
            User usuarioLogado = userRepository.findById(usuarioAutenticado.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco."));

            AvisoGeral aviso = new AvisoGeral();
            aviso.setTitulo(data.titulo());
            aviso.setConteudo(data.conteudo());
            aviso.setUrlImagem(data.urlImagem()); 
            aviso.setEmpresaAlvo(data.empresaAlvo() != null ? data.empresaAlvo() : "AMBAS");
            aviso.setDataExpiracao(data.dataExpiracao());
            
            // Agora atrelamos a entidade gerenciada pelo banco
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

    // 4. UPLOAD DE IMAGEM PARA O AVISO
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImagemAviso(@RequestParam("arquivo") org.springframework.web.multipart.MultipartFile arquivo) {
        try {
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().body("Arquivo vazio.");
            }

            // Cria a pasta uploads/avisos se não existir
            java.nio.file.Path diretorioDestino = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads", "avisos");
            if (!java.nio.file.Files.exists(diretorioDestino)) {
                java.nio.file.Files.createDirectories(diretorioDestino);
            }

            // Gera nome único
            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = nomeOriginal != null && nomeOriginal.contains(".") 
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf(".")) : "";
            String nomeArquivoSalvo = java.util.UUID.randomUUID().toString() + extensao;

            // Salva o arquivo no disco
            java.nio.file.Path caminhoFinal = diretorioDestino.resolve(nomeArquivoSalvo);
            java.nio.file.Files.copy(arquivo.getInputStream(), caminhoFinal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Monta a URL de retorno (Ajuste a porta 7000 se o seu servidor usar outra)
            String urlArquivo = "http://localhost:7000/uploads/avisos/" + nomeArquivoSalvo;

            // Retorna um JSON simples com a URL {"url": "http://..."}
            return ResponseEntity.ok(java.util.Collections.singletonMap("url", urlArquivo));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno no servidor ao fazer upload da imagem: " + e.getMessage());
        }
    }
}