package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.util.LeitorCsv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Abre um CSV bruto, esteja ele solto ou dentro de um .zip (lido em fluxo, sem descompactar no disco). */
public final class ArquivosBrutos {

    private ArquivosBrutos() {
    }

    /**
     * @param sufixoEntrada fim do nome da entrada desejada dentro do zip (ex.: "_SE.csv");
     *                      ignorado quando o arquivo não é zip
     */
    public static LeitorCsv abrir(Path arquivo, Charset charset, String sufixoEntrada)
            throws ArquivoInvalidoException {
        return abrir(arquivo, charset, "", sufixoEntrada, ';');
    }

    /**
     * Como {@link #abrir(Path, Charset, String)}, exigindo também o começo do nome da entrada (ex.: zips
     * com "receitas_candidatos_2026_SE.csv" e "receitas_candidatos_doador_originario_2026_SE.csv").
     */
    public static LeitorCsv abrir(Path arquivo, Charset charset, String prefixoEntrada, String sufixoEntrada,
                                  char separador) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            throw new ArquivoInvalidoException("Arquivo não encontrado: " + arquivo.toAbsolutePath());
        }
        if (!arquivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            try {
                return new LeitorCsv(Files.newInputStream(arquivo), charset, separador, arquivo.getFileName().toString());
            } catch (IOException e) {
                throw new ArquivoInvalidoException("Não foi possível abrir " + arquivo + ": " + e.getMessage(), e);
            }
        }
        ZipInputStream zip = null;
        try {
            InputStream entrada = Files.newInputStream(arquivo);
            zip = new ZipInputStream(entrada, charset);
            ZipEntry entry;
            String sufixo = sufixoEntrada.toLowerCase(Locale.ROOT);
            String prefixo = prefixoEntrada.toLowerCase(Locale.ROOT);
            while ((entry = zip.getNextEntry()) != null) {
                String nome = entry.getName().toLowerCase(Locale.ROOT);
                String base = nome.substring(nome.lastIndexOf('/') + 1);
                if (nome.endsWith(sufixo) && base.startsWith(prefixo)) {
                    return new LeitorCsv(zip, charset, separador, arquivo.getFileName() + "!" + entry.getName());
                }
            }
            zip.close();
        } catch (IOException e) {
            fecharSilenciosamente(zip);
            throw new ArquivoInvalidoException("Erro lendo " + arquivo + ": " + e.getMessage(), e);
        }
        throw new ArquivoInvalidoException("Nenhuma entrada terminada em '" + sufixoEntrada + "' dentro de " + arquivo);
    }

    /** Código usado para a coleta do Brasil inteiro. */
    public static final String NACIONAL = "BR";

    public static boolean isNacional(String uf) {
        return NACIONAL.equalsIgnoreCase(uf);
    }

    /**
     * Entradas a ler dentro dos zips do TSE: a da UF (ex.: "_SE.csv") ou, no modo nacional, o arquivo do
     * país inteiro ("_BRASIL.csv") e o de abrangência nacional ("_BR.csv", onde fica a eleição presidencial).
     */
    public static String[] sufixos(String uf) {
        return isNacional(uf) ? new String[]{"_BRASIL.csv", "_BR.csv"} : new String[]{"_" + uf.toUpperCase() + ".csv"};
    }

    /** true se a linha é da UF pedida (no modo nacional, todas são). */
    public static boolean aceitaUf(String ufPedida, String ufLinha) {
        return isNacional(ufPedida) || ufPedida.equalsIgnoreCase(ufLinha);
    }

    /** Como {@link #abrir}, mas devolve null quando o zip não tem a entrada (útil para entradas opcionais). */
    public static LeitorCsv abrirSeExistir(Path arquivo, Charset charset, String sufixoEntrada)
            throws ArquivoInvalidoException {
        return abrirSeExistir(arquivo, charset, "", sufixoEntrada, ';');
    }

    public static LeitorCsv abrirSeExistir(Path arquivo, Charset charset, String prefixoEntrada, String sufixoEntrada,
                                           char separador) throws ArquivoInvalidoException {
        try {
            return abrir(arquivo, charset, prefixoEntrada, sufixoEntrada, separador);
        } catch (ArquivoInvalidoException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Nenhuma entrada")) {
                return null;
            }
            throw e;
        }
    }

    private static void fecharSilenciosamente(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ignorada) {
            // já estamos tratando um erro anterior, que é o relevante
        }
    }
}
