package com.hemolife.controller;

import com.hemolife.dto.ExameResponse;
import com.hemolife.service.ArquivoService;
import com.hemolife.service.ExameService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios/exames")
@RequiredArgsConstructor
public class ArquivoController {

    private final ArquivoService arquivoService;
    private final ExameService exameService;


    @PostMapping(
            value = "/{id}/arquivo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> enviarArquivo(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            HttpSession session
    ) throws IOException {

        Long usuarioId = obterUsuarioId(session);

        String novoArquivoId = null;
        String arquivoAntigoId = null;

        try {
            arquivoAntigoId =
                    exameService.buscarArquivoId(
                            usuarioId,
                            id
                    );
        } catch (IllegalArgumentException ignored) {
        }

        try {

            novoArquivoId =
                    arquivoService.salvar(arquivo);

            ExameResponse exame =
                    exameService.vincularArquivo(
                            usuarioId,
                            id,
                            novoArquivoId
                    );

            if (arquivoAntigoId != null
                    && !arquivoAntigoId.equals(novoArquivoId)) {

                arquivoService.excluir(
                        arquivoAntigoId
                );
            }

            Map<String, Object> resposta =
                    new LinkedHashMap<>();

            resposta.put("success", true);
            resposta.put(
                    "message",
                    "Arquivo enviado com sucesso."
            );
            resposta.put(
                    "arquivoId",
                    novoArquivoId
            );
            resposta.put(
                    "exame",
                    exame
            );

            return ResponseEntity.ok(resposta);

        } catch (RuntimeException | IOException exception) {

            if (novoArquivoId != null) {
                arquivoService.excluir(
                        novoArquivoId
                );
            }

            throw exception;
        }
    }


    @GetMapping("/{id}/arquivo")
    public ResponseEntity<InputStreamResource> baixarArquivo(
            @PathVariable Long id,
            HttpSession session
    ) throws IOException {

        Long usuarioId =
                obterUsuarioId(session);

        String arquivoId =
                exameService.buscarArquivoId(
                        usuarioId,
                        id
                );

        GridFsResource arquivo =
                arquivoService.buscar(
                        arquivoId
                );

        String nomeArquivo =
                arquivo.getFilename() != null
                        && !arquivo.getFilename().isBlank()
                        ? arquivo.getFilename()
                        : "arquivo";

        String contentType =
                arquivo.getContentType();

        MediaType mediaType;

        try {
            mediaType =
                    contentType != null
                            && !contentType.isBlank()
                            ? MediaType.parseMediaType(contentType)
                            : MediaType.APPLICATION_OCTET_STREAM;

        } catch (IllegalArgumentException exception) {

            mediaType =
                    MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .contentLength(
                        arquivo.contentLength()
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(
                                        nomeArquivo,
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(
                        new InputStreamResource(
                                arquivo.getInputStream()
                        )
                );
    }


    @DeleteMapping("/{id}/arquivo")
    public ResponseEntity<Map<String, Object>> excluirArquivo(
            @PathVariable Long id,
            HttpSession session
    ) {

        Long usuarioId =
                obterUsuarioId(session);

        String arquivoId =
                exameService.buscarArquivoId(
                        usuarioId,
                        id
                );

        ExameResponse exame =
                exameService.removerArquivo(
                        usuarioId,
                        id
                );

        arquivoService.excluir(
                arquivoId
        );

        Map<String, Object> resposta =
                new LinkedHashMap<>();

        resposta.put("success", true);
        resposta.put(
                "message",
                "Arquivo removido com sucesso."
        );
        resposta.put(
                "exame",
                exame
        );

        return ResponseEntity.ok(resposta);
    }


    private Long obterUsuarioId(
            HttpSession session
    ) {

        Object usuarioId =
                session.getAttribute("usuarioId");

        if (usuarioId == null) {
            throw new IllegalStateException(
                    "Usuario nao autenticado."
            );
        }

        if (usuarioId instanceof Number numero) {
            return numero.longValue();
        }

        return Long.valueOf(
                usuarioId.toString()
        );
    }
}