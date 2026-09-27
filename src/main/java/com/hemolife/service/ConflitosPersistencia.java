package com.hemolife.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Arrays;

/** Traduz somente constraints conhecidas; falhas inesperadas continuam sendo propagadas. */
final class ConflitosPersistencia {
    private ConflitosPersistencia() {
    }

    static boolean violou(DataIntegrityViolationException erro, String sqlState, String... constraints) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && sqlState.equals(violacao.getSQLState())
                    && Arrays.asList(constraints).contains(violacao.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
