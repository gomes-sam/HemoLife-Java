package com.hemolife.controller;

import com.hemolife.dto.InscricaoResponse;
import com.hemolife.dto.OngResponse;
import com.hemolife.service.InscricaoService;
import com.hemolife.service.OngService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class OngController {

    private final OngService ongService;
    private final InscricaoService inscricaoService;

    @GetMapping("/ongs")
    public ResponseEntity<Map<String, Object>> listarOngs(
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        List<OngResponse> ongs = ongService.listar();

        List<Long> idsInscritos = inscricaoService
                .listarOngsDoUsuario(usuarioId)
                .stream()
                .map(OngResponse::id)
                .toList();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("ongs", ongs);
        resposta.put("ongs_inscritas", idsInscritos);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/minhas-ongs")
    public ResponseEntity<Map<String, Object>> listarMinhasOngs(
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        List<OngResponse> ongs =
                inscricaoService.listarOngsDoUsuario(usuarioId);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("ongs", ongs);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/ongs/inscrever/{id}")
    public ResponseEntity<Map<String, Object>> inscrever(
            @PathVariable Long id,
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        InscricaoResponse inscricao =
                inscricaoService.inscrever(usuarioId, id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Inscrição realizada com sucesso.");
        resposta.put("inscricao", inscricao);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/ongs/cancelar/{id}")
    public ResponseEntity<Map<String, Object>> cancelarInscricao(
            @PathVariable Long id,
            HttpSession session
    ) {
        Long usuarioId = obterUsuarioId(session);

        inscricaoService.cancelar(usuarioId, id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Inscrição cancelada com sucesso.");

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/ongs/{id}")
    public ResponseEntity<Map<String, Object>> buscarPorId(
            @PathVariable Long id
    ) {
        OngResponse ong =
                ongService.buscarPorId(id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("ong", ong);

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