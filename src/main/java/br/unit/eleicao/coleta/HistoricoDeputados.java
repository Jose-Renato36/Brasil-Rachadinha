package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ColetaException;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta a API da Câmara (/deputados/{id}/historico) para saber quando cada deputado esteve
 * em exercício. Sem isso, licenças no meio do mandato contariam como falta na assiduidade.
 * A resposta JSON é plana (um objeto por mudança de situação), então basta ler campo a campo.
 */
public class HistoricoDeputados {

    private static final Pattern OBJETO = Pattern.compile("\\{[^{}]*\\}");

    private final Path pasta;
    private final Downloader downloader;
    private final Consumer<String> log;

    /** @param downloader null = usar só o que já está em cache no disco */
    public HistoricoDeputados(Path pasta, Downloader downloader, Consumer<String> log) {
        this.pasta = pasta;
        this.downloader = downloader;
        this.log = log;
    }

    /** Um registro do histórico: data/hora e situação ("Exercício", "Afastado", "Fim de Mandato"...). */
    static final class Evento implements Comparable<Evento> {
        final String dataHora;
        final String situacao;

        Evento(String dataHora, String situacao) {
            this.dataHora = dataHora;
            this.situacao = situacao;
        }

        @Override
        public int compareTo(Evento o) {
            return dataHora.compareTo(o.dataHora);
        }
    }

    /**
     * Preenche os períodos de exercício de cada deputado. Falhas são registradas no log e o deputado
     * fica sem períodos (o indicador usa então a aproximação pelo primeiro e último voto).
     */
    public void preencher(Iterable<Deputado> deputados, int legislatura, LocalDate inicio, LocalDate fim) {
        int ok = 0;
        int falhas = 0;
        for (Deputado d : deputados) {
            try {
                String json = obter(d.getId());
                for (LocalDate[] p : periodos(json, String.valueOf(legislatura), inicio, fim)) {
                    d.adicionarExercicio(p[0], p[1]);
                }
                ok++;
            } catch (ColetaException e) {
                falhas++;
                if (downloader != null) {
                    log.accept("  aviso: histórico do deputado " + d.getId() + " indisponível (" + e.getMessage() + ")");
                }
            }
        }
        log.accept("  histórico de exercício: " + ok + " deputados" + (falhas > 0 ? ", " + falhas
                + " sem histórico (assiduidade usará a aproximação pelo 1º e último voto)" : ""));
    }

    private String obter(int id) throws ColetaException {
        Path pastaHist = pasta.resolve("historico");
        Path arquivo = pastaHist.resolve(id + ".json");
        try {
            if (!Files.exists(arquivo)) {
                if (downloader == null) {
                    throw new ColetaException("não baixado (colete com download para obter)");
                }
                Path baixado = downloader.baixar(FontesDados.historicoDeputado(id), pastaHist, false);
                Files.move(baixado, arquivo);
            }
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ColetaException("erro de arquivo: " + e.getMessage(), e);
        }
    }

    /** Converte o JSON do histórico em períodos [início, fim] com situação "Exercício". */
    static List<LocalDate[]> periodos(String json, String legislatura, LocalDate inicioLegislatura,
                                      LocalDate fimAnalise) {
        List<Evento> eventos = new ArrayList<>();
        Matcher m = OBJETO.matcher(json);
        while (m.find()) {
            String obj = m.group();
            String dataHora = campo(obj, "dataHora");
            String leg = campo(obj, "idLegislatura");
            String situacao = campo(obj, "situacao");
            // alguns registros anteriores à posse vêm marcados com a legislatura atual
            if (dataHora.isEmpty() || situacao.isEmpty() || !legislatura.equals(leg)
                    || dataHora.compareTo(inicioLegislatura.toString()) < 0) {
                continue;
            }
            eventos.add(new Evento(dataHora, situacao));
        }
        eventos.sort(null);
        List<LocalDate[]> periodos = new ArrayList<>();
        LocalDate inicio = null;
        for (Evento e : eventos) {
            boolean exercicio = Texto.normalizar(e.situacao).equals("EXERCICIO");
            LocalDate data = Texto.parseData(e.dataHora);
            if (exercicio && inicio == null) {
                inicio = data;
            } else if (!exercicio && inicio != null) {
                periodos.add(new LocalDate[]{inicio, data});
                inicio = null;
            }
        }
        if (inicio != null) {
            periodos.add(new LocalDate[]{inicio, fimAnalise});
        }
        return periodos;
    }

    /** Valor de um campo texto ou número num objeto JSON plano; "" se ausente ou null. */
    static String campo(String objeto, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|-?[0-9]+)").matcher(objeto);
        if (!m.find()) {
            return "";
        }
        return m.group(2) != null ? desescapar(m.group(2)) : m.group(1);
    }

    /** Trata escapes JSON como \u00ed (í) e \" que podem aparecer nos textos. */
    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'u' && i + 4 < s.length()) {
                    sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                } else {
                    sb.append(n == 'n' ? '\n' : n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
