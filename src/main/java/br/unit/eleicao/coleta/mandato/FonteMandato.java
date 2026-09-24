package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.excecao.ColetaException;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Uma fonte pública de indicadores de gestão (IBGE, Tesouro, INEP, Banco Central, Câmara).
 * Cada subclasse sabe para quais mandatos se aplica e como transformar a resposta da fonte em séries
 * ano a ano. As respostas ficam guardadas em dados/brutos/mandatos/NOME, então a segunda coleta
 * não depende da internet.
 */
public abstract class FonteMandato {

    private final Path pasta;
    private final Downloader downloader;
    protected final Consumer<String> log;

    /** @param downloader null = só usa respostas já guardadas */
    protected FonteMandato(Path brutos, Downloader downloader, Consumer<String> log) {
        this.pasta = brutos.resolve("mandatos").resolve(getNome());
        this.downloader = downloader;
        this.log = log;
    }

    /** Nome curto da fonte (também é o nome da pasta de cache). */
    public abstract String getNome();

    /** Se a fonte tem dados para este tipo de mandato. */
    public abstract boolean aplica(MandatoExecutivo m);

    /** Séries do mandato; lista vazia se a fonte não respondeu. */
    public abstract List<SerieMandato> coletar(MandatoExecutivo m);

    /** Espera entre downloads, para respeitar o limite de algumas APIs (ex.: SICONFI, 1 por segundo). */
    protected long getPausaMs() {
        return 0;
    }

    /** Conteúdo guardado ou baixado agora; null se não existir e não puder baixar. */
    protected String obterTexto(String url, String arquivo) {
        Path destino = obterArquivo(url, arquivo, "application/json");
        if (destino == null) {
            return null;
        }
        try {
            return Files.readString(destino, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.accept("  aviso: " + getNome() + ": não foi possível ler " + destino + ": " + e.getMessage());
            return null;
        }
    }

    /** Caminho do arquivo guardado ou baixado agora; null se não existir e não puder baixar. */
    protected Path obterArquivo(String url, String arquivo, String accept) {
        Path destino = pasta.resolve(arquivo);
        if (Files.exists(destino)) {
            return destino;
        }
        if (downloader == null) {
            return null;
        }
        try {
            if (getPausaMs() > 0) {
                Thread.sleep(getPausaMs());
            }
            return downloader.baixar(url, destino, false, accept);
        } catch (ColetaException e) {
            log.accept("  aviso: " + getNome() + ": " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Cria a série já preenchida com os dados do mandato. */
    protected SerieMandato nova(MandatoExecutivo m, TemaMandato tema, String titulo, String unidade, FormaResumo forma,
                                Boolean maiorMelhor, String fonte, String explicacao) {
        SerieMandato s = new SerieMandato(m.getChave(), m.getCargo(), m.getEnte(), m.getAnoEleicao(),
                m.getFimMandato(), tema, titulo, unidade, forma, maiorMelhor);
        s.setFonte(fonte);
        s.setExplicacao(explicacao);
        return s;
    }
}
