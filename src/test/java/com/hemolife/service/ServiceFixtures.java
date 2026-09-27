package com.hemolife.service;

import com.hemolife.model.Exame;
import com.hemolife.model.Inscricao;
import com.hemolife.model.Ong;
import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.Unidade;
import com.hemolife.model.Usuario;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.SQLException;
import java.time.LocalDate;

final class ServiceFixtures {
    private ServiceFixtures() { }

    static <T> T comId(T entidade, long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    static Usuario usuario(long id, String nome) {
        return comId(new Usuario(nome, "usuario" + id + "@example.test", "hash-legado", "O+", PerfilUsuario.DOADOR), id);
    }

    static Ong ong(long id, String nome) {
        return comId(new Ong(nome, "ong" + id + "@example.test", "hash-legado", "12345678000199"), id);
    }

    static Unidade unidade(long id, String nome) {
        return comId(new Unidade(nome, "11999999999", "Rua de teste"), id);
    }

    static Inscricao inscricao() {
        return comId(new Inscricao(usuario(1, "Ana"), ong(2, "ONG")), 4);
    }

    static Exame exame(long id, LocalDate data, String horario) {
        return comId(Exame.agendar(usuario(1, "Ana"), ong(2, "ONG"), unidade(3, "Unidade"), data, horario), id);
    }

    static DataIntegrityViolationException constraint(String estado, String nome) {
        return new DataIntegrityViolationException("Falha simulada",
                new ConstraintViolationException("Constraint simulada", new SQLException("Conflito", estado), nome));
    }
}
