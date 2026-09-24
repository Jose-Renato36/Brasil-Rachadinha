package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * PIB dos municípios e das UFs (IBGE, tabela 5938): PIB total (variável 37, em R$ mil) e PIB por
 * pessoa (variável 543, em R$). Valores correntes: a comparação justa é com a referência no mesmo
 * período, que sofre a mesma inflação. O IBGE publica com cerca de 2 anos de atraso.
 */
public class PibIbge extends FonteIbge {

    static final int TABELA = 5938;

    public PibIbge(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    @Override
    public String getNome() {
        return "ibge-pib";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isMunicipal() || m.isEstadual();
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        Map<String, Map<String, Double>> proprio = consultar(TABELA, "37|543", m.getCodigoIbge());
        String codRef = codigoReferencia(m);
        Map<String, Map<String, Double>> ref = codRef == null ? Map.of() : consultar(TABELA, "37|543", codRef);
        List<SerieMandato> series = new ArrayList<>();
        String explicacao = "Valores em reais de cada ano, sem descontar a inflação. Compare com " + nomeReferencia(m)
                + " no mesmo período, que passou pela mesma inflação. O IBGE publica o PIB com cerca de 2 anos de atraso.";
        SerieMandato pib = nova(m, TemaMandato.ECONOMIA, "Crescimento do PIB", "R$", FormaResumo.VARIACAO_PERCENTUAL,
                true, "IBGE – PIB dos Municípios / Contas Regionais", explicacao);
        pib.setReferenciaNome(nomeReferencia(m));
        preencher(pib, m, ApiIbge.anual(proprio.getOrDefault("37", Map.of())),
                ref.containsKey("37") ? ApiIbge.anual(ref.get("37")) : null, 1000);
        SerieMandato porPessoa = nova(m, TemaMandato.ECONOMIA, "PIB por pessoa", "R$", FormaResumo.VARIACAO_PERCENTUAL,
                true, "IBGE – PIB dos Municípios / Contas Regionais", explicacao);
        porPessoa.setReferenciaNome(nomeReferencia(m));
        preencher(porPessoa, m, ApiIbge.anual(proprio.getOrDefault("543", Map.of())),
                ref.containsKey("543") ? ApiIbge.anual(ref.get("543")) : null, 1);
        for (SerieMandato s : new SerieMandato[]{pib, porPessoa}) {
            if (!s.isVazia()) {
                series.add(s);
            }
        }
        return series;
    }
}
