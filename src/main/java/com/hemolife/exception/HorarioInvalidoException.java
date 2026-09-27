package com.hemolife.exception;

public class HorarioInvalidoException extends NegocioException {
    public HorarioInvalidoException() {
        super("Horario invalido. Use HH:mm.");
    }
}
