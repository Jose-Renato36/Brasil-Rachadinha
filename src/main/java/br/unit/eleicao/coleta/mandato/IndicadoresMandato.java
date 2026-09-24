package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.ArquivosBrutos;
import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.coleta.FontesDados;
import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Antes e depois" de quem já governou: para cada mandato de prefeito(a), governador(a) ou presidente
 * encontrado na trajetória, aplica todas as fontes (polimorfismo: a lista guarda {@link FonteMandato})
 * e liga as séries ao candidato.
 */
public class IndicadoresMandato {

    private final Path brutos;
    private final Consumer<String> log;
    private final int anoEleicao;
    private final List<FonteMandato> fontes = new ArrayList<>();

    /** @param downloader null = só usa respostas já guardadas */
    public IndicadoresMandato(Path brutos, Downloader downloader, Consumer<String> log, int anoEleicao) {
        this.brutos = brutos;
        this.log = log;
        this.anoEleicao = anoEleicao;
        fontes.add(new PibIbge(brutos, downloader, log));
        fontes.add(new EmpregoIbge(brutos, downloader, log));
        fontes.add(new DesempregoIbge(brutos, downloader, log));
        fontes.add(new ContasSiconfi(brutos, downloader, log));
        fontes.add(new IdebInep(brutos, downloader, log, anoEleicao));
        fontes.add(new EconomiaNacionalBcb(brutos, downloader, log));
        fontes.add(new ProjetosExecutivo(brutos, downloader, log));
    }

    /** @return true se algum mandato recebeu indicadores */
    public boolean processar(List<Candidato> candidatos) throws ArquivoInvalidoException {
        Map<String, String> ibgePorTse = lerMunicipios();
        Map<String, List<SerieMandato>> cache = new HashMap<>();
        int mandatos = 0;
        int series = 0;
        for (Candidato c : candidatos) {
            for (CandidaturaAnterior t : c.getTrajetoria()) {
                MandatoExecutivo m = mandato(c, t, ibgePorTse);
                if (m == null) {
                    continue;
                }
                List<SerieMandato> achadas = cache.get(m.getChave());
                if (achadas == null) {
                    achadas = new ArrayList<>();
                    for (FonteMandato f : fontes) {
                        if (f.aplica(m)) {
                            try {
                                achadas.addAll(f.coletar(m));
                            } catch (RuntimeException e) {
                                log.accept("  aviso: " + f.getNome() + " falhou para " + m.getChave() + ": " + e.getMessage());
                            }
                        }
                    }
                    cache.put(m.getChave(), achadas);
                }
                if (!achadas.isEmpty()) {
                    mandatos++;
                }
                for (SerieMandato s : achadas) {
                    c.adicionarSerieMandato(s);
                    series++;
                }
            }
        }
        log.accept("Antes e depois: " + mandatos + " mandato(s) no Executivo com indicadores (" + series + " séries).");
        return mandatos > 0;
    }

    /** Monta o mandato a partir de uma candidatura vencedora a prefeito, governador ou presidente. */
    MandatoExecutivo mandato(Candidato c, CandidaturaAnterior t, Map<String, String> ibgePorTse) {
        Cargo cargo = t.getTipoCargo();
        if (!t.isEleito() || (cargo != Cargo.PREFEITO && cargo != Cargo.GOVERNADOR && cargo != Cargo.PRESIDENTE)) {
            return null;
        }
        int ultimoFechado = anoEleicao - 1;
        if (t.getAno() >= ultimoFechado) {
            return null; // mandato começou agora: ainda não há ano completo
        }
        if (cargo == Cargo.PRESIDENTE) {
            return new MandatoExecutivo(cargo, "Governo federal", "1", "BR", t.getAno(), t.getFimMandato(), ultimoFechado);
        }
        if (cargo == Cargo.GOVERNADOR) {
            String uf = t.getLocal().isEmpty() ? c.getUf() : t.getLocal();
            String cod = CodigosIbge.CODIGO_UF.get(uf);
            String nome = CodigosIbge.NOME_UF.get(uf);
            return cod == null ? null
                    : new MandatoExecutivo(cargo, "Governo de " + (nome == null ? uf : nome), cod, uf, t.getAno(),
                    t.getFimMandato(), ultimoFechado);
        }
        String cod = ibgePorTse.get(Texto.somenteDigitos(t.getCodigoUe()).replaceFirst("^0+", ""));
        if (cod == null) {
            return null;
        }
        String local = t.getLocal();
        String uf = local.contains("/") ? local.substring(local.lastIndexOf('/') + 1) : c.getUf();
        return new MandatoExecutivo(cargo, "Prefeitura de " + local, cod, uf, t.getAno(), t.getFimMandato(), ultimoFechado);
    }

    /** Código do município no TSE (sem zeros à esquerda) → código IBGE de 7 dígitos. */
    private Map<String, String> lerMunicipios() throws ArquivoInvalidoException {
        Map<String, String> mapa = new HashMap<>();
        Path arquivo = brutos.resolve(FontesDados.nomeLocal(FontesDados.municipiosTseIbge()));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - prefeituras sem indicadores");
            return mapa;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, "", "", ',')) {
            int iTse = csv.indice("codigo_tse");
            int iIbge = csv.indice("codigo_ibge");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String tse = LeitorCsv.campo(l, iTse).replaceFirst("^0+", "");
                String ibge = LeitorCsv.campo(l, iIbge);
                if (!tse.isEmpty() && !ibge.isEmpty()) {
                    mapa.put(tse, ibge);
                }
            }
        }
        return mapa;
    }
}
