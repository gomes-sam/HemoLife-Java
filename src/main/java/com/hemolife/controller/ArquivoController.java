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

    // ==========================================
    // UPLOAD
    // POST /usuarios/exames/{id}/arquivo
    // ==========================================

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

        /*
         * Verifica se o exame já possui um arquivo.
         */
        try {
            arquivoAntigoId =
                    exameService.buscarArquivoId(
                            usuarioId,
                            id
                    );
        } catch (IllegalArgumentException ignored) {
            // O exame ainda não possui arquivo.
        }

        try {

            /*
             * 1. Salva o arquivo real no MongoDB / GridFS.
             */
            novoArquivoId =
                    arquivoService.salvar(arquivo);

            /*
             * 2. Salva somente o ObjectId no PostgreSQL.
             */
            ExameResponse exame =
                    exameService.vincularArquivo(
                            usuarioId,
                            id,
                            novoArquivoId
                    );

            /*
             * 3. Se existia um arquivo anterior,
             * remove ele do Mongo somente depois
             * que o novo ObjectId foi salvo no PostgreSQL.
             */
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

            /*
             * Se o arquivo novo foi salvo no Mongo,
             * mas houve erro ao atualizar o PostgreSQL,
             * remove o arquivo órfão.
             */
            if (novoArquivoId != null) {
                arquivoService.excluir(
                        novoArquivoId
                );
            }

            throw exception;
        }
    }

    // ==========================================
    // DOWNLOAD
    // GET /usuarios/exames/{id}/arquivo
    // ==========================================

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

    // ==========================================
    // EXCLUIR
    // DELETE /usuarios/exames/{id}/arquivo
    // ==========================================

    @DeleteMapping("/{id}/arquivo")
    public ResponseEntity<Map<String, Object>> excluirArquivo(
            @PathVariable Long id,
            HttpSession session
    ) {

        Long usuarioId =
                obterUsuarioId(session);

        /*
         * Primeiro descobre qual ObjectId está ligado
         * ao exame.
         */
        String arquivoId =
                exameService.buscarArquivoId(
                        usuarioId,
                        id
                );

        /*
         * Remove a referência do PostgreSQL.
         */
        ExameResponse exame =
                exameService.removerArquivo(
                        usuarioId,
                        id
                );

        /*
         * Remove o arquivo real do MongoDB/GridFS.
         */
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

    // ==========================================
    // USUÁRIO DA SESSÃO
    // ==========================================

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