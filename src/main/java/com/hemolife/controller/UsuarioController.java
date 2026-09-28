package com.hemolife.controller;

import com.hemolife.dto.UsuarioResponse;
import com.hemolife.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody Map<String, Object> body) {
        String nome = obterString(body, "nome");
        String email = obterString(body, "email");
        String senha = obterString(body, "senha");
        String tipoSanguineo = obterStringOpcional(body, "tipo_sanguineo");
        String perfil = obterString(body, "perfil");

        UsuarioResponse usuario = usuarioService.criar(nome, email, senha, tipoSanguineo, perfil);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("message", "Usuario cadastrado com sucesso.");
        resposta.put("usuario", usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> buscarPorId(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.buscarPorId(id);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("usuario", usuario);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarPorEmail(@RequestParam String email) {
        UsuarioResponse usuario = usuarioService.buscarPorEmail(email);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("success", true);
        resposta.put("usuario", usuario);

        return ResponseEntity.ok(resposta);
    }

    private String obterString(Map<String, Object> body, String campo) {
        Object valor = body.get(campo);
        if (valor == null) {
            return null;
        }
        return String.valueOf(valor);
    }

    private String obterStringOpcional(Map<String, Object> body, String campo) {
        Object valor = body.get(campo);
        if (valor == null) {
            return null;
        }

        String texto = String.valueOf(valor);
        return texto.isBlank() ? null : texto;
    }
}
