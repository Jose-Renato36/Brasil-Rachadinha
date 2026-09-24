package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Verba de gabinete dos senadores (CEAPS, Senado Federal): despesa_ceaps_ANO.csv, Windows-1252, ';'.
 * A 1ª linha traz a data de atualização; a 2ª é o cabeçalho (ANO;MES;SENADOR;TIPO_DESPESA;CNPJ_CPF;
 * FORNECEDOR;DOCUMENTO;DATA;DETALHAMENTO;VALOR_REEMBOLSADO;COD_DOCUMENTO). Resultado: gasto médio por mês
 * de cada senador, pelo nome parlamentar.
 */
public class CotaSenado {

    private final Path pasta;
    private final Consumer<String> log;

    public CotaSenado(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    /** @return nome parlamentar normalizado → gasto médio mensal (meses com alguma despesa) */
    public Map<String, Double> gastoMensal(int[] anos) {
        Map<String, Double> total = new HashMap<>();
        Map<String, Set<String>> meses = new HashMap<>();
        int lidos = 0;
        for (int ano : anos) {
            Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.cotaSenado(ano)));
            if (!Files.exists(arquivo)) {
                continue;
            }
            try {
                ler(arquivo, total, meses);
                lidos++;
            } catch (ArquivoInvalidoException | IOException e) {
                log.accept("  aviso: verba do Senado " + ano + ": " + e.getMessage());
            }
        }
        Map<String, Double> media = new HashMap<>();
        for (Map.Entry<String, Double> e : total.entrySet()) {
            media.put(e.getKey(), e.getValue() / Math.max(1, meses.get(e.getKey()).size()));
        }
        if (lidos > 0) {
            log.accept("  Senado: verba de gabinete de " + media.size() + " senadores (" + lidos + " ano(s)).");
        }
        return media;
    }

    static void ler(Path arquivo, Map<String, Double> total, Map<String, Set<String>> meses)
            throws IOException, ArquivoInvalidoException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(arquivo))) {
            pularPrimeiraLinhaSeNaoForCabecalho(in);
            LeitorCsv csv = new LeitorCsv(in, EmendasParlamentares.WINDOWS_1252, ';', arquivo.getFileName().toString());
            int iAno = csv.indice("ANO");
            int iMes = csv.indice("MES");
            int iSenador = csv.indice("SENADOR");
            int iValor = csv.indice("VALOR_REEMBOLSADO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Double v = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                String nome = Texto.normalizar(LeitorCsv.campo(l, iSenador));
                if (v == null || nome.isEmpty()) {
                    continue;
                }
                total.merge(nome, v, Double::sum);
                meses.computeIfAbsent(nome, k -> new HashSet<>()).add(LeitorCsv.campo(l, iAno) + "-" + LeitorCsv.campo(l, iMes));
            }
        }
    }

    /** O arquivo começa com "ULTIMA ATUALIZACAO;...": descarta essa linha antes do cabeçalho. */
    private static void pularPrimeiraLinhaSeNaoForCabecalho(InputStream in) throws IOException {
        in.mark(4096);
        StringBuilder linha = new StringBuilder();
        int b;
        while ((b = in.read()) != -1 && b != '\n') {
            linha.append((char) b);
        }
        if (linha.toString().replace("\"", "").toUpperCase().startsWith("ANO;")) {
            in.reset();
        }
    }
}
