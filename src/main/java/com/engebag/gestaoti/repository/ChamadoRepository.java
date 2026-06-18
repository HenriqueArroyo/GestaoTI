package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    
    // Método mágico do Spring que faz "SELECT * FROM chamados WHERE empresa = ?"
    List<Chamado> findByEmpresa(String empresa);
}