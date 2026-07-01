package com.engebag.gestaoti.controller;

import com.engebag.gestaoti.dto.AvisoRequestDTO;
import com.engebag.gestaoti.dto.AvisoResponseDTO;
import com.engebag.gestaoti.dto.ComentarioRequestDTO;
import com.engebag.gestaoti.dto.ComentarioResponseDTO;
import com.engebag.gestaoti.model.AvisoGeral;
import com.engebag.gestaoti.model.PostComentario;
import com.engebag.gestaoti.model.PostCurtida;
import com.engebag.gestaoti.model.PostFavorito;
import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.AvisoGeralRepository;
import com.engebag.gestaoti.repository.PostComentarioRepository;
import com.engebag.gestaoti.repository.PostCurtidaRepository;
import com.engebag.gestaoti.repository.PostFavoritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; // IMPORTANTE
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/avisos")
public class AvisoGeralController {

    @Autowired private AvisoGeralRepository avisoGeralRepository;
    @Autowired private com.engebag.gestaoti.repository.DepartamentoRepository departamentoRepository;
    @Autowired private com.engebag.gestaoti.repository.UserRepository userRepository;
    @Autowired private PostCurtidaRepository curtidaRepository;
    @Autowired private PostComentarioRepository comentarioRepository;
    @Autowired private PostFavoritoRepository favoritoRepository;

    // Injeção do template de WebSocket para enviar mensagens ao frontend
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private User getUsuarioLogado() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ── Utilitário para montar o DTO completo de um aviso ────────────────────
    private AvisoResponseDTO toDTO(AvisoGeral a, Long usuarioLogadoId) {
        User criador = a.getUsuarioCriador();
        String nomeSetor = "";
        if (criador != null && criador.getIdDepartamento() != null) {
            nomeSetor = departamentoRepository.findById(criador.getIdDepartamento())
                    .map(d -> d.getNome()).orElse("");
        }

        long totalCurtidas   = curtidaRepository.countByAvisoId(a.getId());
        long totalComentarios = comentarioRepository.countByAvisoId(a.getId());
        boolean euCurti      = curtidaRepository.existsByAvisoIdAndUsuarioId(a.getId(), usuarioLogadoId);
        boolean euFavoritei  = favoritoRepository.existsByAvisoIdAndUsuarioId(a.getId(), usuarioLogadoId);

        return new AvisoResponseDTO(
                a.getId(),
                a.getTitulo(),
                a.getConteudo(),
                a.getUrlImagem(),
                a.getUrlAnexo(),
                a.getEmpresaAlvo(),
                criador != null ? criador.getId() : null,
                criador != null ? criador.getNome() : "Sistema",
                criador != null && criador.getCargo() != null ? criador.getCargo() : "",
                nomeSetor,
                criador != null ? criador.getFotoPerfil() : null,
                a.getDataCriacao() != null ? a.getDataCriacao().toString() : LocalDateTime.now().toString(),
                a.getEditadoEm() != null ? a.getEditadoEm().toString() : null,
                a.getFixado(),
                totalCurtidas,
                totalComentarios,
                euCurti,
                euFavoritei
        );
    }

    // ── Helper: converte PostComentario → DTO ─────────────────────────────────
    private ComentarioResponseDTO toComentarioDTO(PostComentario c) {
        return new ComentarioResponseDTO(
                c.getId(),
                c.getUsuario().getId(),
                c.getUsuario().getNome(),
                c.getUsuario().getFotoPerfil(),
                c.getConteudo(),
                c.getPai() != null ? c.getPai().getId() : null,
                c.getCriadoEm().toString(),
                c.getEditadoEm() != null ? c.getEditadoEm().toString() : null
        );
    }

    // ── 1. LISTAR FEED ───────────────────────────────────────────────────────
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

