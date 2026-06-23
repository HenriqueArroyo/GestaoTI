package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.Chamado;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
            List<Chamado> findByEmpresa(String empresa);
}



