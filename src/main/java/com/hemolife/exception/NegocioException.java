package com.hemolife.exception;

public abstract class NegocioException extends RuntimeException {
    protected NegocioException(String mensagem) {
        super(mensagem);
    }
}
