package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.persistencia.RepositorioArquivos;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gera uma base 100% FICTÍCIA (pessoas "Exemplo", partidos "PX_", UF "XX") para demonstrar
 * o sistema sem internet. Nenhum número aqui descreve pessoa ou partido real.
 * A semente é fixa, então a demonstração é sempre a mesma.
 */
public class GeradorDadosDemo {

    private static final long SEMENTE = 2026L;
    private static final String[] NOMES = {"Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabriela", "Heitor",
        "Irene", "João", "Karina", "Lucas", "Marta", "Nelson", "Olívia", "Paulo", "Quitéria", "Rafael", "Sílvia",
        "Tiago", "Úrsula", "Vítor", "Wanda", "Xavier", "Yara", "Zeca", "Alice", "Benedito", "Cecília", "Davi"};
    private static final String[] PARTIDOS = {"PXA", "PXB", "PXC", "PXD", "PXE"};
    private static final String[] TEMAS = {"educação básica", "saneamento", "segurança pública", "sistema tributário",
        "meio ambiente", "saúde pública", "transporte urbano", "agricultura familiar", "ciência e tecnologia",
        "previdência"};
    private static final String[] CATEGORIAS = {"DIVULGAÇÃO DA ATIVIDADE PARLAMENTAR", "PASSAGEM AÉREA",
        "COMBUSTÍVEIS E LUBRIFICANTES", "LOCAÇÃO DE VEÍCULOS", "MANUTENÇÃO DE ESCRITÓRIO", "TELEFONIA"};
    private static final String[] OCUPACOES = {"ADVOGADO", "PROFESSOR", "EMPRESÁRIO", "MÉDICO", "DEPUTADO",
        "SERVIDOR PÚBLICO", "AGRICULTOR", "ENGENHEIRO", "COMERCIANTE", "JORNALISTA"};
    private static final String[] CORES = {"BRANCA", "PARDA", "PRETA", "INDÍGENA", "AMARELA"};

    private final Random rnd = new Random(SEMENTE);

    public BaseDados gerar() {
        Metadados meta = new Metadados("XX", 2026, 2022, PipelineColeta.dataPrimeiroTurno(2026));
        meta.setDemonstracao(true);
        meta.setDescricao("DADOS FICTÍCIOS de demonstração - não representam pessoas reais");
        meta.setGeradoEm("gerador com semente fixa " + SEMENTE);
        BaseDados base = new BaseDados(meta);

        List<Votacao> votacoes = gerarVotacoes(base, 150);
        double[] orientacao = new double[votacoes.size()];
        for (int i = 0; i < orientacao.length; i++) {
            orientacao[i] = rnd.nextGaussian();
        }

        // 12 deputados (pares da UF para o z-score); 9 deles tentam a reeleição
        List<Deputado> deputados = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Deputado d = new Deputado(9000 + i, NOMES[i] + " Exemplo", NOMES[i] + " da Silva Exemplo",
                    nascimento(), i % 3 == 0 ? "FEMININO" : "MASCULINO");
            d.setPartido(PARTIDOS[i % PARTIDOS.length]);
            d.setUf("XX");
            base.adicionarDeputado(d);
            deputados.add(d);
            gerarAtividade(base, d, votacoes, orientacao, i == 11);
        }

