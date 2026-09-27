package com.hemolife.exception;

public class UsuarioNaoInscritoException extends NegocioException {
    public UsuarioNaoInscritoException() {
        super("Usuario nao inscrito nesta ONG.");
    }
}
