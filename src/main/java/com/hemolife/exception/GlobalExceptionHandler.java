package com.hemolife.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, Object>>
    emailJaCadastrado(EmailJaCadastradoException exception) {

        return erro(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(PerfilInvalidoException.class)
    public ResponseEntity<Map<String, Object>>
    perfilInvalido(PerfilInvalidoException exception) {

        return erro(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(TipoSanguineoObrigatorioException.class)
    public ResponseEntity<Map<String, Object>>
    tipoSanguineoObrigatorio(
            TipoSanguineoObrigatorioException exception
    ) {

        return erro(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>>
    usuarioNaoEncontrado(
            UsuarioNaoEncontradoException exception
    ) {

        return erro(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    argumentoInvalido(
            IllegalArgumentException exception
    ) {

        return erro(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>> erro(
            HttpStatus status,
            String mensagem
    ) {

        Map<String, Object> resposta =
                new LinkedHashMap<>();

        resposta.put("success", false);
        resposta.put("message", mensagem);

        return ResponseEntity
                .status(status)
                .body(resposta);
    }
}