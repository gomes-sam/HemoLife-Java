package com.hemolife.repository;

import com.hemolife.model.Exame;
import com.hemolife.model.StatusExame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ExameRepository extends JpaRepository<Exame, Long> {
    Optional<Exame> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndDataExameAndHorario(Long usuarioId, LocalDate dataExame, LocalTime horario);
    List<Exame> findByUsuarioIdOrderByDataExameAscHorarioAsc(Long usuarioId);
    List<Exame> findByOngIdAndStatus(Long ongId, StatusExame status);
    List<Exame> findByUnidadeIdAndDataExame(Long unidadeId, LocalDate dataExame);
}
