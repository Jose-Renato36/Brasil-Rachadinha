package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.CargoPublico;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pessoas Expostas Politicamente (CGU / Portal da Transparência): quem exerce ou exerceu nos últimos
 * 5 anos função pública de destaque (ministro, secretário de estado, dirigente de estatal, cargos de
 * confiança de alto nível, mandatos eletivos). Arquivo mensal AAAAMM_PEP.csv, ';', ISO-8859-1, colunas
 * CPF (mascarado), Nome_PEP, Sigla_Função, Descrição_Função, Nível_Função, Nome_Órgão,
 * Data_Início_Exercício, Data_Fim_Exercício, Data_Fim_Carência. Ligação por nome + CPF parcial.
 */
public class CargosPublicosPep {

    public static final String ARQUIVO = "pep.zip";

    private final Path pasta;
    private final Consumer<String> log;

    public CargosPublicosPep(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    /** @return true se o arquivo existia e foi lido */
    public boolean processar(List<Candidato> candidatos, Map<String, String> cpfPorSq) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(ARQUIVO);
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + ARQUIVO + " ausente - cargos públicos de destaque não verificados");
            return false;
        }
        Map<String, List<Candidato>> porNome = new HashMap<>();
        for (Candidato c : candidatos) {
            String cpf = cpfPorSq.get(c.getSq());
            if (cpf != null && cpf.length() == 11 && !Texto.vazio(c.getNome())) {
                porNome.computeIfAbsent(Texto.normalizar(c.getNome()), k -> new ArrayList<>()).add(c);
            }
        }
        Set<String> vistos = new HashSet<>();
        int n = 0;
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, ProcessadorTSE.LATIN1, "", ".csv", ';')) {
            int iCpf = csv.indice("CPF");
            int iNome = csv.indice("NOME_PEP");
            int iFuncao = csv.indiceOpcional("DESCRIÇÃO_FUNÇÃO", "DESCRICAO_FUNCAO");
            int iOrgao = csv.indiceOpcional("NOME_ÓRGÃO", "NOME_ORGAO");
            int iInicio = csv.indiceOpcional("DATA_INÍCIO_EXERCÍCIO", "DATA_INICIO_EXERCICIO");
            int iFim = csv.indiceOpcional("DATA_FIM_EXERCÍCIO", "DATA_FIM_EXERCICIO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                List<Candidato> achados = porNome.get(Texto.normalizar(LeitorCsv.campo(l, iNome)));
                if (achados == null) {
                    continue;
                }
                for (Candidato c : achados) {
                    if (!Texto.cpfCompativel(cpfPorSq.get(c.getSq()), LeitorCsv.campo(l, iCpf))) {
                        continue;
                    }
                    String funcao = Texto.limparTse(LeitorCsv.campo(l, iFuncao));
                    String orgao = Texto.limparTse(LeitorCsv.campo(l, iOrgao));
                    String inicio = LeitorCsv.campo(l, iInicio);
                    if (vistos.add(c.getSq() + "|" + funcao + "|" + orgao + "|" + inicio)) {
                        c.adicionarCargoPublico(new CargoPublico(funcao, orgao, inicio,
                                Texto.limparTse(LeitorCsv.campo(l, iFim))));
                        n++;
                    }
                }
            }
        }
        log.accept("Cargos públicos de destaque (PEP): " + n + " ligados a candidatos.");
        return true;
    }
}
