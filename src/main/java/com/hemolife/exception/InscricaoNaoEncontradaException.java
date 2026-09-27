package com.hemolife.exception;

public class InscricaoNaoEncontradaException extends NegocioException {
    public InscricaoNaoEncontradaException() {
        super("Inscricao nao encontrada.");
    }
}
