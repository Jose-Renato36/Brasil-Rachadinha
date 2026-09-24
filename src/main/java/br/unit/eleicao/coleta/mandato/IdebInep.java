package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
import br.unit.eleicao.util.LeitorXlsx;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * IDEB (INEP), nota de 0 a 10 que junta aprovação e desempenho em provas, publicada a cada 2 anos.
 * Pacotes oficiais em download.inep.gov.br/ideb/resultados/ (zip com .xlsx, cabeçalho na 10ª linha,
 * colunas VL_OBSERVADO_AAAA com a série desde 2005):
 * <ul>
 *   <li>prefeito(a): rede municipal, anos iniciais (1º ao 5º ano), comparada com a rede municipal do estado;</li>
 *   <li>governador(a): rede estadual, anos finais e ensino médio, comparada com a rede estadual do Brasil;</li>
 *   <li>presidente: escolas públicas do Brasil nas três etapas.</li>
 * </ul>
 */
public class IdebInep extends FonteMandato {

    private static final String BASE = "https://download.inep.gov.br/ideb/resultados/";
    static final String AI = "AI";
    static final String AF = "AF";
    static final String EM = "EM";
    private static final Map<String, String> ETAPA = Map.of(AI, "anos iniciais", AF, "anos finais", EM, "ensino médio");

    private final int[] edicoes;
    private boolean carregado;
    /** código do município → ano → IDEB da rede municipal (anos iniciais). */
    private final Map<String, Map<Integer, Double>> municipios = new HashMap<>();
    /** "UF|REDE|ETAPA" ou "BR|REDE|ETAPA" → ano → IDEB. */
    private final Map<String, Map<Integer, Double>> agregados = new HashMap<>();

    public IdebInep(Path brutos, Downloader downloader, Consumer<String> log, int anoEleicao) {
        super(brutos, downloader, log);
        int ultima = anoEleicao % 2 == 1 ? anoEleicao - 2 : anoEleicao - 1; // edições em anos ímpares
        this.edicoes = new int[]{ultima, ultima - 2, ultima - 4};
    }

    @Override
    public String getNome() {
        return "inep-ideb";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return true;
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        carregar();
        List<SerieMandato> series = new ArrayList<>();
        if (m.isMunicipal()) {
            adicionar(series, m, "rede municipal, " + ETAPA.get(AI), municipios.get(m.getCodigoIbge()),
                    agregados.get(m.getUf() + "|MUNICIPAL|" + AI), "rede municipal do estado");
        } else if (m.isEstadual()) {
            for (String etapa : new String[]{AF, EM}) {
                adicionar(series, m, "rede estadual, " + ETAPA.get(etapa), agregados.get(m.getUf() + "|ESTADUAL|" + etapa),
                        agregados.get("BR|ESTADUAL|" + etapa), "redes estaduais do Brasil");
            }
        } else {
            for (String etapa : new String[]{AI, AF, EM}) {
                Map<Integer, Double> br = agregados.get("BR|PUBLICA|" + etapa);
                adicionar(series, m, "escolas públicas, " + ETAPA.get(etapa), br, null, null);
            }
        }
        return series;
    }

    private void adicionar(List<SerieMandato> series, MandatoExecutivo m, String rotulo, Map<Integer, Double> proprio,
                           Map<Integer, Double> referencia, String nomeRef) {
        if (proprio == null) {
            return;
        }
        SerieMandato s = nova(m, TemaMandato.EDUCACAO, "IDEB (" + rotulo + ")", "nota", FormaResumo.VARIACAO_PONTOS,
                true, "INEP – IDEB", "Nota de 0 a 10 que combina aprovação escolar e resultado nas provas do SAEB, "
                        + "medida a cada 2 anos. A pandemia afetou as edições de 2021 e 2023.");
        if (nomeRef != null) {
            s.setReferenciaNome(nomeRef);
        }
        // o IDEB é bienal: inclui a edição imediatamente anterior ao mandato como "antes"
        for (Map.Entry<Integer, Double> e : proprio.entrySet()) {
            int ano = e.getKey();
            if (ano >= m.getAnoEleicao() - 2 && ano <= m.getUltimoAno()) {
                Double ref = referencia == null ? null : referencia.get(ano);
                s.adicionar(ano, e.getValue(), ref);
            }
        }
        if (!s.isVazia()) {
            series.add(s);
        }
    }

    private void carregar() {
        if (carregado) {
            return;
        }
        carregado = true;
        for (int ed : edicoes) {
            Path mun = obterArquivo(BASE + "divulgacao_anos_iniciais_municipios_" + ed + ".zip",
                    "anos_iniciais_municipios_" + ed + ".zip", "application/zip");
            Path ufs = obterArquivo(BASE + "divulgacao_regioes_ufs_ideb_" + ed + ".zip", "regioes_ufs_" + ed + ".zip",
                    "application/zip");
            Path br = obterArquivo(BASE + "divulgacao_brasil_ideb_" + ed + ".zip", "brasil_" + ed + ".zip",
                    "application/zip");
            if (mun == null && ufs == null && br == null) {
                continue;
            }
            try {
                if (mun != null) {
                    lerMunicipios(abrir(mun));
                }
                if (ufs != null) {
                    lerAgregados(abrir(ufs), false);
                }
                if (br != null) {
                    lerAgregados(abrir(br), true);
                }
                log.accept("  IDEB: edição " + ed + " lida (" + municipios.size() + " municípios)");
                return;
            } catch (ArquivoInvalidoException | IOException e) {
                log.accept("  aviso: IDEB " + ed + ": " + e.getMessage());
            }
        }
        log.accept("  aviso: IDEB não encontrado (baixe os pacotes do INEP para dados/brutos/mandatos/inep-ideb)");
    }

