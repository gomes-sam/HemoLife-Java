package com.hemolife.exception;

public class DadosObrigatoriosException extends NegocioException {
    public DadosObrigatoriosException(String campo) {
        super("Campo obrigatorio: " + campo + ".");
    }
}
