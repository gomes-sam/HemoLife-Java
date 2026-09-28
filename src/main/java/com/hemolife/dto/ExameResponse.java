package com.hemolife.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hemolife.model.Exame;
import com.hemolife.model.StatusExame;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExameResponse(

        Long id,

        Long usuarioId,

        Long ongId,

        Long unidadeId,

        @JsonProperty("data_exame")
        LocalDate dataExame,

        String horario,

        StatusExame status,

        LocalDateTime criadoEm,

        @JsonProperty("arquivo_id")
        String arquivoId

) {

    public static ExameResponse de(Exame exame) {

        return new ExameResponse(
                exame.getId(),
                exame.getUsuario().getId(),
                exame.getOng().getId(),
                exame.getUnidade().getId(),
                exame.getDataExame(),
                exame.getHorarioFormatado(),
                exame.getStatus(),
                exame.getCriadoEm(),
                exame.getArquivoId()
        );
    }
}