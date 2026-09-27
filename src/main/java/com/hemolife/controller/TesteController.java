package com.hemolife.controller;

import com.hemolife.dto.MensagemResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TesteController {

    @GetMapping(value = "/teste", produces = MediaType.APPLICATION_JSON_VALUE)
    public MensagemResponse teste() {
        return new MensagemResponse("Backend HemoLife Java funcionando");
    }
}
