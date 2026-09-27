package com.hemolife.exception;

public class ConflitoAgendamentoException extends NegocioException {
    public ConflitoAgendamentoException() {
        super("Usuario ja possui exame nesta data e horario.");
    }
}
