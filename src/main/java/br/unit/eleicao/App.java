package br.unit.eleicao;

import br.unit.eleicao.coleta.GeradorDadosDemo;
import br.unit.eleicao.coleta.PipelineColeta;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.indicador.CatalogoIndicadores;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.ranking.ConfiguracaoRanking;
import br.unit.eleicao.ranking.ItemRanking;
import br.unit.eleicao.ranking.MatrizIndicadores;
import br.unit.eleicao.ranking.SomaPonderada;
import br.unit.eleicao.util.Texto;
import br.unit.eleicao.web.ServicoApi;
import br.unit.eleicao.web.ServidorWeb;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Ponto de entrada. Sem argumentos, sobe a interface em http://localhost:8080 e abre o navegador.
 * <pre>
 *   (sem argumentos)                       interface web com a demonstração (ou a última UF processada)
 *   web [UF|demo] [porta]                  interface web com a base escolhida
 *   coletar SE [2026] [--sem-download] [--sem-empresas]  coleta tudo da UF (--sem-empresas pula o CNPJ, vários GB)
 *   ranking SE|demo                        imprime o ranking com pesos iguais
 * </pre>
 */
public final class App {

    private static final Path RAIZ = Path.of("dados");
    private static final int PORTA_PADRAO = 8080;

    private App() {
    }

    public static void main(String[] args) {
        String comando = args.length == 0 ? "web" : args[0].toLowerCase();
        try {
            switch (comando) {
                case "web":
                    iniciarWeb(args.length > 1 ? args[1] : null,
                            args.length > 2 ? Integer.parseInt(args[2]) : PORTA_PADRAO);
                    break;
                case "coletar":
                    coletar(args);
                    break;
                case "ranking":
                    imprimirRanking(argumento(args, 1));
                    break;
                default:
                    System.err.println("Comando desconhecido: " + comando);
                    System.err.println("Use: web [UF|demo] [porta] | coletar UF [ano] [--sem-download] [--sem-empresas] | ranking UF|demo");
                    System.exit(2);
            }
        } catch (DadosException e) {
            System.err.println("ERRO: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERRO ao iniciar o servidor: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void iniciarWeb(String baseInicial, int porta) throws IOException, DadosException {
        ServicoApi api = new ServicoApi(RAIZ);
        // sem escolha explícita: dados reais sempre que existirem (Brasil inteiro ou o último estado coletado)
        String base = baseInicial == null ? baseReal(RAIZ.resolve("processados")) : baseInicial;
        api.abrir(base);
        int usada = new ServidorWeb(api).iniciar(porta);
        String endereco = "http://localhost:" + usada + "/";
        System.out.println("Interface disponível em " + endereco);
        System.out.println("Deixe esta janela aberta enquanto usa o sistema. Para encerrar: Ctrl+C (ou o botão Stop do IntelliJ).");
        abrirNavegador(endereco);
    }

    /**
     * Base real que o site abre sozinho: o Brasil inteiro, se foi coletado; senão o estado coletado por último;
     * sem nenhuma coleta, a demonstração (dados fictícios).
     */
    static String baseReal(Path processados) {
        if (RepositorioArquivos.contemBase(processados.resolve("BR"))) {
            return "BR";
        }
        String escolhida = "demo";
        java.nio.file.attribute.FileTime maisRecente = null;
        try (java.nio.file.DirectoryStream<Path> pastas = java.nio.file.Files.newDirectoryStream(processados)) {
            for (Path pasta : pastas) {
                if (!RepositorioArquivos.contemBase(pasta)) {
                    continue;
                }
                java.nio.file.attribute.FileTime quando = java.nio.file.Files.getLastModifiedTime(
                        pasta.resolve(RepositorioArquivos.META));
                if (maisRecente == null || quando.compareTo(maisRecente) > 0) {
                    maisRecente = quando;
                    escolhida = pasta.getFileName().toString();
                }
            }
        } catch (IOException e) {
            return "demo"; // pasta ainda não existe: nenhuma coleta feita
        }
        return escolhida;
    }

    /** Tenta abrir o navegador padrão; se não der (servidor sem tela), o endereço já foi impresso. */
    private static void abrirNavegador(String endereco) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(endereco));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            System.out.println("Abra o endereço acima no navegador.");
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
        boolean empresas = true; // coleta completa por padrão
        for (int i = 2; i < args.length; i++) {
            if ("--sem-download".equals(args[i])) {
                baixar = false;
            } else if ("--sem-empresas".equals(args[i])) {
                empresas = false;
            } else if ("--empresas".equals(args[i])) {
                empresas = true; // aceito por compatibilidade (já é o padrão)
            } else {
                ano = Integer.parseInt(args[i]);
            }
        }
        new PipelineColeta(RAIZ, System.out::println).executar(uf, ano, baixar, empresas);
    }

    private static void imprimirRanking(String uf) throws DadosException {
        BaseDados base = new RepositorioArquivos().carregar(pastaDaBase(uf));
        List<Indicador> indicadores = CatalogoIndicadores.todos();
        ConfiguracaoRanking config = new ConfiguracaoRanking();
        for (Indicador ind : indicadores) {
            config.setPeso(ind.getCodigo(), 5);
        }
        config.setCoberturaMinima(2); // mesmo padrão da interface web
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
