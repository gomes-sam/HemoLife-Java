package com.hemolife.exception;

/** Falha de negocio; a futura camada HTTP definira sua representacao e status. */
public abstract class NegocioException extends RuntimeException {
    protected NegocioException(String mensagem) {
        super(mensagem);
    }
}
