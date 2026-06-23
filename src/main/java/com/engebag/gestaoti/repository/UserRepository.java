package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // NOVO: Busca apenas os funcionários com cadastro ativo
    List<User> findByAtivoTrue();
}