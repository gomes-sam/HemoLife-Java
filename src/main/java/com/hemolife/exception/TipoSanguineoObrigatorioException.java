package com.hemolife.exception;

public class TipoSanguineoObrigatorioException extends NegocioException {
    public TipoSanguineoObrigatorioException() {
        super("Tipo sanguineo obrigatorio para doador.");
    }
}
