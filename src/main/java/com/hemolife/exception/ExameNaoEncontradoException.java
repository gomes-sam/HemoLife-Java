package com.hemolife.exception;

public class ExameNaoEncontradoException extends NegocioException {
    public ExameNaoEncontradoException() {
        super("Exame nao encontrado.");
    }
}
