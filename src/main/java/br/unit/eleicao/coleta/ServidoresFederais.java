package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.VinculoServidor;
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
 * Servidores civis do Executivo federal (SIAPE), publicados mensalmente no Portal da Transparência
 * (zip AAAAMM_Servidores_SIAPE com a entrada AAAAMM_Cadastro.csv). CPF mascarado: ligação por nome
 * completo + dígitos visíveis do CPF. Mostra se o candidato é (ou foi, se aposentado) servidor e onde.
 */
public class ServidoresFederais {

    public static final String ARQUIVO = "servidores_siape.zip";

    private final Path pasta;
    private final Consumer<String> log;

    public ServidoresFederais(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    /** @return true se o arquivo existia e foi lido */
    public boolean processar(List<Candidato> candidatos, Map<String, String> cpfPorSq) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(ARQUIVO);
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + ARQUIVO + " ausente - vínculo com o serviço público federal não verificado");
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
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, EmendasParlamentares.WINDOWS_1252, "", "_cadastro.csv", ';')) {
            int iNome = csv.indice("NOME");
            int iCpf = csv.indice("CPF");
            int iCargo = csv.indiceOpcional("DESCRICAO_CARGO");
            int iFuncao = csv.indiceOpcional("FUNCAO");
            int iOrgao = csv.indiceOpcional("ORG_EXERCICIO", "ORG_LOTACAO");
            int iSituacao = csv.indiceOpcional("SITUACAO_VINCULO");
            int iIngresso = csv.indiceOpcional("DATA_INGRESSO_SERVICOPUBLICO", "DATA_INGRESSO_ORGAO");
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
                    String cargo = Texto.limparTse(LeitorCsv.campo(l, iCargo));
                    if (cargo.isEmpty()) {
                        cargo = Texto.limparTse(LeitorCsv.campo(l, iFuncao));
                    }
                    String orgao = Texto.limparTse(LeitorCsv.campo(l, iOrgao));
                    if (vistos.add(c.getSq() + "|" + cargo + "|" + orgao)) {
                        c.adicionarVinculoServidor(new VinculoServidor(cargo, orgao,
                                Texto.limparTse(LeitorCsv.campo(l, iSituacao)), LeitorCsv.campo(l, iIngresso)));
                        n++;
                    }
                }
            }
        }
        log.accept("Servidores federais: " + n + " vínculo(s) ligados a candidatos.");
        return true;
    }
}
