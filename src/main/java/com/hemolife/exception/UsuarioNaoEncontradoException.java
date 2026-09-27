package com.hemolife.exception;

public class UsuarioNaoEncontradoException extends NegocioException {
    public UsuarioNaoEncontradoException() {
        super("Usuario nao encontrado.");
    }
}
