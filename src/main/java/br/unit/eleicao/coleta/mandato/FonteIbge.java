package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.SerieMandato;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base das fontes do IBGE: consulta a API de agregados para o lugar governado e para a referência
 * (o estado, no caso de prefeituras; o Brasil, no caso de estados).
 */
public abstract class FonteIbge extends FonteMandato {

    protected FonteIbge(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    protected Map<String, Map<String, Double>> consultar(int tabela, String variaveis, String codigo) {
        String json = obterTexto(ApiIbge.url(tabela, variaveis, ApiIbge.nivel(codigo), codigo),
                "t" + tabela + "_v" + variaveis.replace('|', '-') + "_" + codigo + ".json");
        return ApiIbge.ler(json);
    }

    /** Código da referência: UF para município, Brasil ("1") para UF; null para o país. */
    protected static String codigoReferencia(MandatoExecutivo m) {
        if (m.isMunicipal()) {
            return CodigosIbge.CODIGO_UF.get(m.getUf());
        }
        return m.isEstadual() ? "1" : null;
    }

    protected static String nomeReferencia(MandatoExecutivo m) {
        if (m.isMunicipal()) {
            String nome = CodigosIbge.NOME_UF.get(m.getUf());
            return nome == null ? "estado" : nome;
        }
        return "Brasil";
    }

    /** Preenche a série com os anos do mandato, usando a referência quando houver. */
    protected static void preencher(SerieMandato s, MandatoExecutivo m, Map<Integer, Double> proprio,
                                    Map<Integer, Double> referencia, double fator) {
        for (Map.Entry<Integer, Double> e : proprio.entrySet()) {
            if (m.contem(e.getKey())) {
                Double ref = referencia == null ? null : referencia.get(e.getKey());
                s.adicionar(e.getKey(), e.getValue() * fator, ref == null ? null : ref * fator);
            }
        }
    }
}
