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
        if (!Files.exists(arquivo)) {
            throw new ArquivoInvalidoException("Arquivo não encontrado: " + arquivo.toAbsolutePath());
        }
        if (!arquivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return new LeitorCsv(arquivo, charset);
        }
        ZipInputStream zip = null;
        try {
            InputStream entrada = Files.newInputStream(arquivo);
            zip = new ZipInputStream(entrada, charset);
            ZipEntry entry;
            String sufixo = sufixoEntrada.toLowerCase(Locale.ROOT);
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().toLowerCase(Locale.ROOT).endsWith(sufixo)) {
                    return new LeitorCsv(zip, charset, ';', arquivo.getFileName() + "!" + entry.getName());
                }
            }
            zip.close();
        } catch (IOException e) {
            fecharSilenciosamente(zip);
            throw new ArquivoInvalidoException("Erro lendo " + arquivo + ": " + e.getMessage(), e);
        }
        throw new ArquivoInvalidoException("Nenhuma entrada terminada em '" + sufixoEntrada + "' dentro de " + arquivo);
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
