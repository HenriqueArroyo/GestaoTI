package com.engebag.gestaoti.repository;

import com.engebag.gestaoti.model.AvisoGeral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvisoGeralRepository extends JpaRepository<AvisoGeral, Long> {

    // Retorna avisos para as empresas solicitadas, ordenados do mais novo para o mais antigo.
    // Ignora avisos cuja data_expiracao já passou.
    @Query("SELECT a FROM AvisoGeral a WHERE a.empresaAlvo IN :empresas AND (a.dataExpiracao IS NULL OR a.dataExpiracao > CURRENT_TIMESTAMP) ORDER BY a.dataCriacao DESC")
    List<AvisoGeral> findAvisosAtivosParaEmpresas(@Param("empresas") List<String> empresas);
}