        for (int i = 0; i < NOMES.length; i++) {
            base.adicionarCandidato(gerarCandidato(i, i < 9 ? deputados.get(i) : null));
        }
        return base;
    }

    public BaseDados gerarESalvar(Path destino) throws DadosException {
        BaseDados base = gerar();
        new RepositorioArquivos().salvar(base, destino);
        return base;
    }

    private List<Votacao> gerarVotacoes(BaseDados base, int quantidade) {
        List<Votacao> lista = new ArrayList<>();
        LocalDate inicio = LocalDate.of(2023, 2, 7);
        String[] tipos = {"Aprovado o Projeto de Lei", "Rejeitado o Requerimento de retirada de pauta",
            "Aprovada a Emenda", "Mantido o texto (destaque)", "Aprovada a Redação Final"};
        for (int i = 0; i < quantidade; i++) {
            LocalDate data = inicio.plusDays(i * 8L + rnd.nextInt(5));
            String tema = TEMAS[rnd.nextInt(TEMAS.length)];
            String proposicao = "PL " + (1000 + rnd.nextInt(4000)) + "/" + data.getYear();
            Votacao v = new Votacao("DEMO-" + (i + 1), data, tipos[i % 3 == 0 ? 0 : rnd.nextInt(tipos.length)] + ".");
            v.setProposicao(proposicao, "[FICTÍCIA] Dispõe sobre " + tema + ".");
            base.adicionarVotacao(v);
            lista.add(v);
        }
        return lista;
    }

    private void gerarAtividade(BaseDados base, Deputado d, List<Votacao> votacoes, double[] orientacao,
                                boolean suplente) {
        double presenca = 0.60 + 0.38 * rnd.nextDouble();
        double posicao = rnd.nextGaussian();
        int primeira = suplente ? votacoes.size() / 2 : 0;
        LocalDate inicioMandato = suplente ? votacoes.get(primeira).getData() : LocalDate.of(2023, 2, 1);
        LocalDate fimMandato = LocalDate.of(2026, 9, 1);
        // um dos deputados tirou licença de 4 meses: essas votações não contam como falta
        boolean licenciado = d.getId() == 9003;
        if (licenciado) {
            d.adicionarExercicio(inicioMandato, LocalDate.of(2024, 3, 1));
            d.adicionarExercicio(LocalDate.of(2024, 7, 1), fimMandato);
        } else {
            d.adicionarExercicio(inicioMandato, fimMandato);
        }
        for (int i = primeira; i < votacoes.size(); i++) {
            if (rnd.nextDouble() > presenca || !d.emExercicio(votacoes.get(i).getData())) {
                continue;
            }
            double sorteio = rnd.nextDouble();
            String voto;
            if (sorteio < 0.04) {
                voto = "Abstenção";
            } else if (sorteio < 0.07) {
                voto = "Obstrução";
            } else {
                double pSim = 1 / (1 + Math.exp(-2 * posicao * orientacao[i]));
                voto = rnd.nextDouble() < pSim ? "Sim" : "Não";
            }
            base.adicionarVoto(d.getId(), votacoes.get(i).getId(), voto);
        }
        double nivelGasto = 22000 + 20000 * rnd.nextDouble();
        int anoInicio = suplente ? 2024 : 2023;
        for (int ano = anoInicio; ano <= 2026; ano++) {
            int ultimoMes = ano == 2026 ? 7 : 12;
            for (int mes = (suplente && ano == 2024) ? 9 : 1; mes <= ultimoMes; mes++) {
                for (String cat : CATEGORIAS) {
                    double valor = nivelGasto / CATEGORIAS.length * (0.5 + rnd.nextDouble());
                    base.adicionarDespesa(new Despesa(d.getId(), ano, mes, cat, Math.round(valor * 100) / 100.0));
                }
            }
        }
        int total = 3 + rnd.nextInt(suplente ? 10 : 60);
        for (int i = 0; i < total; i++) {
            int ano = anoInicio + rnd.nextInt(2027 - anoInicio);
            boolean aprovada = rnd.nextDouble() < 0.06;
            String tipo = rnd.nextDouble() < 0.8 ? "PL" : rnd.nextBoolean() ? "PEC" : "PLP";
            String tema = TEMAS[rnd.nextInt(TEMAS.length)];
            base.adicionarProposicao(new Proposicao(d.getId(), "DEMO-" + d.getId() + "-" + i, tipo,
                    String.valueOf(100 + rnd.nextInt(5000)), ano,
                    aprovada ? "Transformado em Norma Jurídica" : "Aguardando Parecer",
                    aprovada, "[FICTÍCIA] Dispõe sobre " + tema + "."));
        }
    }

    private Candidato gerarCandidato(int i, Deputado deputado) {
        String genero = deputado != null ? deputado.getGenero() : (rnd.nextDouble() < 0.35 ? "FEMININO" : "MASCULINO");
        LocalDate nasc = deputado != null ? deputado.getDataNascimento() : nascimento();
        Candidato c = new Candidato("DEMO" + String.format("%04d", i + 1), NOMES[i] + " da Silva Exemplo",
                NOMES[i] + " Exemplo", nasc, genero);
        c.setNumero(String.valueOf(1100 + i * 7));
        c.setPartido(deputado != null ? deputado.getPartido() : PARTIDOS[rnd.nextInt(PARTIDOS.length)]);
        c.setUf("XX");
        c.setCargo("DEPUTADO FEDERAL");
        c.setGrauInstrucao(escolaridade());
        c.setCorRaca(rnd.nextDouble() < 0.08 ? CORES[3 + rnd.nextInt(2)] : CORES[rnd.nextInt(3)]);
        c.setOcupacao(deputado != null ? "DEPUTADO" : OCUPACOES[rnd.nextInt(OCUPACOES.length)]);
        if (i == 13 || i == 22) {
            c.setSituacao("INAPTO");
            c.setDetalheSituacao("INDEFERIDO");
        } else if (i == 17) {
            c.setSituacao("APTO");
            c.setDetalheSituacao("INDEFERIDO COM RECURSO");
        } else if (i % 6 == 5) {
            c.setSituacao("CADASTRADO");
        } else {
            c.setSituacao("APTO");
            c.setDetalheSituacao("DEFERIDO");
        }
        c.setReeleicao(deputado != null);
        double patrimonio = Math.round(Math.exp(11.5 + 1.6 * rnd.nextGaussian()));
        c.setPatrimonio(patrimonio);
        if (deputado != null || rnd.nextDouble() < 0.4) {
            double anterior = i == 20 ? 0.0 : Math.round(patrimonio / (0.7 + 0.9 * rnd.nextDouble()));
            c.setPatrimonioAnterior(anterior);
        }
        if (deputado != null) {
            c.vincularDeputado(deputado.getId(), "DEMONSTRAÇÃO");
        }
        return c;
    }

    private LocalDate nascimento() {
        return LocalDate.of(1950 + rnd.nextInt(50), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));
    }

    private GrauInstrucao escolaridade() {
        double s = rnd.nextDouble();
        if (s < 0.55) {
            return GrauInstrucao.SUPERIOR_COMPLETO;
        }
        if (s < 0.65) {
            return GrauInstrucao.SUPERIOR_INCOMPLETO;
        }
        if (s < 0.88) {
            return GrauInstrucao.MEDIO_COMPLETO;
        }
        if (s < 0.95) {
            return GrauInstrucao.FUNDAMENTAL_COMPLETO;
        }
        return GrauInstrucao.LE_ESCREVE;
    }
}
