package com.hemolife.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler({
            EmailJaCadastradoException.class,
            CnpjJaCadastradoException.class,
            InscricaoDuplicadaException.class,
            ConflitoAgendamentoException.class,
            OngEmUsoException.class
    })
    public ResponseEntity<Map<String, Object>> conflito(
            RuntimeException exception
    ) {
        return erro(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }


    @ExceptionHandler({
            UsuarioNaoEncontradoException.class,
            OngNaoEncontradaException.class,
            UnidadeNaoEncontradaException.class,
            ExameNaoEncontradoException.class,
            InscricaoNaoEncontradaException.class
    })
    public ResponseEntity<Map<String, Object>> naoEncontrado(
            RuntimeException exception
    ) {
        return erro(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }


    @ExceptionHandler({
            PerfilInvalidoException.class,
            TipoSanguineoObrigatorioException.class,
            DataExameInvalidaException.class,
            HorarioInvalidoException.class,
            UsuarioNaoInscritoException.class
    })
    public ResponseEntity<Map<String, Object>> regraNegocioInvalida(
            RuntimeException exception
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(
            IllegalArgumentException exception
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoInvalido(
            IllegalStateException exception
    ) {
        return erro(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> arquivoMuitoGrande(
            MaxUploadSizeExceededException exception
    ) {
        return erro(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo excede o tamanho máximo permitido."
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> erroArquivo(
            IOException exception
    ) {
        return erro(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível processar o arquivo."
        );
    }


    private ResponseEntity<Map<String, Object>> erro(
            HttpStatus status,
            String mensagem
    ) {
        Map<String, Object> resposta =
                new LinkedHashMap<>();

        resposta.put("success", false);
        resposta.put(
                "message",
                mensagem != null
                        ? mensagem
                        : "Não foi possível concluir a operação."
        );

        return ResponseEntity
                .status(status)
                .body(resposta);
    }
}