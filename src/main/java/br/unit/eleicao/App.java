package br.unit.eleicao;

import br.unit.eleicao.coleta.GeradorDadosDemo;
import br.unit.eleicao.coleta.PipelineColeta;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.CatalogoIndicadores;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.ranking.ConfiguracaoRanking;
import br.unit.eleicao.ranking.ItemRanking;
import br.unit.eleicao.ranking.MatrizIndicadores;
import br.unit.eleicao.ranking.SomaPonderada;
import br.unit.eleicao.ui.JanelaPrincipal;
import br.unit.eleicao.util.Texto;

import java.nio.file.Path;
import java.util.List;

/**
 * Ponto de entrada.
 * <pre>
 *   java -jar decisao-eleitoral.jar                     abre a interface com a demonstração
 *   java -jar decisao-eleitoral.jar abrir SE            abre a interface com dados/processados/SE
 *   java -jar decisao-eleitoral.jar coletar SE [2026] [--sem-download]
 *   java -jar decisao-eleitoral.jar ranking SE|demo     imprime o ranking com pesos iguais
 * </pre>
 */
public final class App {

    private static final Path RAIZ = Path.of("dados");

    private App() {
    }

    public static void main(String[] args) {
        String comando = args.length == 0 ? "interface" : args[0].toLowerCase();
        try {
            switch (comando) {
                case "interface":
                    JanelaPrincipal.iniciar(RAIZ, null);
                    break;
                case "abrir":
                    JanelaPrincipal.iniciar(RAIZ, pastaDaBase(argumento(args, 1)));
                    break;
                case "coletar":
                    coletar(args);
                    break;
                case "ranking":
                    imprimirRanking(argumento(args, 1));
                    break;
                default:
                    System.err.println("Comando desconhecido: " + comando);
                    System.err.println("Use: interface | abrir UF | coletar UF [ano] [--sem-download] | ranking UF|demo");
                    System.exit(2);
            }
        } catch (DadosException e) {
            System.err.println("ERRO: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String argumento(String[] args, int i) {
        if (args.length <= i) {
            System.err.println("Informe a UF (ex.: SE) ou 'demo'.");
            System.exit(2);
        }
        return args[i];
    }

    private static Path pastaDaBase(String uf) throws DadosException {
        if ("demo".equalsIgnoreCase(uf)) {
            Path demo = RAIZ.resolve("demo");
            if (!RepositorioArquivos.contemBase(demo)) {
                new GeradorDadosDemo().gerarESalvar(demo);
            }
            return demo;
        }
        return new PipelineColeta(RAIZ, System.out::println).pastaProcessada(uf);
    }

    private static void coletar(String[] args) throws DadosException {
        String uf = argumento(args, 1);
        int ano = 2026;
        boolean baixar = true;
        for (int i = 2; i < args.length; i++) {
            if ("--sem-download".equals(args[i])) {
                baixar = false;
            } else {
                ano = Integer.parseInt(args[i]);
            }
        }
        new PipelineColeta(RAIZ, System.out::println).executar(uf, ano, baixar);
    }

    private static void imprimirRanking(String uf) throws DadosException {
        BaseDados base = new RepositorioArquivos().carregar(pastaDaBase(uf));
        List<Indicador> indicadores = CatalogoIndicadores.todos();
        ConfiguracaoRanking config = new ConfiguracaoRanking();
        for (Indicador ind : indicadores) {
            config.setPeso(ind.getCodigo(), 5);
        }
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        System.out.println(base.getMetadados().getDescricao());
        System.out.printf("%-4s %-32s %-8s %9s %9s%n", "#", "Candidato(a)", "Partido", "Pontuação", "Cobertura");
        for (ItemRanking item : new SomaPonderada().classificar(matriz, config)) {
            System.out.printf("%-4s %-32s %-8s %9s %9s%n", item.getPosicao() == 0 ? "-" : item.getPosicao(),
                    item.getCandidato().getNomeExibicao(), item.getCandidato().getPartido(),
                    item.temPontuacao() ? Texto.decimal(item.getPontuacao(), 1) : "sem dados", item.getCobertura());
        }
    }
}
