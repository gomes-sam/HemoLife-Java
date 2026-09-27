package com.hemolife.exception;

public class PerfilInvalidoException extends NegocioException {
    public PerfilInvalidoException() {
        super("Perfil invalido. Use ADMIN ou DOADOR.");
    }
}
