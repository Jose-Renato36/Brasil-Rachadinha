package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Financiamento;
import br.unit.eleicao.modelo.IndicadorFiscal;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.ResumoEmendas;
import br.unit.eleicao.modelo.Sancao;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.modelo.VinculoServidor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Preenche a base de demonstração com patrimônio ao longo do tempo, financiamento, emendas, empresas,
 * sanções, serviço público e gestão fiscal. Tudo FICTÍCIO (empresas "EXEMPLO", cidades "FICTÍCIA").
 */
class ComplementosDemo {

    private static final String[] AREAS = {"Saúde", "Educação", "Urbanismo", "Agricultura", "Assistência social",
        "Saneamento"};
    private static final String[] RAMOS = {"COMÉRCIO", "CONSTRUÇÕES", "CONSULTORIA", "AGROPECUÁRIA", "TRANSPORTES",
        "EDUCAÇÃO"};

    private final Random rnd;

    ComplementosDemo(long semente) {
        this.rnd = new Random(semente + 1);
    }

    void preencher(BaseDados base) {
        Metadados meta = base.getMetadados();
        for (String f : new String[]{Metadados.FONTE_CASSACAO, Metadados.FONTE_BENS_ANTERIORES, Metadados.FONTE_RECEITAS,
                Metadados.FONTE_EMENDAS, Metadados.FONTE_EMPRESAS, Metadados.FONTE_SANCOES, Metadados.FONTE_SIAPE,
                Metadados.FONTE_SICONFI}) {
            meta.marcarVerificada(f);
        }
        List<Candidato> todos = new ArrayList<>(base.getCandidatos());
        todos.sort(Comparator.comparing(Candidato::getSq));
        int n = 0;
        for (Candidato c : todos) {
            patrimonioAnterior(c);
            financiamento(c);
            if (EmendasParlamentares.foiParlamentarFederal(c)) {
                emendas(c);
            }
            if (n % 4 == 1) {
                empresa(c, n);
            }
            if (n % 9 == 3) {
                c.adicionarVinculoServidor(new VinculoServidor(n % 2 == 0 ? "PROFESSOR DO MAGISTÉRIO SUPERIOR"
                        : "TÉCNICO DO SEGURO SOCIAL", n % 2 == 0 ? "UNIVERSIDADE FEDERAL FICTÍCIA" : "INSTITUTO FICTÍCIO",
                        n % 5 == 0 ? "CEDIDO/REQUISITADO" : "ATIVO PERMANENTE", "0" + (1 + n % 9) + "/03/" + (2005 + n % 12)));
            }
            gestaoFiscal(c, meta.getAnoEleicao());
            n++;
        }
        // um caso de sanção em empresa e um de cassação, para mostrar os alertas
        for (Candidato c : todos) {
            if (!c.getEmpresas().isEmpty() && c.getContasIrregulares().isEmpty()) {
                VinculoEmpresa e = c.getEmpresas().get(0);
                c.adicionarSancao(new Sancao("CEIS", e.getRazaoSocial(), "Impedimento/proibição de contratar (FICTÍCIO)",
                        "PREFEITURA DE CIDADE FICTÍCIA", "12/03/2024", "11/03/2026", true));
                break;
            }
        }
        for (Candidato c : todos) {
            for (CandidaturaAnterior t : c.getTrajetoria()) {
                if (t.getTipoCargo() == Cargo.VEREADOR && t.isEleito()) {
                    t.setMotivoCassacao("Captação ilícita de sufrágio (FICTÍCIO)");
                    return;
                }
            }
        }
    }