        List<AvisoResponseDTO> resposta = avisos.stream()
                .sorted(Comparator.comparing(AvisoGeral::getFixado).reversed()
                        .thenComparing(Comparator.comparing(AvisoGeral::getDataCriacao).reversed()))
                .map(a -> toDTO(a, usuarioLogado.getId()))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    // ── 2. CRIAR AVISO ───────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> criarAviso(@RequestBody AvisoRequestDTO data) {
        try {
            User usuarioAutenticado = getUsuarioLogado();
            User usuarioLogado = userRepository.findById(usuarioAutenticado.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

            AvisoGeral aviso = new AvisoGeral();
            aviso.setTitulo(data.titulo());
            aviso.setConteudo(data.conteudo());
            aviso.setUrlImagem(data.urlImagem());
            aviso.setUrlAnexo(data.urlAnexo());
            aviso.setEmpresaAlvo(data.empresaAlvo() != null ? data.empresaAlvo() : "AMBAS");
            aviso.setDataExpiracao(data.dataExpiracao());
            aviso.setFixado(false);
            aviso.setUsuarioCriador(usuarioLogado);

            avisoGeralRepository.save(aviso);
            return ResponseEntity.ok(toDTO(aviso, usuarioLogado.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao criar aviso: " + e.getMessage());
        }
    }

    // ── 3. EDITAR AVISO ──────────────────────────────────────────────────────
    @PutMapping("/{idAviso}")
    public ResponseEntity<?> editarAviso(@PathVariable Long idAviso, @RequestBody AvisoRequestDTO data) {
        try {
            User usuarioLogado = getUsuarioLogado();
            AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                    .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

            boolean isCriador = aviso.getUsuarioCriador() != null
                    && aviso.getUsuarioCriador().getId().equals(usuarioLogado.getId());
            boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");

            if (!isCriador && !isAdmin) {
                return ResponseEntity.status(403).body("Sem permissão para editar este aviso.");
            }

            if (data.titulo()   != null) aviso.setTitulo(data.titulo());
            if (data.conteudo() != null) aviso.setConteudo(data.conteudo());
            if (data.urlImagem() != null) aviso.setUrlImagem(data.urlImagem());
            if (data.urlAnexo()  != null) aviso.setUrlAnexo(data.urlAnexo());
            aviso.setEditadoEm(LocalDateTime.now());

            avisoGeralRepository.save(aviso);
            return ResponseEntity.ok(toDTO(aviso, usuarioLogado.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao editar aviso: " + e.getMessage());
        }
    }

    // ── 4. EXCLUIR AVISO ─────────────────────────────────────────────────────
    @DeleteMapping("/{idAviso}")
    public ResponseEntity<?> excluirAviso(@PathVariable Long idAviso) {
        User usuarioLogado = getUsuarioLogado();
        AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

        boolean isCriador = aviso.getUsuarioCriador() != null
                && aviso.getUsuarioCriador().getId().equals(usuarioLogado.getId());
        boolean isAdmin = usuarioLogado.getRole().equals("ADMIN");

        if (!isCriador && !isAdmin) {
            return ResponseEntity.status(403).body("Sem permissão para excluir este aviso.");
        }

        avisoGeralRepository.deleteById(idAviso);

        // AVISA O FRONTEND QUE O POST FOI EXCLUÍDO
        messagingTemplate.convertAndSend("/topic/avisos", Map.of(
            "tipo", "EXCLUIDO",
            "payload", Map.of("id", idAviso)
        ));

        return ResponseEntity.ok("Aviso excluído com sucesso.");
    }

    // ── 5. FIXAR / DESAFIXAR ─────────────────────────────────────────────────
    @PatchMapping("/{idAviso}/fixar")
    public ResponseEntity<?> toggleFixar(@PathVariable Long idAviso) {
        User usuarioLogado = getUsuarioLogado();
        if (!usuarioLogado.getRole().equals("ADMIN")) {
            return ResponseEntity.status(403).body("Apenas administradores podem fixar posts.");
        }

        AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

        aviso.setFixado(!aviso.getFixado());
        avisoGeralRepository.save(aviso);
        return ResponseEntity.ok(Map.of("fixado", aviso.getFixado()));
    }

    // ── 6. CURTIR / DESCURTIR ────────────────────────────────────────────────
    @PostMapping("/{idAviso}/curtir")
    public ResponseEntity<?> toggleCurtir(@PathVariable Long idAviso) {
        User usuarioLogado = getUsuarioLogado();

        AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

        Optional<PostCurtida> curtidaExistente =
                curtidaRepository.findByAvisoIdAndUsuarioId(idAviso, usuarioLogado.getId());

        if (curtidaExistente.isPresent()) {
            curtidaRepository.delete(curtidaExistente.get());
        } else {
            User u = userRepository.findById(usuarioLogado.getId()).orElseThrow();
            PostCurtida curtida = new PostCurtida();
            curtida.setAviso(aviso);
            curtida.setUsuario(u);
            curtidaRepository.save(curtida);
        }

        long total = curtidaRepository.countByAvisoId(idAviso);
        boolean euCurti = curtidaRepository.existsByAvisoIdAndUsuarioId(idAviso, usuarioLogado.getId());
        return ResponseEntity.ok(Map.of("totalCurtidas", total, "euCurti", euCurti));
    }

    // ── 7. FAVORITAR / DESFAVORITAR ──────────────────────────────────────────
    @PostMapping("/{idAviso}/favoritar")
    public ResponseEntity<?> toggleFavoritar(@PathVariable Long idAviso) {
        User usuarioLogado = getUsuarioLogado();

        AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

        Optional<PostFavorito> favExistente =
                favoritoRepository.findByAvisoIdAndUsuarioId(idAviso, usuarioLogado.getId());

        if (favExistente.isPresent()) {
            favoritoRepository.delete(favExistente.get());
        } else {
            User u = userRepository.findById(usuarioLogado.getId()).orElseThrow();
            PostFavorito fav = new PostFavorito();
            fav.setAviso(aviso);
            fav.setUsuario(u);
            favoritoRepository.save(fav);
        }

        boolean euFavoritei = favoritoRepository.existsByAvisoIdAndUsuarioId(idAviso, usuarioLogado.getId());
        return ResponseEntity.ok(Map.of("euFavoritei", euFavoritei));
    }

    // ── 8. LISTAR COMENTÁRIOS (raiz + respostas em lista plana) ───────────────
    // O frontend monta a árvore localmente usando idPai.
    @GetMapping("/{idAviso}/comentarios")
    public ResponseEntity<?> listarComentarios(@PathVariable Long idAviso) {
        List<PostComentario> comentarios =
                comentarioRepository.findByAvisoIdOrderByCriadoEmAsc(idAviso);
        return ResponseEntity.ok(
                comentarios.stream().map(this::toComentarioDTO).toList()
        );
    }

    // ── 9. CRIAR COMENTÁRIO (raiz ou resposta) ────────────────────────────────
    @PostMapping("/{idAviso}/comentarios")
    public ResponseEntity<?> criarComentario(@PathVariable Long idAviso,
                                              @RequestBody ComentarioRequestDTO data) {
        try {
            User usuarioAutenticado = getUsuarioLogado();
            User usuario = userRepository.findById(usuarioAutenticado.getId()).orElseThrow();
            AvisoGeral aviso = avisoGeralRepository.findById(idAviso)
                    .orElseThrow(() -> new RuntimeException("Aviso não encontrado."));

            PostComentario comentario = new PostComentario();
            comentario.setAviso(aviso);
            comentario.setUsuario(usuario);
            comentario.setConteudo(data.conteudo());

            // Resolve o comentário pai, se informado
            if (data.idPai() != null) {
                PostComentario pai = comentarioRepository.findById(data.idPai())
                        .orElseThrow(() -> new RuntimeException("Comentário pai não encontrado."));
                comentario.setPai(pai);
            }

            comentarioRepository.save(comentario);
            return ResponseEntity.ok(toComentarioDTO(comentario));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao criar comentário: " + e.getMessage());
        }
    }

    // ── 10. EXCLUIR COMENTÁRIO ────────────────────────────────────────────────
    @DeleteMapping("/{idAviso}/comentarios/{idComentario}")
    public ResponseEntity<?> excluirComentario(@PathVariable Long idAviso,
                                                @PathVariable Long idComentario) {
        User usuarioLogado = getUsuarioLogado();
        PostComentario comentario = comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado."));

        boolean isCriador = comentario.getUsuario().getId().equals(usuarioLogado.getId());
        boolean isAdmin   = usuarioLogado.getRole().equals("ADMIN");

        if (!isCriador && !isAdmin) {
            return ResponseEntity.status(403).body("Sem permissão para excluir este comentário.");
        }

        // Ao excluir um comentário raiz, o ON DELETE CASCADE do banco remove as respostas.
        comentarioRepository.deleteById(idComentario);
        return ResponseEntity.ok("Comentário excluído.");
    }

    // ── 11. UPLOAD DE IMAGEM ─────────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImagem(@RequestParam("arquivo") MultipartFile arquivo) {
        return processarUpload(arquivo, "avisos");
    }

    // ── 12. UPLOAD DE ANEXO ──────────────────────────────────────────────────
    @PostMapping("/upload-anexo")
    public ResponseEntity<?> uploadAnexo(@RequestParam("arquivo") MultipartFile arquivo) {
        return processarUpload(arquivo, "avisos-anexos");
    }

    private ResponseEntity<?> processarUpload(MultipartFile arquivo, String subpasta) {
        try {
            if (arquivo.isEmpty()) return ResponseEntity.badRequest().body("Arquivo vazio.");

            Path dir = Paths.get(System.getProperty("user.dir"), "uploads", subpasta);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String nomeOriginal = arquivo.getOriginalFilename();
            String ext = nomeOriginal != null && nomeOriginal.contains(".")
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf(".")) : "";
            String nomeSalvo = UUID.randomUUID() + ext;

            Files.copy(arquivo.getInputStream(), dir.resolve(nomeSalvo), StandardCopyOption.REPLACE_EXISTING);

            String url = "http://localhost:7000/uploads/" + subpasta + "/" + nomeSalvo;
            return ResponseEntity.ok(Map.of("url", url, "nomeOriginal", nomeOriginal != null ? nomeOriginal : nomeSalvo));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro no upload: " + e.getMessage());
        }
    }
}