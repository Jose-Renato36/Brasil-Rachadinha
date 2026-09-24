package br.unit.eleicao.util;

import br.unit.eleicao.excecao.ArquivoInvalidoException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor de CSV com separador ';' (padrão do TSE e da Câmara).
 * Trata aspas, aspas duplicadas ("") e campos entre aspas que ocupam mais de uma linha.
 * As colunas são localizadas pelo nome do cabeçalho, não pela posição, para tolerar
 * mudanças de layout das fontes.
 */
public class LeitorCsv implements AutoCloseable {

    private final BufferedReader leitor;
    private final char separador;
    private final String nomeArquivo;
    private final Map<String, Integer> cabecalho = new HashMap<>();
    private long linhasLidas;

    public LeitorCsv(Path arquivo, Charset charset) throws ArquivoInvalidoException {
        this(abrir(arquivo), charset, ';', arquivo.getFileName().toString());
    }

    public LeitorCsv(InputStream entrada, Charset charset, char separador, String nomeArquivo)
            throws ArquivoInvalidoException {
        this.leitor = new BufferedReader(new InputStreamReader(entrada, charset), 1 << 16);
        this.separador = separador;
        this.nomeArquivo = nomeArquivo;
        lerCabecalho();
    }

    private static InputStream abrir(Path arquivo) throws ArquivoInvalidoException {
        try {
            return Files.newInputStream(arquivo);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível abrir " + arquivo + ": " + e.getMessage(), e);
        }
    }

    private void lerCabecalho() throws ArquivoInvalidoException {
        String[] campos = proximaLinha();
        if (campos == null) {
            throw new ArquivoInvalidoException("Arquivo vazio: " + nomeArquivo);
        }
        for (int i = 0; i < campos.length; i++) {
            String nome = campos[i].replace("﻿", "").strip();
            cabecalho.put(nome.toUpperCase(), i);
        }
    }

    /** Índice da primeira coluna encontrada entre os nomes alternativos; lança exceção se nenhuma existir. */
    public int indice(String... nomesAlternativos) throws ArquivoInvalidoException {
        int i = indiceOpcional(nomesAlternativos);
        if (i < 0) {
            throw new ArquivoInvalidoException("Coluna " + String.join(" / ", nomesAlternativos)
                    + " não encontrada em " + nomeArquivo);
        }
        return i;
    }

    /** Índice da coluna ou -1 se ela não existir no arquivo. */
    public int indiceOpcional(String... nomesAlternativos) {
        for (String nome : nomesAlternativos) {
            Integer i = cabecalho.get(nome.toUpperCase());
            if (i != null) {
                return i;
            }
        }
        return -1;
    }

    /** Lê o próximo registro; devolve null no fim do arquivo. */
    public String[] proximaLinha() throws ArquivoInvalidoException {
        try {
            String linha = leitor.readLine();
            if (linha == null) {
                return null;
            }
            linhasLidas++;
            List<String> campos = new ArrayList<>();
            StringBuilder atual = new StringBuilder();
            boolean entreAspas = false;
            while (true) {
                for (int i = 0; i < linha.length(); i++) {
                    char c = linha.charAt(i);
                    if (entreAspas) {
                        if (c == '"') {
                            if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                                atual.append('"');
                                i++;
                            } else {
                                entreAspas = false;
                            }
                        } else {
                            atual.append(c);
                        }
                    } else if (c == '"') {
                        entreAspas = true;
                    } else if (c == separador) {
                        campos.add(atual.toString());
                        atual.setLength(0);
                    } else {
                        atual.append(c);
                    }
                }
                if (!entreAspas) {
                    break;
                }
                // campo entre aspas continua na próxima linha física
                linha = leitor.readLine();
                if (linha == null) {
                    break;
                }
                atual.append('\n');
            }
            campos.add(atual.toString());
            return campos.toArray(new String[0]);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro lendo " + nomeArquivo + " (linha " + linhasLidas + "): "
                    + e.getMessage(), e);
        }
    }

    /** Acesso seguro a um campo: devolve "" se o índice não existir nessa linha. */
    public static String campo(String[] linha, int indice) {
        if (indice < 0 || indice >= linha.length) {
            return "";
        }
        return linha[indice].strip();
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    @Override
    public void close() throws ArquivoInvalidoException {
        try {
            leitor.close();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro ao fechar " + nomeArquivo, e);
        }
    }
}
