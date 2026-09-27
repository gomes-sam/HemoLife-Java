package com.hemolife.exception;

public class CnpjJaCadastradoException extends NegocioException {
    public CnpjJaCadastradoException() {
        super("CNPJ ja cadastrado.");
    }
}
