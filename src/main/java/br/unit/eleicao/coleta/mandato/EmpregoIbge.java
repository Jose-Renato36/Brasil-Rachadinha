package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Empregos formais (pessoal ocupado assalariado em 31/12, variável 708) do Cadastro Central de
 * Empresas do IBGE: tabela 1685 até 2021 e tabela 9509 a partir de 2022 (nova metodologia).
 * Inclui empresas e órgãos públicos.
 */
public class EmpregoIbge extends FonteIbge {

    static final int TABELA_ATE_2021 = 1685;
    static final int TABELA_DESDE_2022 = 9509;
    static final String VARIAVEL = "708";

    public EmpregoIbge(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    @Override
    public String getNome() {
        return "ibge-emprego";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isMunicipal() || m.isEstadual();
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        Map<Integer, Double> proprio = serie(m.getCodigoIbge());
        String codRef = codigoReferencia(m);
        SerieMandato s = nova(m, TemaMandato.EMPREGO, "Empregos formais", "empregos", FormaResumo.VARIACAO_PERCENTUAL,
                true, "IBGE – Cadastro Central de Empresas (CEMPRE)",
                "Pessoas com vínculo formal (carteira assinada ou servidor público) em 31 de dezembro de cada ano. "
                        + "O IBGE mudou a metodologia em 2022; a referência mudou junto, então a comparação continua válida.");
        s.setReferenciaNome(nomeReferencia(m));
        preencher(s, m, proprio, codRef == null ? null : serie(codRef), 1);
        return s.isVazia() ? List.of() : List.of(s);
    }

    private Map<Integer, Double> serie(String codigo) {
        Map<Integer, Double> anos = new TreeMap<>();
        anos.putAll(ApiIbge.anual(consultar(TABELA_ATE_2021, VARIAVEL, codigo).getOrDefault(VARIAVEL, Map.of())));
        anos.putAll(ApiIbge.anual(consultar(TABELA_DESDE_2022, VARIAVEL, codigo).getOrDefault(VARIAVEL, Map.of())));
        return anos;
    }
}
