package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ColetaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

/**
 * Downloads cujo endereço muda com a data de publicação: sanções da CGU (arquivo diário), servidores
 * federais (mensal) e dados abertos do CNPJ (mensal). Tenta as datas mais recentes primeiro.
 * Falhas não interrompem a coleta: a fonte fica marcada como "não verificada" na ficha.
 */
public class DownloadsComplementares {

    private static final int DIAS_SANCOES = 10;
    private static final int MESES_SIAPE = 4;
    private static final int MESES_CNPJ = 3;

    private final Path pasta;
    private final Downloader downloader;
    private final Consumer<String> log;

    public DownloadsComplementares(Path pasta, Downloader downloader, Consumer<String> log) {
        this.pasta = pasta;
        this.downloader = downloader;
        this.log = log;
    }

    public void baixar(String uf, int anoEleicao, boolean empresas) {
        log.accept("Baixando cadastros da CGU e do serviço público federal...");
        for (String cadastro : SancoesCgu.CADASTROS) {
            Path destino = SancoesCgu.arquivo(pasta, cadastro);
            if (!Files.exists(destino)) {
                for (int d = 0; d < DIAS_SANCOES; d++) {
                    if (tentar(FontesDados.sancoes(cadastro, LocalDate.now().minusDays(d)), destino)) {
                        break;
                    }
                }
            }
        }
        Path siape = pasta.resolve(ServidoresFederais.ARQUIVO);
        if (!Files.exists(siape)) {
            for (int m = 1; m <= MESES_SIAPE; m++) {
                if (tentar(FontesDados.servidoresSiape(YearMonth.now().minusMonths(m)), siape)) {
                    break;
                }
            }
        }
        Path pep = pasta.resolve(CargosPublicosPep.ARQUIVO);
        if (!Files.exists(pep)) {
            for (int m = 0; m < MESES_SIAPE; m++) {
                if (tentar(FontesDados.pep(YearMonth.now().minusMonths(m)), pep)) {
                    break;
                }
            }
        }
        log.accept("Baixando planos de governo registrados no TSE...");
        for (String u : PlanosGoverno.ufs(uf)) {
            String url = FontesDados.propostaGoverno(anoEleicao, u);
            tentar(url, pasta.resolve(FontesDados.nomeLocal(url)));
        }
        if (empresas) {
            baixarCnpj();
            baixarContratos(anoEleicao);
        } else {
            log.accept("  (empresas) dados do CNPJ não baixados: use --empresas ou coloque Socios*.zip e Empresas*.zip "
                    + "em " + pasta.resolve(EmpresasReceita.PASTA).toAbsolutePath());
        }
    }

    /** Contratos federais mensais desde o início do último mandato (ano da eleição anterior + 1). */
    private void baixarContratos(int anoEleicao) {
        log.accept("Baixando contratos do governo federal (arquivos mensais)...");
        YearMonth mes = YearMonth.of(anoEleicao - 3, 1);
        YearMonth fim = YearMonth.now().minusMonths(1);
        Path destino = pasta.resolve(ContratosFederais.PASTA);
        while (!mes.isAfter(fim)) {
            String nome = mes.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + ".zip";
            tentar(FontesDados.comprasFederais(mes), destino.resolve(nome));
            mes = mes.plusMonths(1);
        }
    }

    private void baixarCnpj() {
        Path destino = pasta.resolve(EmpresasReceita.PASTA);
        for (int m = 0; m < MESES_CNPJ; m++) {
            YearMonth mes = YearMonth.now().minusMonths(m);
            if (!tentar(FontesDados.cnpj(mes, "Qualificacoes.zip"), destino.resolve("Qualificacoes.zip"))) {
                continue; // mês ainda não publicado
            }
            for (int i = 0; i <= 9; i++) {
                tentar(FontesDados.cnpj(mes, "Socios" + i + ".zip"), destino.resolve("Socios" + i + ".zip"));
                tentar(FontesDados.cnpj(mes, "Empresas" + i + ".zip"), destino.resolve("Empresas" + i + ".zip"));
            }
            return;
        }
        log.accept("  aviso: dados do CNPJ não encontrados nos últimos " + MESES_CNPJ + " meses");
    }

    /** Baixa e confere se veio um zip de verdade (o portal pode responder com uma página HTML). */
    private boolean tentar(String url, Path destino) {
        try {
            downloader.baixar(url, destino, false, "application/zip, application/octet-stream, */*;q=0.5");
            if (SancoesCgu.pareceZip(destino)) {
                return true;
            }
            Files.deleteIfExists(destino);
        } catch (ColetaException | IOException e) {
            log.accept("  aviso: " + e.getMessage());
        }
        return false;
    }
}
