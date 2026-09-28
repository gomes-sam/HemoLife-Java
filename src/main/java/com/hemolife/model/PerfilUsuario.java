package com.hemolife.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PerfilUsuario {
    ADMIN("admin"),
    DOADOR("doador");

    private final String valor;

    PerfilUsuario(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    @Converter
    public static class Conversor implements AttributeConverter<PerfilUsuario, String> {
        @Override
        public String convertToDatabaseColumn(PerfilUsuario perfil) {
            return perfil == null ? null : perfil.valor;
        }

        @Override
        public PerfilUsuario convertToEntityAttribute(String valor) {
            if (valor == null) {
                return null;
            }
            for (PerfilUsuario perfil : values()) {
                if (perfil.valor.equals(valor)) {
                    return perfil;
                }
            }
            throw new IllegalArgumentException("Perfil de usuario desconhecido: " + valor);
        }
    }
}
