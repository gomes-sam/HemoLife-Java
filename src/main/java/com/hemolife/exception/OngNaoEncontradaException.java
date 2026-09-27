package com.hemolife.exception;

public class OngNaoEncontradaException extends NegocioException {
    public OngNaoEncontradaException() {
        super("ONG nao encontrada.");
    }
}
