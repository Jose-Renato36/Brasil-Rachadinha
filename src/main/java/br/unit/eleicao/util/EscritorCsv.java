package br.unit.eleicao.util;

import br.unit.eleicao.excecao.ArquivoInvalidoException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Grava arquivos CSV (UTF-8, separador ';') usados como cache local dos dados processados. */
public class EscritorCsv implements AutoCloseable {

    private final BufferedWriter escritor;
    private final Path arquivo;

    public EscritorCsv(Path arquivo, String... cabecalho) throws ArquivoInvalidoException {
        this.arquivo = arquivo;
        try {
            if (arquivo.getParent() != null) {
                Files.createDirectories(arquivo.getParent());
            }
            this.escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível criar " + arquivo + ": " + e.getMessage(), e);
        }
        escrever((Object[]) cabecalho);
    }

    public void escrever(Object... valores) throws ArquivoInvalidoException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escapar(valores[i]));
        }
        try {
            escritor.write(sb.toString());
            escritor.newLine();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro gravando " + arquivo + ": " + e.getMessage(), e);
        }
    }

    private static String escapar(Object valor) {
        if (valor == null) {
            return "";
        }
        String s = valor.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    @Override
    public void close() throws ArquivoInvalidoException {
        try {
            escritor.close();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro ao fechar " + arquivo, e);
        }
    }
}
