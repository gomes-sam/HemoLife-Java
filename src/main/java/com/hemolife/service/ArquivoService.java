package com.hemolife.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ArquivoService {

    private final GridFsTemplate gridFsTemplate;

    public ArquivoService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    public String salvar(MultipartFile arquivo) throws IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "O arquivo deve ser informado."
            );
        }

        String nomeArquivo =
                arquivo.getOriginalFilename() != null
                        && !arquivo.getOriginalFilename().isBlank()
                        ? arquivo.getOriginalFilename()
                        : "arquivo";

        ObjectId id = gridFsTemplate.store(
                arquivo.getInputStream(),
                nomeArquivo,
                arquivo.getContentType()
        );

        return id.toHexString();
    }

    public GridFsResource buscar(String arquivoId) {

        validarArquivoId(arquivoId);

        GridFSFile arquivo = gridFsTemplate.findOne(
                Query.query(
                        Criteria.where("_id")
                                .is(new ObjectId(arquivoId))
                )
        );

        if (arquivo == null) {
            throw new IllegalArgumentException(
                    "Arquivo nao encontrado."
            );
        }

        return gridFsTemplate.getResource(arquivo);
    }

    public void excluir(String arquivoId) {

        if (arquivoId == null || arquivoId.isBlank()) {
            return;
        }

        if (!ObjectId.isValid(arquivoId)) {
            return;
        }

        gridFsTemplate.delete(
                Query.query(
                        Criteria.where("_id")
                                .is(new ObjectId(arquivoId))
                )
        );
    }

    private void validarArquivoId(String arquivoId) {

        if (arquivoId == null
                || arquivoId.isBlank()
                || !ObjectId.isValid(arquivoId)) {

            throw new IllegalArgumentException(
                    "Arquivo invalido."
            );
        }
    }
}