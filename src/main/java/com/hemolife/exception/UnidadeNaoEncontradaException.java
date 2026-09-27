package com.hemolife.exception;

public class UnidadeNaoEncontradaException extends NegocioException {
    public UnidadeNaoEncontradaException() {
        super("Unidade nao encontrada.");
    }
}
