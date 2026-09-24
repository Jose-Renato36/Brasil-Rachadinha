package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.ArquivosBrutos;
import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.coleta.FontesDados;
import br.unit.eleicao.coleta.ProcessadorCamara;
import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Propostas que o Presidente da República enviou ao Congresso, pelos arquivos anuais da Câmara:
 * proposicoesAutores-ANO.csv (autor com codTipoAutor 30000 / "Poder Executivo") e proposicoes-ANO.csv
 * (tipo, data de apresentação e situação). Conta medidas provisórias (MPV) e projetos (PL, PLP, PEC) por ano
 * e quantos viraram lei. Usa os arquivos já baixados para a Câmara; não faz novos downloads.
 */
public class ProjetosExecutivo extends FonteMandato {

    private static final Set<String> PROJETOS = Set.of("PL", "PLP", "PEC");
    private final Path brutos;

    public ProjetosExecutivo(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
        this.brutos = brutos;
    }

    @Override
    public String getNome() {
        return "camara-executivo";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isFederal();
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        String fonte = "Câmara dos Deputados – proposições e autores";
        SerieMandato mpv = nova(m, TemaMandato.LEIS, "Medidas provisórias editadas", "", FormaResumo.SOMA, null, fonte,
                "Medidas provisórias têm força de lei assim que publicadas e perdem a validade se o Congresso não as "
                        + "aprovar em até 120 dias.");
        SerieMandato mpvLei = nova(m, TemaMandato.LEIS, "Medidas provisórias que viraram lei", "", FormaResumo.SOMA, null,
                fonte, "Medidas provisórias editadas no ano que o Congresso aprovou e transformou em lei.");
        SerieMandato projetos = nova(m, TemaMandato.LEIS, "Projetos enviados ao Congresso", "", FormaResumo.SOMA, null,
                fonte, "Projetos de lei, de lei complementar e propostas de emenda à Constituição do Poder Executivo.");
        SerieMandato projetosLei = nova(m, TemaMandato.LEIS, "Projetos do governo aprovados", "", FormaResumo.SOMA, null,
                fonte, "Projetos enviados no ano que viraram lei ou foram aprovados na Câmara e seguiram para o Senado.");
        for (int ano = m.getAnoEleicao() + 1; ano <= m.getUltimoAno(); ano++) {
            int[] n = contar(ano, m);
            if (n == null) {
                continue;
            }
            mpv.adicionar(ano, n[0], null);
            mpvLei.adicionar(ano, n[1], null);
            projetos.adicionar(ano, n[2], null);
            projetosLei.adicionar(ano, n[3], null);
        }
        List<SerieMandato> series = new ArrayList<>();
        for (SerieMandato s : new SerieMandato[]{mpv, mpvLei, projetos, projetosLei}) {
            if (!s.isVazia()) {
                series.add(s);
            }
        }
        return series;
    }

    /** @return {MPs, MPs que viraram lei, projetos, projetos aprovados} do ano, ou null se faltar arquivo */
    int[] contar(int ano, MandatoExecutivo m) {
        Path autores = brutos.resolve(FontesDados.nomeLocal(FontesDados.autores(ano)));
        Path props = brutos.resolve(FontesDados.nomeLocal(FontesDados.proposicoes(ano)));
        if (!Files.exists(autores) || !Files.exists(props)) {
            return null;
        }
        try {
            Set<String> doExecutivo = new HashSet<>();
            try (LeitorCsv csv = ArquivosBrutos.abrir(autores, StandardCharsets.UTF_8, ".csv")) {
                int iProp = csv.indice("idProposicao");
                int iCod = csv.indiceOpcional("codTipoAutor");
                int iTipo = csv.indiceOpcional("tipoAutor");
                int iNome = csv.indiceOpcional("nomeAutor");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    boolean executivo = "30000".equals(LeitorCsv.campo(l, iCod))
                            || Texto.normalizar(LeitorCsv.campo(l, iTipo)).contains("PODER EXECUTIVO")
                            || Texto.normalizar(LeitorCsv.campo(l, iNome)).equals("PODER EXECUTIVO");
                    if (executivo) {
                        doExecutivo.add(LeitorCsv.campo(l, iProp));
                    }
                }
            }
            int[] n = new int[4];
            try (LeitorCsv csv = ArquivosBrutos.abrir(props, StandardCharsets.UTF_8, ".csv")) {
                int iId = csv.indice("id");
                int iTipo = csv.indice("siglaTipo");
                int iData = csv.indiceOpcional("dataApresentacao");
                int iSit = csv.indiceOpcional("ultimoStatus_descricaoSituacao");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    if (!doExecutivo.contains(LeitorCsv.campo(l, iId))) {
                        continue;
                    }
                    LocalDate data = Texto.parseData(LeitorCsv.campo(l, iData));
                    if (data != null && data.getYear() != ano) {
                        continue;
                    }
                    String tipo = LeitorCsv.campo(l, iTipo).toUpperCase();
                    boolean aprovada = ProcessadorCamara.situacaoAprovada(LeitorCsv.campo(l, iSit));
                    if (tipo.equals("MPV")) {
                        n[0]++;
                        n[1] += aprovada ? 1 : 0;
                    } else if (PROJETOS.contains(tipo)) {
                        n[2]++;
                        n[3] += aprovada ? 1 : 0;
                    }
                }
            }
            return n;
        } catch (ArquivoInvalidoException e) {
            log.accept("  aviso: projetos do Executivo " + ano + ": " + e.getMessage());
            return null;
        }
    }
}
