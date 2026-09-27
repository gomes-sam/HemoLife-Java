package com.hemolife.repository;

import com.hemolife.model.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    boolean existsByUsuarioIdAndOngId(Long usuarioId, Long ongId);
    Optional<Inscricao> findByUsuarioIdAndOngId(Long usuarioId, Long ongId);
    List<Inscricao> findByUsuarioId(Long usuarioId);
    List<Inscricao> findByOngId(Long ongId);

    @EntityGraph(attributePaths = "usuario")
    List<Inscricao> findByOngIdOrderByUsuarioNomeAsc(Long ongId);
}
