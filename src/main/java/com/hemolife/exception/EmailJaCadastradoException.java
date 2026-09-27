package com.hemolife.exception;

public class EmailJaCadastradoException extends NegocioException {
    public EmailJaCadastradoException() {
        super("Email ja cadastrado.");
    }
}
