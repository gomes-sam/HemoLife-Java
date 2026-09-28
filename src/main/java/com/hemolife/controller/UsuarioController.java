package com.hemolife.controller;

import com.hemolife.dto.UsuarioResponse;
import com.hemolife.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody Map<String, Object> body
    ) {
        String nome = obterString(body, "nome");
        String email = obterString(body, "email");
        String senha = obterString(body, "senha");
        String tipoSanguineo = obterStringOpcional(body, "tipo_sanguineo");
        String perfil = obterString(body, "perfil");

        UsuarioResponse usuario = usuarioService.criar(
                nome,
                email,
                senha,
                tipoSanguineo,
                perfil
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Usuario cadastrado com sucesso.");
        resposta.put("usuario", usuario);

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
        String perfil = obterString(body, "perfil");

        UsuarioResponse usuario = usuarioService.autenticar(
                email,
                senha,
                perfil
        );

        session.setAttribute("usuarioId", usuario.id());
        session.setAttribute("perfil", usuario.perfil());

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Login realizado com sucesso.");
        resposta.put("usuario", usuario);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(
            HttpSession session
    ) {
        Object usuarioId = session.getAttribute("usuarioId");

        if (usuarioId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "authenticated", false,
                            "message", "Login required"
                    ));
        }

        Long id = ((Number) usuarioId).longValue();

        UsuarioResponse usuario = usuarioService.buscarPorId(id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("authenticated", true);
        resposta.put("usuario", usuario);

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
                        "message", "Logout realizado com sucesso."
                )
        );
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<Map<String, Object>> buscarPorId(
            @PathVariable Long id
    ) {
        UsuarioResponse usuario = usuarioService.buscarPorId(id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("usuario", usuario);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarPorEmail(
            @RequestParam String email
    ) {
        UsuarioResponse usuario = usuarioService.buscarPorEmail(email);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("usuario", usuario);

        return ResponseEntity.ok(resposta);
    }

    private String obterString(
            Map<String, Object> body,
            String campo
    ) {
        Object valor = body.get(campo);

        if (valor == null) {
            return null;
        }

        return String.valueOf(valor);
    }

    private String obterStringOpcional(
            Map<String, Object> body,
            String campo
    ) {
        Object valor = body.get(campo);

        if (valor == null) {
            return null;
        }

        String texto = String.valueOf(valor);

        return texto.isBlank()
                ? null
                : texto;
    }
}