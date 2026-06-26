package com.engebag.gestaoti.repository;
 
import com.engebag.gestaoti.model.PostCurtida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface PostCurtidaRepository extends JpaRepository<PostCurtida, Long> {
    long countByAvisoId(Long avisoId);
    boolean existsByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);
    Optional<PostCurtida> findByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);
}
 