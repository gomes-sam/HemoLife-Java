package com.hemolife.repository;

import com.hemolife.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OngRepository extends JpaRepository<Ong, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    boolean existsByCnpjAndIdNot(
            String cnpj,
            Long id
    );

    Optional<Ong> findByEmailIgnoreCase(String email);

    Optional<Ong> findByEmail(String email);

    Optional<Ong> findByCnpj(String cnpj);

    List<Ong> findAllByOrderByNomeAsc();

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Ong o
               set o.nome = :nome,
                   o.email = :email,
                   o.cnpj = :cnpj
             where o.id = :id
            """)
    int atualizarDados(
            @Param("id") Long id,
            @Param("nome") String nome,
            @Param("email") String email,
            @Param("cnpj") String cnpj
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            delete from Ong o
             where o.id = :id
            """)
    int excluirPorId(
            @Param("id") Long id
    );
}