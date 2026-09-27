package com.hemolife.repository;

import com.hemolife.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OngRepository extends JpaRepository<Ong, Long> {
    Optional<Ong> findByEmail(String email);
    Optional<Ong> findByCnpj(String cnpj);
    boolean existsByEmail(String email);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByCnpjAndIdNot(String cnpj, Long id);
    List<Ong> findAllByOrderByNomeAsc();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ong o set o.nome = :nome, o.email = :email, o.cnpj = :cnpj where o.id = :id")
    int atualizarDados(@Param("id") Long id, @Param("nome") String nome,
                       @Param("email") String email, @Param("cnpj") String cnpj);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Ong o where o.id = :id")
    int excluirPorId(@Param("id") Long id);
}
