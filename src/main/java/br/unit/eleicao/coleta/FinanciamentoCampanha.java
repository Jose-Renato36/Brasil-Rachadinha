package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Financiamento;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lê as receitas declaradas pelas campanhas (prestação de contas ao TSE) e soma por origem:
 * fundo eleitoral, fundo partidário, recursos próprios, doações de pessoas, vaquinha etc.
 * Arquivo: prestacao_de_contas_eleitorais_candidatos_ANO.zip, entrada receitas_candidatos_ANO_UF.csv.
 */
public class FinanciamentoCampanha {

    private final Path pasta;
    private final String uf;
    private final Consumer<String> log;

    public FinanciamentoCampanha(Path pasta, String uf, Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf.toUpperCase();
        this.log = log;
    }

    /** @return true se o arquivo existia e foi lido */
    public boolean processar(List<Candidato> candidatos, int ano) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.prestacaoContas(ano)));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - sem dados de financiamento de campanha");
            return false;
        }
        Map<String, Candidato> porSq = new HashMap<>();
        for (Candidato c : candidatos) {
            porSq.put(c.getSq(), c);
        }
        Map<String, Financiamento> achados = new HashMap<>();
        java.util.Set<String> vistas = new java.util.HashSet<>();
        String prefixo = "receitas_candidatos_" + ano;
        for (String sufixo : ArquivosBrutos.sufixos(uf)) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(arquivo, ProcessadorTSE.LATIN1, prefixo, sufixo, ';');
            if (aberto == null) {
                continue;
            }
            try (LeitorCsv csv = aberto) {
                int iSq = csv.indice("SQ_CANDIDATO");
                int iFonte = csv.indiceOpcional("DS_FONTE_RECEITA");
                int iOrigem = csv.indice("DS_ORIGEM_RECEITA");
                int iValor = csv.indice("VR_RECEITA");
                int iId = csv.indiceOpcional("SQ_RECEITA");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    String sq = LeitorCsv.campo(l, iSq);
                    if (!porSq.containsKey(sq)) {
                        continue;
                    }
                    Double valor = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                    String id = LeitorCsv.campo(l, iId);
                    // no modo nacional a mesma receita pode vir em _BRASIL e _BR
                    if (valor == null || (!id.isEmpty() && !vistas.add(sq + "|" + id))) {
                        continue;
                    }
                    String categoria = Financiamento.origemLegivel(Texto.limparTse(LeitorCsv.campo(l, iFonte)) + " "
                            + Texto.limparTse(LeitorCsv.campo(l, iOrigem)));
                    achados.computeIfAbsent(sq, k -> new Financiamento()).somar(categoria, valor);
                }
            }
        }
        for (Map.Entry<String, Financiamento> e : achados.entrySet()) {
            porSq.get(e.getKey()).setFinanciamento(e.getValue());
        }
        log.accept("Financiamento: " + achados.size() + " candidaturas com receitas declaradas.");
        lerDespesas(arquivo, ano, porSq);
        return true;
    }

    private boolean despesasLidas;

    /** true se o arquivo de despesas contratadas foi encontrado dentro do zip. */
    public boolean isDespesasLidas() {
        return despesasLidas;
    }

    /** Despesas contratadas (despesas_contratadas_candidatos_ANO_UF.csv), somadas por tipo em linguagem simples. */
    private void lerDespesas(Path arquivo, int ano, Map<String, Candidato> porSq) throws ArquivoInvalidoException {
        Map<String, Financiamento> achados = new HashMap<>();
        java.util.Set<String> vistas = new java.util.HashSet<>();
        for (String sufixo : ArquivosBrutos.sufixos(uf)) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(arquivo, ProcessadorTSE.LATIN1,
                    "despesas_contratadas_candidatos_" + ano, sufixo, ';');
            if (aberto == null) {
                continue;
            }
            despesasLidas = true;
            try (LeitorCsv csv = aberto) {
                int iSq = csv.indice("SQ_CANDIDATO");
                int iOrigem = csv.indice("DS_ORIGEM_DESPESA");
                int iValor = csv.indice("VR_DESPESA_CONTRATADA");
                int iId = csv.indiceOpcional("SQ_DESPESA");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    String sq = LeitorCsv.campo(l, iSq);
                    if (!porSq.containsKey(sq)) {
                        continue;
                    }
                    Double valor = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                    String id = LeitorCsv.campo(l, iId);
                    if (valor == null || (!id.isEmpty() && !vistas.add(sq + "|" + id))) {
                        continue;
                    }
                    achados.computeIfAbsent(sq, k -> new Financiamento())
                            .somar(tipoDespesa(Texto.limparTse(LeitorCsv.campo(l, iOrigem))), valor);
                }
            }
        }
        for (Map.Entry<String, Financiamento> e : achados.entrySet()) {
            porSq.get(e.getKey()).setDespesasCampanha(e.getValue());
        }
        log.accept("Despesas de campanha: " + achados.size() + " candidaturas com gastos declarados.");
    }

    /** Agrupa os ~40 tipos de despesa do TSE em poucas categorias que o eleitor entende. */
    public static String tipoDespesa(String dsOrigem) {
        String t = Texto.normalizar(dsOrigem);
        if (t.contains("IMPULSIONAMENTO") || t.contains("INTERNET") || t.contains("SITIO")) {
            return "Anúncios na internet";
        }
        if (t.contains("IMPRESSO") || t.contains("PUBLICIDADE") || t.contains("ADESIVO") || t.contains("PLACA")
                || t.contains("JORNAIS") || t.contains("CARRO DE SOM") || t.contains("MATERIAL")) {
            return "Propaganda (impressos, adesivos, carro de som)";
        }
        if (t.contains("AUDIOVISUAL") || t.contains("PRODUCAO DE PROGRAMAS") || t.contains("RADIO")
                || t.contains("TELEVISAO") || t.contains("VIDEO")) {
            return "Produção de vídeo, rádio e TV";
        }
        if (t.contains("PESSOAL") || t.contains("MILITANCIA") || t.contains("MOBILIZACAO")) {
            return "Pessoal e cabos eleitorais";
        }
        if (t.contains("COMBUSTIVEL") || t.contains("TRANSPORTE") || t.contains("VEICULO") || t.contains("PASSAGEM")
                || t.contains("DESLOCAMENTO") || t.contains("HOSPEDAGEM") || t.contains("ALIMENTACAO")) {
            return "Viagens, transporte e alimentação";
        }
        if (t.contains("ADVOCATIC") || t.contains("CONTAB") || t.contains("SERVICOS PRESTADOS POR TERCEIROS")
                || t.contains("CONSULTORIA") || t.contains("PESQUISA")) {
            return "Serviços (advogados, contadores, pesquisas)";
        }
        if (t.contains("EVENTO") || t.contains("COMICIO") || t.contains("LOCACAO") || t.contains("ALUGUEL")
                || t.contains("CESSAO")) {
            return "Eventos, aluguéis e espaços";
        }
        if (t.contains("DOACOES FINANCEIRAS") || t.contains("OUTROS CANDIDATOS") || t.contains("PARTIDO")) {
            return "Repasses a outros candidatos e partidos";
        }
        return t.isEmpty() ? "Não informado" : "Outras despesas";
    }
}
