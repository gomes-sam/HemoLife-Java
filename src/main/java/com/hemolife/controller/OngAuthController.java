package com.hemolife.controller;

import com.hemolife.dto.OngResponse;
import com.hemolife.dto.UsuarioResponse;
import com.hemolife.service.InscricaoService;
import com.hemolife.service.OngService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ong")
@RequiredArgsConstructor
public class OngAuthController {

    private final OngService ongService;
    private final InscricaoService inscricaoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody Map<String, Object> body
    ) {

        String nome = obterString(body, "nome");
        String email = obterString(body, "email");
        String senha = obterString(body, "senha");
        String cnpj = obterString(body, "cnpj");

        OngResponse ong = ongService.cadastrar(
                nome,
                email,
                senha,
                cnpj
        );

        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("success", true);
        resposta.put(
                "message",
                "ONG cadastrada com sucesso."
        );
        resposta.put("ong", ong);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {

        String email = obterString(body, "email");
        String senha = obterString(body, "senha");

        OngResponse ong = ongService.autenticar(
                email,
                senha
        );

        session.setAttribute(
                "ongId",
                ong.id()
        );

        session.setAttribute(
                "perfil",
                "ONG"
        );

        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("success", true);
        resposta.put(
                "message",
                "Login realizado com sucesso."
        );
        resposta.put("ong", ong);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(
            HttpSession session
    ) {

        Object ongId = session.getAttribute("ongId");

        if (ongId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "authenticated", false,
                            "message", "Login required"
                    ));
        }

        Long id = converterParaLong(ongId);

        OngResponse ong = ongService.buscarPorId(id);

        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("success", true);
        resposta.put("authenticated", true);
        resposta.put("ong", ong);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/membros")
    public ResponseEntity<Map<String, Object>> membros(
            HttpSession session
    ) {

        Long ongId = obterOngId(session);

        List<UsuarioResponse> membros =
                inscricaoService.listarUsuariosDaOng(ongId);

        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("success", true);
        resposta.put("membros", membros);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpSession session
    ) {

        session.invalidate();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Logout realizado com sucesso."
                )
        );
    }

    private Long obterOngId(HttpSession session) {

        Object ongId = session.getAttribute("ongId");

        if (ongId == null) {
            throw new IllegalStateException(
                    "ONG nao autenticada."
            );
        }

        return converterParaLong(ongId);
    }

    private Long converterParaLong(Object valor) {

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        return Long.valueOf(
                valor.toString()
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
}