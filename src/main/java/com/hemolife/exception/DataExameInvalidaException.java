package com.hemolife.exception;

public class DataExameInvalidaException extends NegocioException {
    public DataExameInvalidaException() {
        super("A data do exame nao pode estar no passado.");
    }
}
