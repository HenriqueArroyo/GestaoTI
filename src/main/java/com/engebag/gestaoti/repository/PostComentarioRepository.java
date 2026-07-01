package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.PostComentario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostComentarioRepository extends JpaRepository<PostComentario, Long> {

    /** Todos os comentários do aviso (raiz + respostas), ordenados por data. */
    List<PostComentario> findByAvisoIdOrderByCriadoEmAsc(Long avisoId);

    long countByAvisoId(Long avisoId);
}