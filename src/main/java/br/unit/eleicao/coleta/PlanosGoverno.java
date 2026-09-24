package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Planos de governo que os candidatos registraram no TSE (obrigatórios para prefeito, governador e
 * presidente). O TSE publica um zip por UF (proposta_governo_ANO_UF.zip; "BR" para a Presidência) com os
 * PDFs nomeados pelo número da candidatura (ex.: 2026SE260001234567_01.pdf). Os PDFs das candidaturas da
 * base são copiados para a pasta "planos" da base processada, para o site mostrar.
 */
public class PlanosGoverno {

    public static final String PASTA = "planos";
    private static final long LIMITE_BYTES = 40L * 1024 * 1024;

    private final Path brutos;
    private final Consumer<String> log;

    public PlanosGoverno(Path brutos, Consumer<String> log) {
        this.brutos = brutos;
        this.log = log;
    }

    /** UFs cujos zips são consultados: a pedida (ou todas, no modo nacional) e a Presidência. */
    public static List<String> ufs(String uf) {
        List<String> l = new ArrayList<>();
        if (ArquivosBrutos.isNacional(uf)) {
            l.addAll(br.unit.eleicao.coleta.mandato.CodigosIbge.CODIGO_UF.keySet());
        } else {
            l.add(uf.toUpperCase());
        }
        l.add("BR");
        return l;
    }

    /** @return true se algum zip de planos foi lido */
    public boolean processar(List<Candidato> candidatos, String uf, int ano, Path destino) {
        Map<String, Candidato> porSq = new HashMap<>();
        for (Candidato c : candidatos) {
            if (c.getTipoCargo().isExecutivo() && c.getTipoCargo().isPrincipal()) {
                porSq.put(c.getSq(), c);
            }
        }
        boolean algum = false;
        int n = 0;
        for (String u : ufs(uf)) {
            Path zip = brutos.resolve(FontesDados.nomeLocal(FontesDados.propostaGoverno(ano, u)));
            if (!Files.exists(zip)) {
                continue;
            }
            algum = true;
            try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
                ZipEntry e;
                while ((e = in.getNextEntry()) != null) {
                    String nome = e.getName().substring(e.getName().lastIndexOf('/') + 1);
                    Candidato c = dono(nome, porSq);
                    if (c == null || !nome.toLowerCase().endsWith(".pdf")) {
                        continue;
                    }
                    String arquivo = c.getSq() + "_" + (c.getPlanosGoverno().size() + 1) + ".pdf";
                    try {
                        copiar(in, destino.resolve(PASTA).resolve(arquivo));
                        c.adicionarPlanoGoverno(arquivo);
                        n++;
                    } catch (IOException ex) {
                        log.accept("  aviso: " + ex.getMessage());
                    }
                }
            } catch (IOException ex) {
                log.accept("  aviso: plano de governo " + zip.getFileName() + ": " + ex.getMessage());
            }
        }
        if (!algum) {
            log.accept("  aviso: pacotes de planos de governo ausentes - sem link para os planos");
        } else {
            log.accept("Planos de governo: " + n + " arquivo(s) copiado(s).");
        }
        return algum;
    }

    /** O nome do PDF traz o número da candidatura: procura o SQ de alguma candidatura da base. */
    static Candidato dono(String nomeArquivo, Map<String, Candidato> porSq) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{9,}").matcher(nomeArquivo);
        while (m.find()) {
            String numeros = m.group();
            for (int i = 0; i + 9 <= numeros.length(); i++) {
                Candidato c = porSq.get(numeros.substring(i));
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    private static void copiar(InputStream in, Path destino) throws IOException {
        Files.createDirectories(destino.getParent());
        byte[] buffer = new byte[1 << 16];
        long total = 0;
        try (var out = Files.newOutputStream(destino)) {
            int lidos;
            while ((lidos = in.read(buffer)) > 0) {
                total += lidos;
                if (total > LIMITE_BYTES) {
                    break; // PDF anormalmente grande: guarda só o começo seria enganoso
                }
                out.write(buffer, 0, lidos);
            }
        }
        if (total > LIMITE_BYTES) {
            Files.deleteIfExists(destino);
            throw new IOException("PDF acima de 40 MB ignorado: " + destino.getFileName());
        }
    }
}
