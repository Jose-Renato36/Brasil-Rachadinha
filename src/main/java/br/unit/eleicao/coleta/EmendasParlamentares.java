package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.ResumoEmendas;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Emendas individuais ao Orçamento da União (Portal da Transparência / CGU, EmendasParlamentares.csv).
 * O arquivo identifica o autor pelo nome parlamentar, sem CPF: por isso a ligação só é feita para quem
 * foi deputado(a) federal ou senador(a) e quando o nome aponta para uma única candidatura.
 * Emendas de bancada, comissão e relator ficam de fora (o "autor" não é uma pessoa ou é o relator-geral).
 */
public class EmendasParlamentares {

    public static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final Path pasta;
    private final Consumer<String> log;

    public EmendasParlamentares(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    /** @return true se o arquivo existia e foi lido */
    public boolean processar(BaseDados base) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.emendas()));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - sem emendas parlamentares");
            return false;
        }
        Map<String, List<Candidato>> porNome = new HashMap<>();
        for (Candidato c : base.getCandidatos()) {
            if (!foiParlamentarFederal(c)) {
                continue;
            }
            java.util.Set<String> nomes = new java.util.HashSet<>();
            nomes.add(Texto.normalizar(c.getNomeUrna()));
            if (c.getIdDeputado() != null) {
                Deputado d = base.getDeputado(c.getIdDeputado());
                if (d != null) {
                    nomes.add(Texto.normalizar(d.getNomeParlamentar()));
                }
            }
            for (String n : nomes) {
                if (!n.isEmpty()) {
                    porNome.computeIfAbsent(n, k -> new ArrayList<>()).add(c);
                }
            }
        }
        Map<Candidato, ResumoEmendas> resumos = new HashMap<>();
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, WINDOWS_1252, "", ".csv", ';')) {
            int iAutor = csv.indice("NOME DO AUTOR DA EMENDA");
            int iTipo = csv.indice("TIPO DE EMENDA");
            int iAno = csv.indice("ANO DA EMENDA");
            int iLocal = csv.indiceOpcional("LOCALIDADE DE APLICAÇÃO DO RECURSO", "LOCALIDADE DE APLICACAO DO RECURSO");
            int iFuncao = csv.indiceOpcional("NOME FUNÇÃO", "NOME FUNCAO");
            int iEmpenhado = csv.indice("VALOR EMPENHADO");
            int iPago = csv.indice("VALOR PAGO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String tipo = Texto.normalizar(LeitorCsv.campo(l, iTipo));
                if (!tipo.contains("INDIVIDUAL")) {
                    continue;
                }
                String autor = LeitorCsv.campo(l, iAutor);
                List<Candidato> achados = porNome.get(Texto.normalizar(autor));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                if (achados == null || achados.size() != 1 || ano == null) {
                    continue;
                }
                Candidato c = achados.get(0);
                Double emp = Texto.parseDecimal(LeitorCsv.campo(l, iEmpenhado));
                Double pago = Texto.parseDecimal(LeitorCsv.campo(l, iPago));
                resumos.computeIfAbsent(c, k -> new ResumoEmendas(autor))
                        .somar(ano, LeitorCsv.campo(l, iLocal), LeitorCsv.campo(l, iFuncao),
                                emp == null ? 0 : emp, pago == null ? 0 : pago, tipo.contains("ESPECIA"));
            }
        }
        for (Map.Entry<Candidato, ResumoEmendas> e : resumos.entrySet()) {
            e.getKey().setEmendas(e.getValue());
        }
        log.accept("Emendas: " + resumos.size() + " candidaturas ligadas a emendas individuais.");
        return true;
    }

    /** Emendas individuais ao orçamento federal só existem para deputados federais e senadores. */
    static boolean foiParlamentarFederal(Candidato c) {
        if (c.getIdDeputado() != null) {
            return true;
        }
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            Cargo cargo = t.getTipoCargo();
            if (t.isEleito() && (cargo == Cargo.DEPUTADO_FEDERAL || cargo == Cargo.SENADOR)) {
                return true;
            }
        }
        return !c.getAtuacao().isEmpty();
    }
}
