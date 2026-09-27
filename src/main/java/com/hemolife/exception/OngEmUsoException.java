package com.hemolife.exception;

public class OngEmUsoException extends NegocioException {
    public OngEmUsoException() {
        super("ONG possui inscricoes ou exames vinculados.");
    }
}
