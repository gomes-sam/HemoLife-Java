package com.hemolife.database;

import com.hemolife.exception.DatabaseConnectionException;

/**
 * Contrato comum que adapta a verificacao de conectividade de cada banco.
 * Pools e clientes sao gerenciados pelo Spring; este contrato nao unifica SQL e arquivos.
 */
public interface DatabaseAdapter {

    /**
     * Verifica uma conexao real sem alterar dados.
     *
     * @throws DatabaseConnectionException quando o banco nao responde corretamente
     */
    void verificarConexao();
}
