package com.hemolife.controller;

import com.hemolife.dto.ExameResponse;
import com.hemolife.service.ExameService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios/exames")
@RequiredArgsConstructor
public class ExameController {

    private final ExameService exameService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        List<ExameResponse> exames =
                exameService.listarDoUsuario(usuarioId);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("exames", exames);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> agendar(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        Long ongId = obterLong(body, "ong_id");
        Long unidadeId = obterLong(body, "unidade_id");

        String dataTexto = obterString(body, "data_exame");
        String horario = obterString(body, "horario");

        LocalDate dataExame = LocalDate.parse(dataTexto);

        ExameResponse exame = exameService.agendar(
                usuarioId,
                ongId,
                unidadeId,
                dataExame,
                horario
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Agendamento realizado com sucesso.");
        resposta.put("exame", exame);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Map<String, Object>> cancelar(
            @PathVariable Long id,
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        ExameResponse exame =
                exameService.cancelar(usuarioId, id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Agendamento cancelado com sucesso.");
        resposta.put("exame", exame);

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

    private String obterString(
            Map<String, Object> body,
            String campo
    ) {
        Object valor = body.get(campo);

        if (valor == null) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " deve ser informado."
            );
        }

        String texto =
                String.valueOf(valor).trim();

        if (texto.isBlank()) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " deve ser informado."
            );
        }

        return texto;
    }

    private Long obterLong(
            Map<String, Object> body,
            String campo
    ) {
        Object valor = body.get(campo);

        if (valor == null) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " deve ser informado."
            );
        }

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        try {
            return Long.valueOf(
                    String.valueOf(valor)
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " deve ser um numero."
            );
        }
    }
}