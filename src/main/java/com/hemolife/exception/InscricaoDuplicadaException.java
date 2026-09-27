package com.hemolife.exception;

public class InscricaoDuplicadaException extends NegocioException {
    public InscricaoDuplicadaException() {
        super("Usuario ja inscrito nesta ONG.");
    }
}
