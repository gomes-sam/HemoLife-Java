package com.hemolife.service;

import com.hemolife.exception.DadosInvalidosException;
import com.hemolife.exception.DadosObrigatoriosException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ValidacoesNegocio {
    private ValidacoesNegocio() {
    }

    static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) {
            throw new DadosObrigatoriosException(campo);
        }
        return valor;
    }

    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosObrigatoriosException(campo);
        }
        return valor.trim();
    }

    static String email(String valor) {
        return texto(valor, "email").toLowerCase(Locale.ROOT);
    }

    static String senha(String valor, boolean removerEspacosExternos) {
        texto(valor, "senha");
        String senha = removerEspacosExternos ? valor.trim() : valor;
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DadosInvalidosException("A senha deve ter no maximo 72 bytes em UTF-8.");
        }
        return senha;
    }
}
