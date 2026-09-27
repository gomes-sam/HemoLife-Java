package com.hemolife.repository;

import com.hemolife.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    List<Unidade> findAllByOrderByNomeAsc();
}
