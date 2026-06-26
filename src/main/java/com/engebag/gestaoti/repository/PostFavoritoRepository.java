package com.engebag.gestaoti.repository;
 
import com.engebag.gestaoti.model.PostFavorito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface PostFavoritoRepository extends JpaRepository<PostFavorito, Long> {
    boolean existsByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);
    Optional<PostFavorito> findByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);
}