    private void patrimonioAnterior(Candidato c) {
        double atual = c.getPatrimonio() == null ? 0 : c.getPatrimonio();
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            if (t.getAno() == 2022 && c.getPatrimonioAnterior() != null) {
                t.setPatrimonio(c.getPatrimonioAnterior()); // mesma declaração usada no indicador
                continue;
            }
            double fator = Math.pow(0.75 + 0.45 * rnd.nextDouble(), (2026 - t.getAno()) / 2.0);
            t.setPatrimonio((double) Math.round(atual * fator / 100) * 100);
            if (t.getAno() == 2022) {
                c.setPatrimonioAnterior(t.getPatrimonio()); // como na coleta real: mesma declaração de 2022
            }
        }
    }

    private void financiamento(Candidato c) {
        double escala = c.getTipoCargo() == Cargo.PRESIDENTE ? 5e7 : c.getTipoCargo() == Cargo.GOVERNADOR ? 8e6
                : c.getTipoCargo() == Cargo.SENADOR ? 4e6 : c.getTipoCargo() == Cargo.DEPUTADO_FEDERAL ? 1.5e6 : 3e5;
        Financiamento f = new Financiamento();
        double total = escala * (0.2 + rnd.nextDouble());
        double fundo = 0.35 + 0.5 * rnd.nextDouble();
        double proprio = rnd.nextDouble() < 0.3 ? 0.15 * rnd.nextDouble() : 0;
        double pessoas = (1 - fundo - proprio) * (0.4 + 0.5 * rnd.nextDouble());
        double partido = Math.max(0, 1 - fundo - proprio - pessoas) * 0.8;
        f.somar("Fundo eleitoral (dinheiro público)", Math.round(total * fundo));
        if (pessoas > 0) {
            f.somar("Doações de pessoas", Math.round(total * pessoas));
        }
        if (proprio > 0) {
            f.somar("Dinheiro do próprio candidato", Math.round(total * proprio));
        }
        if (partido > 0.01) {
            f.somar("Repasses de partidos e outros candidatos", Math.round(total * partido));
        }
        if (rnd.nextDouble() < 0.3) {
            f.somar("Vaquinha on-line", Math.round(total * 0.03));
        }
        c.setFinanciamento(f);
    }

    private void emendas(Candidato c) {
        ResumoEmendas r = new ResumoEmendas(c.getNomeUrna().toUpperCase());
        int quantidade = 20 + rnd.nextInt(40);
        for (int i = 0; i < quantidade; i++) {
            int ano = 2019 + rnd.nextInt(7);
            double empenhado = Math.round(100_000 + 900_000 * rnd.nextDouble());
            double pago = rnd.nextDouble() < 0.8 ? Math.round(empenhado * (0.5 + 0.5 * rnd.nextDouble())) : 0;
            r.somar(ano, "CIDADE FICTÍCIA " + (1 + rnd.nextInt(6)) + " - " + c.getUf(), AREAS[rnd.nextInt(AREAS.length)],
                    empenhado, pago, rnd.nextDouble() < 0.35);
        }
        c.setEmendas(r);
    }

    private void empresa(Candidato c, int n) {
        String ramo = RAMOS[n % RAMOS.length];
        c.adicionarEmpresa(new VinculoEmpresa(String.format("99%06d", n), ramo + " EXEMPLO " + (n + 1) + " LTDA (FICTÍCIA)",
                n % 3 == 0 ? "Sócio" : "Sócio-Administrador", "15/0" + (1 + n % 9) + "/" + (2008 + n % 14)));
        if (n % 3 == 0) {
            c.adicionarEmpresa(new VinculoEmpresa(String.format("98%06d", n), "HOLDING EXEMPLO " + (n + 1)
                    + " S.A. (FICTÍCIA)", "Administrador", "02/10/2019"));
        }
    }

    private void gestaoFiscal(Candidato c, int anoEleicao) {
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            Cargo cargo = t.getTipoCargo();
            if (!t.isEleito() || (cargo != Cargo.PREFEITO && cargo != Cargo.GOVERNADOR)) {
                continue;
            }
            boolean municipal = cargo == Cargo.PREFEITO;
            String ente = (municipal ? "Prefeitura de " : "Governo de ") + (t.getLocal().isEmpty() ? c.getUf() : t.getLocal());
            double limite = municipal ? 54.0 : 49.0;
            double valor = limite - 6 + 8 * rnd.nextDouble();
            double tendencia = (rnd.nextDouble() - 0.5) * 3;
            for (int ano = t.getAno(); ano <= Math.min(t.getFimMandato(), anoEleicao - 1); ano++) {
                c.adicionarIndicadorFiscal(new IndicadorFiscal(ente, ano, Math.round(valor * 100) / 100.0, limite,
                        ano > t.getAno()));
                valor += tendencia + rnd.nextGaussian();
            }
        }
    }
}