    private static LeitorXlsx abrir(Path zip) throws IOException, ArquivoInvalidoException {
        try (InputStream in = Files.newInputStream(zip)) {
            return LeitorXlsx.deZip(in);
        }
    }

    /** Arquivo de municípios: colunas SG_UF, CO_MUNICIPIO, NO_MUNICIPIO, REDE, ..., VL_OBSERVADO_AAAA. */
    void lerMunicipios(LeitorXlsx x) throws ArquivoInvalidoException {
        List<String[]> linhas = x.linhas(x.getAbas().get(0));
        int cab = linhaCabecalho(linhas);
        if (cab < 0) {
            throw new ArquivoInvalidoException("cabeçalho VL_OBSERVADO_ não encontrado na planilha de municípios");
        }
        String[] h = linhas.get(cab);
        int iCod = coluna(h, "CO_MUNICIPIO");
        int iRede = coluna(h, "REDE");
        Map<Integer, Integer> anos = colunasDeAno(h);
        for (int i = cab + 1; i < linhas.size(); i++) {
            String[] l = linhas.get(i);
            if (iCod < 0 || iRede < 0 || !Texto.normalizar(celula(l, iRede)).startsWith("MUNICIPAL")) {
                continue;
            }
            String cod = celula(l, iCod).replaceAll("\\.0+$", "");
            Map<Integer, Double> serie = valores(l, anos);
            if (!cod.isEmpty() && !serie.isEmpty()) {
                municipios.put(cod, serie);
            }
        }
    }

    /**
     * Planilhas de UFs/regiões (1ª coluna = nome da UF ou região, 2ª = rede) e do Brasil (uma coluna de rede).
     * Uma aba por etapa: "(AI)", "(AF)", "(EM)".
     */
    void lerAgregados(LeitorXlsx x, boolean brasil) throws ArquivoInvalidoException {
        List<String> abas = x.getAbas();
        for (int a = 0; a < abas.size(); a++) {
            String aba = abas.get(a);
            String etapa = aba.contains("(AF)") ? AF : aba.contains("(EM)") ? EM : aba.contains("(AI)") ? AI
                    : a == 0 ? AI : a == 1 ? AF : EM;
            List<String[]> linhas = x.linhas(aba);
            int cab = linhaCabecalho(linhas);
            if (cab < 0) {
                continue;
            }
            String[] h = linhas.get(cab);
            Map<Integer, Integer> anos = colunasDeAno(h);
            int iRede = brasil ? Math.max(0, coluna(h, "REDE")) : 1;
            String lugar = null;
            for (int i = cab + 1; i < linhas.size(); i++) {
                String[] l = linhas.get(i);
                if (!brasil && !celula(l, 0).isBlank()) {
                    lugar = celula(l, 0); // nome da UF aparece só na primeira linha do bloco
                }
                String sigla = brasil ? "BR" : lugar == null ? null : CodigosIbge.siglaPorNome(lugar);
                if (sigla == null && lugar != null && Texto.normalizar(lugar).equals("BRASIL")) {
                    sigla = "BR";
                }
                String rede = Texto.normalizar(celula(l, iRede)).replaceAll("[^A-Z]", " ").strip();
                rede = rede.isEmpty() ? "" : rede.split(" ")[0];
                Map<Integer, Double> serie = valores(l, anos);
                if (sigla != null && !rede.isEmpty() && !serie.isEmpty()) {
                    agregados.putIfAbsent(sigla + "|" + rede + "|" + etapa, serie);
                }
            }
        }
    }

    private static int linhaCabecalho(List<String[]> linhas) {
        for (int i = 0; i < Math.min(40, linhas.size()); i++) {
            for (String c : linhas.get(i)) {
                if (c.startsWith("VL_OBSERVADO_")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int coluna(String[] cabecalho, String nome) {
        for (int i = 0; i < cabecalho.length; i++) {
            if (Texto.normalizar(cabecalho[i]).replace(' ', '_').equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    private static Map<Integer, Integer> colunasDeAno(String[] cabecalho) {
        Map<Integer, Integer> anos = new TreeMap<>();
        for (int i = 0; i < cabecalho.length; i++) {
            String c = cabecalho[i].strip();
            if (c.matches("VL_OBSERVADO_\\d{4}")) {
                anos.put(Integer.parseInt(c.substring(13)), i);
            }
        }
        return anos;
    }

    private static Map<Integer, Double> valores(String[] linha, Map<Integer, Integer> anos) {
        Map<Integer, Double> serie = new TreeMap<>();
        for (Map.Entry<Integer, Integer> e : anos.entrySet()) {
            Double v = Texto.parseDecimal(celula(linha, e.getValue()));
            if (v != null && v >= 0 && v <= 10) {
                serie.put(e.getKey(), v);
            }
        }
        return serie;
    }

    private static String celula(String[] linha, int i) {
        return i >= 0 && i < linha.length ? linha[i].strip() : "";
    }
}
