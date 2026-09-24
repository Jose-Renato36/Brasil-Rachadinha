package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Financiamento;
import br.unit.eleicao.modelo.CargoPublico;
import br.unit.eleicao.modelo.ContratoPublico;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
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
                Metadados.FONTE_MANDATOS, Metadados.FONTE_EXPULSOES, Metadados.FONTE_CARGOS_PUBLICOS,
                Metadados.FONTE_CONTRATOS, Metadados.FONTE_PLANOS, Metadados.FONTE_DESPESAS, Metadados.FONTE_VOTOS}) {
            meta.marcarVerificada(f);
        }
        List<Candidato> todos = new ArrayList<>(base.getCandidatos());
        todos.sort(Comparator.comparing(Candidato::getSq));
        int n = 0;
        for (Candidato c : todos) {
            patrimonioAnterior(c);
            financiamento(c);
            despesas(c);
            for (CandidaturaAnterior t : c.getTrajetoria()) {
                long votosBase = t.getTipoCargo() == Cargo.VEREADOR ? 2_000 : t.getTipoCargo().isMunicipal() ? 40_000
                        : t.getTipoCargo() == Cargo.PRESIDENTE ? 30_000_000 : 150_000;
                t.setVotos(Math.round(votosBase * (0.3 + 1.4 * rnd.nextDouble())));
            }
            if (n % 7 == 2) {
                c.adicionarCargoPublico(new CargoPublico(n % 2 == 0 ? "SECRETÁRIO DE ESTADO DE EDUCAÇÃO (FICTÍCIO)"
                        : "DIRETOR-PRESIDENTE DE EMPRESA ESTATAL (FICTÍCIO)", "GOVERNO DO ESTADO FICTÍCIO",
                        "01/02/" + (2019 + n % 3), n % 3 == 0 ? "" : "31/03/2022"));
            }
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
            mandatosExecutivo(c, meta.getAnoEleicao());
            n++;
        }
        // um contrato federal de empresa de candidato e uma expulsão, para mostrar os alertas
        for (Candidato c : todos) {
            if (c.getEmpresas().size() > 1) {
                VinculoEmpresa e = c.getEmpresas().get(0);
                c.adicionarContrato(new ContratoPublico(e.getCnpj() + "000199", e.getRazaoSocial(), "MINISTÉRIO FICTÍCIO",
                        "Prestação de serviços de manutenção predial (FICTÍCIO)", 1_250_000, "14/06/2024"));
                break;
            }
        }
        for (Candidato c : todos) {
            if (!c.getVinculosServidor().isEmpty()) {
                c.adicionarSancao(new Sancao("CEAF", c.getNome(), "Demissão (FICTÍCIO)", "INSTITUTO FICTÍCIO",
                        "03/05/2021", "", false));
                break;
            }
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
        if (c.getTrajetoria().stream().noneMatch(t -> t.getAno() == 2022)) {
            c.setPatrimonioAnterior(null); // como na coleta real: só há "antes" para quem concorreu em 2022
        }
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

    private void despesas(Candidato c) {
        if (c.getFinanciamento() == null) {
            return;
        }
        double total = c.getFinanciamento().getTotal() * (0.6 + 0.35 * rnd.nextDouble());
        String[] tipos = {"Anúncios na internet", "Propaganda (impressos, adesivos, carro de som)",
            "Produção de vídeo, rádio e TV", "Pessoal e cabos eleitorais", "Viagens, transporte e alimentação",
            "Serviços (advogados, contadores, pesquisas)"};
        double[] pesos = new double[tipos.length];
        double soma = 0;
        for (int i = 0; i < tipos.length; i++) {
            pesos[i] = 0.2 + rnd.nextDouble();
            soma += pesos[i];
        }
        Financiamento f = new Financiamento();
        for (int i = 0; i < tipos.length; i++) {
            f.somar(tipos[i], Math.round(total * pesos[i] / soma));
        }
        c.setDespesasCampanha(f);
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

    /** Indicadores de antes e depois para ex-prefeitos, ex-governadores e ex-presidentes (tudo fictício). */
    private void mandatosExecutivo(Candidato c, int anoEleicao) {
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            Cargo cargo = t.getTipoCargo();
            if (!t.isEleito() || t.getAno() >= anoEleicao - 1
                    || (cargo != Cargo.PREFEITO && cargo != Cargo.GOVERNADOR && cargo != Cargo.PRESIDENTE)) {
                continue;
            }
            Mandato m = new Mandato(c, t, anoEleicao);
            if (cargo == Cargo.PRESIDENTE) {
                m.taxa(TemaMandato.ECONOMIA, "Crescimento do PIB", FormaResumo.MEDIA, true, 1.5, 2.5, null, 0);
                m.taxa(TemaMandato.ECONOMIA, "Inflação (IPCA)", FormaResumo.MEDIA, false, 5.5, 2.5, null, 0);
                m.taxa(TemaMandato.CONTAS, "Dívida pública bruta", FormaResumo.VARIACAO_PONTOS, false, 74, 3, null, 0);
                m.taxa(TemaMandato.EMPREGO, "Desemprego", FormaResumo.VARIACAO_PONTOS, false, 11, 1.2, null, 0);
                for (String etapa : new String[]{"anos iniciais", "anos finais", "ensino médio"}) {
                    m.ideb("escolas públicas, " + etapa, 5.0, null);
                }
                m.contagem("Medidas provisórias editadas", 55);
                m.contagem("Medidas provisórias que viraram lei", 28);
                m.contagem("Projetos enviados ao Congresso", 40);
                m.contagem("Projetos do governo aprovados", 15);
                continue;
            }
            boolean municipal = cargo == Cargo.PREFEITO;
            String ref = municipal ? "estado" : "Brasil";
            m.crescimento(TemaMandato.ECONOMIA, "Crescimento do PIB", municipal ? 2e9 : 5e10, ref, 2);
            m.crescimento(TemaMandato.ECONOMIA, "PIB por pessoa", municipal ? 24_000 : 30_000, ref, 2);
            m.crescimento(TemaMandato.EMPREGO, "Empregos formais", municipal ? 40_000 : 600_000, ref, 1);
            if (municipal) {
                m.ideb("rede municipal, anos iniciais", 5.2, "rede municipal do estado");
            } else {
                m.taxa(TemaMandato.EMPREGO, "Desemprego", FormaResumo.VARIACAO_PONTOS, false, 12, 1.0, "Brasil", 10.5);
                m.ideb("rede estadual, anos finais", 4.4, "redes estaduais do Brasil");
                m.ideb("rede estadual, ensino médio", 3.9, "redes estaduais do Brasil");
            }
            SerieMandato pessoal = m.taxa(TemaMandato.CONTAS, "Gasto com pessoal", FormaResumo.VARIACAO_PONTOS, false,
                    municipal ? 50 : 46, 1.5, null, 0);
            pessoal.setLimite(municipal ? 54.0 : 49.0);
            m.taxa(TemaMandato.CONTAS, "Investimento", FormaResumo.MEDIA, true, 8, 2.5, null, 0);
            m.taxa(TemaMandato.CONTAS, "Sobrou ou faltou dinheiro no ano", FormaResumo.MEDIA, null, 1, 3, null, 0);
        }
    }

    /** Ajuda a montar as séries fictícias de um mandato. */
    private final class Mandato {
        private final Candidato c;
        private final CandidaturaAnterior t;
        private final int primeiro;
        private final int ultimo;
        private final String ente;

        Mandato(Candidato c, CandidaturaAnterior t, int anoEleicao) {
            this.c = c;
            this.t = t;
            this.primeiro = t.getAno() - 1;
            this.ultimo = Math.min(t.getFimMandato(), anoEleicao - 1);
            Cargo cargo = t.getTipoCargo();
            this.ente = cargo == Cargo.PRESIDENTE ? "Governo federal" : cargo == Cargo.GOVERNADOR
                    ? "Governo de " + (t.getLocal().isEmpty() ? c.getUf() : t.getLocal()) : "Prefeitura de " + t.getLocal();
        }

        private SerieMandato nova(TemaMandato tema, String titulo, String unidade, FormaResumo forma, Boolean maior) {
            SerieMandato s = new SerieMandato(SerieMandato.descreverMandato(t.getTipoCargo(), ente, t.getAno(),
                    t.getFimMandato()), t.getTipoCargo(), ente, t.getAno(), t.getFimMandato(), tema, titulo, unidade,
                    forma, maior);
            s.setFonte("Demonstração (valores fictícios)");
            s.setExplicacao("Série inventada para mostrar como a tela funciona.");
            c.adicionarSerieMandato(s);
            return s;
        }

        /** Valor que cresce em % ao ano, com a referência crescendo em ritmo parecido; atraso = anos sem dado. */
        void crescimento(TemaMandato tema, String titulo, double inicial, String ref, int atraso) {
            SerieMandato s = nova(tema, titulo, titulo.startsWith("Emp") ? "empregos" : "R$",
                    FormaResumo.VARIACAO_PERCENTUAL, true);
            s.setReferenciaNome(ref);
            double v = inicial;
            double r = inicial * (3 + rnd.nextDouble());
            for (int ano = primeiro; ano <= ultimo - atraso; ano++) {
                s.adicionar(ano, Math.round(v), (double) Math.round(r));
                v *= 1 + (0.02 + 0.10 * rnd.nextDouble());
                r *= 1 + (0.05 + 0.04 * rnd.nextDouble());
            }
        }

        SerieMandato taxa(TemaMandato tema, String titulo, FormaResumo forma, Boolean maior, double base, double ruido,
                          String ref, double baseRef) {
            SerieMandato s = nova(tema, titulo, "%", forma, maior);
            if (ref != null) {
                s.setReferenciaNome(ref);
            }
            double v = base;
            for (int ano = primeiro; ano <= ultimo; ano++) {
                s.adicionar(ano, Math.round(v * 10) / 10.0, ref == null ? null : Math.round((baseRef + rnd.nextGaussian())
                        * 10) / 10.0);
                v += (rnd.nextDouble() - 0.5) * ruido;
            }
            return s;
        }

        void ideb(String rotulo, double base, String ref) {
            SerieMandato s = nova(TemaMandato.EDUCACAO, "IDEB (" + rotulo + ")", "nota", FormaResumo.VARIACAO_PONTOS, true);
            if (ref != null) {
                s.setReferenciaNome(ref);
            }
            double v = base;
            for (int ano = t.getAno() - 2 + (t.getAno() % 2 == 0 ? 1 : 0); ano <= ultimo; ano += 2) {
                s.adicionar(ano, Math.round(v * 10) / 10.0, ref == null ? null : Math.round((base + 0.1 * rnd.nextGaussian())
                        * 10) / 10.0);
                v += 0.3 * rnd.nextGaussian();
            }
        }

        void contagem(String titulo, int media) {
            SerieMandato s = nova(TemaMandato.LEIS, titulo, "", FormaResumo.SOMA, null);
            for (int ano = t.getAno() + 1; ano <= ultimo; ano++) {
                s.adicionar(ano, Math.max(0, Math.round(media * (0.6 + 0.8 * rnd.nextDouble()))), null);
            }
        }
    }
}
