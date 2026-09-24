package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Taxa de desemprego (PNAD Contínua, tabela 4099, variável 4099), média dos 4 trimestres de cada ano.
 * Existe para estados e para o Brasil; não existe por município.
 */
public class DesempregoIbge extends FonteIbge {

    static final int TABELA = 4099;

    public DesempregoIbge(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    @Override
    public String getNome() {
        return "ibge-desemprego";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isEstadual() || m.isFederal();
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        String codigo = m.isFederal() ? "1" : m.getCodigoIbge();
        Map<Integer, Double> proprio = anual(codigo);
        SerieMandato s = nova(m, TemaMandato.EMPREGO, "Desemprego", "%", FormaResumo.VARIACAO_PONTOS, false,
                "IBGE – PNAD Contínua", "Pessoas procurando trabalho, em % da força de trabalho; média dos 4 trimestres do ano. "
                        + "Depende muito da economia do país como um todo.");
        if (m.isEstadual()) {
            s.setReferenciaNome("Brasil");
            preencher(s, m, proprio, anual("1"), 1);
        } else {
            preencher(s, m, proprio, null, 1);
        }
        return s.isVazia() ? List.of() : List.of(s);
    }

    private Map<Integer, Double> anual(String codigo) {
        return ApiIbge.mediaAnualDeTrimestres(consultar(TABELA, "4099", codigo).getOrDefault("4099", Map.of()));
    }
}
