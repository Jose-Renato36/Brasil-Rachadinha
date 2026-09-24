package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ColetaException;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.PosicaoUsuario;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.util.EscritorCsv;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Orquestra a coleta: baixa os arquivos (se pedido), processa TSE e Câmara, cruza identidades
 * e grava a base processada em dados/processados/UF.
 */
public class PipelineColeta {

    private final Path raiz;
    private final Consumer<String> log;

    public PipelineColeta(Path raiz, Consumer<String> log) {
        this.raiz = raiz;
        this.log = log;
    }

    public Path pastaBrutos() {
        return raiz.resolve("brutos");
    }

    public Path pastaProcessada(String uf) {
        return raiz.resolve("processados").resolve(uf.toUpperCase());
    }

    /** Anos da legislatura que termina no ano da eleição (ex.: 2026 -> 2023 a 2026). */
    public static int[] anosLegislatura(int anoEleicao) {
        return new int[]{anoEleicao - 3, anoEleicao - 2, anoEleicao - 1, anoEleicao};
    }

    /** Legislatura da Câmara que termina no ano da eleição (2023-2027 = 57ª). */
    public static int legislatura(int anoEleicao) {
        return 57 + (anoEleicao - 2026) / 4;
    }

    public static LocalDate inicioLegislatura(int anoEleicao) {
        return LocalDate.of(anoEleicao - 3, 2, 1);
    }

    /** 1º turno: primeiro domingo de outubro. */
    public static LocalDate dataPrimeiroTurno(int ano) {
        return LocalDate.of(ano, 10, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    public BaseDados executar(String uf, int anoEleicao, boolean baixar) throws DadosException {
        return executar(uf, anoEleicao, baixar, false);
    }

    /**
     * @param baixarEmpresas também baixa os dados abertos do CNPJ (vários GB); sem isso, as empresas só são
     *                       verificadas se os arquivos já estiverem em dados/brutos/cnpj
     */
    public BaseDados executar(String uf, int anoEleicao, boolean baixar, boolean baixarEmpresas) throws DadosException {
        uf = uf.toUpperCase();
        int anoAnterior = anoEleicao - 4;
        int[] anos = anosLegislatura(anoEleicao);
        if (baixar) {
            baixarTudo(anoEleicao, anoAnterior, anos);
            new DownloadsComplementares(pastaBrutos(), new Downloader(log), log).baixar(anoEleicao, baixarEmpresas);
        }

        Metadados meta = new Metadados(uf, anoEleicao, anoAnterior, dataPrimeiroTurno(anoEleicao));
        meta.setDescricao("Eleições " + anoEleicao + " · " + (ArquivosBrutos.isNacional(uf) ? "Brasil inteiro" : uf)
                + " · TSE, Câmara, Senado e TCU");
        meta.setGeradoEm(LocalDateTime.now().withNano(0).toString());
        BaseDados base = new BaseDados(meta);

        log.accept("Processando TSE...");
        ProcessadorTSE tse = new ProcessadorTSE(pastaBrutos(), uf, anoEleicao, anoAnterior, log);
        tse.processar(base);

        log.accept("Montando a trajetória política (eleições " + ProcessadorTrajetoria.anosAnteriores(anoEleicao)[0]
                + " a " + (anoEleicao - 2) + ")...");
        new ProcessadorTrajetoria(pastaBrutos(), uf, log).processar(new ArrayList<>(base.getCandidatos()),
                tse.getCpfPorSq(), ProcessadorTrajetoria.anosAnteriores(anoEleicao));

        log.accept("Completando a trajetória com bens declarados e cassações de cada eleição...");
        HistoricoTse historico = new HistoricoTse(pastaBrutos(), uf, log);
        historico.processar(new ArrayList<>(base.getCandidatos()), ProcessadorTrajetoria.anosAnteriores(anoEleicao));
        if (historico.isBensLidos()) {
            meta.marcarVerificada(Metadados.FONTE_BENS_ANTERIORES);
        }
        if (historico.isCassacoesLidas()) {
            meta.marcarVerificada(Metadados.FONTE_CASSACAO);
        }

        log.accept("Lendo o financiamento das campanhas (prestação de contas)...");
        if (new FinanciamentoCampanha(pastaBrutos(), uf, log).processar(new ArrayList<>(base.getCandidatos()), anoEleicao)) {
            meta.marcarVerificada(Metadados.FONTE_RECEITAS);
        }

        log.accept("Verificando contas julgadas irregulares pelo TCU...");
        meta.setTcuVerificado(new ContasIrregularesTcu(pastaBrutos(), log)
                .processar(new ArrayList<>(base.getCandidatos()), tse.getCpfPorSq()));

        log.accept("Processando Câmara...");
        ProcessadorCamara camara = new ProcessadorCamara(pastaBrutos(), uf, anos, legislatura(anoEleicao),
                inicioLegislatura(anoEleicao), log);
        camara.processar(base);
        log.accept(baixar ? "Consultando licenças e reassunções dos deputados (API da Câmara)..."
                : "Lendo licenças e reassunções já baixadas (sem download)...");
        LocalDate fim = LocalDate.now().isBefore(meta.getDataEleicao()) ? LocalDate.now() : meta.getDataEleicao();
        new HistoricoDeputados(pastaBrutos(), baixar ? new Downloader(log) : null, log)
                .preencher(camara.getDeputados().values(), legislatura(anoEleicao), inicioLegislatura(anoEleicao), fim);

        log.accept("Resumindo mandatos anteriores na Câmara (" + MandatoAnteriorCamara.anos(anoEleicao)[0] + "-"
                + (anoEleicao - 4) + ")...");
        new MandatoAnteriorCamara(pastaBrutos(), uf, log).processar(new ArrayList<>(base.getCandidatos()),
                tse.getCpfPorSq(), anoEleicao, baixar ? new Downloader(log) : null);
        log.accept("Consultando o Senado...");
        new MandatoSenado(pastaBrutos(), uf, baixar ? new Downloader(log) : null, log)
                .processar(new ArrayList<>(base.getCandidatos()));

        log.accept("Cruzando candidaturas com deputados...");
        CruzadorIdentidades cruzador = new CruzadorIdentidades();
        cruzador.cruzar(new ArrayList<>(base.getCandidatos()), base.getDeputados(), tse.getCpfPorSq(),
                camara.getCpfPorDeputado(), lerVinculosManuais());
        long vinculados = base.getCandidatos().stream().filter(c -> c.getIdDeputado() != null).count();
        log.accept("  " + vinculados + " candidaturas ligadas a um mandato; "
                + cruzador.getParaConferir().size() + " casos para conferir manualmente.");

        List<br.unit.eleicao.modelo.Candidato> todos = new ArrayList<>(base.getCandidatos());
        log.accept("Ligando emendas parlamentares...");
        if (new EmendasParlamentares(pastaBrutos(), log).processar(base)) {
            meta.marcarVerificada(Metadados.FONTE_EMENDAS);
        }
        log.accept("Procurando empresas das quais os candidatos são sócios (Receita Federal)...");
        if (new EmpresasReceita(pastaBrutos(), log).processar(todos, tse.getCpfPorSq())) {
            meta.marcarVerificada(Metadados.FONTE_EMPRESAS);
        }
        log.accept("Verificando sanções da CGU (CEIS/CNEP)...");
        if (new SancoesCgu(pastaBrutos(), log).processar(todos, tse.getCpfPorSq())) {
            meta.marcarVerificada(Metadados.FONTE_SANCOES);
        }
        log.accept("Verificando vínculos com o serviço público federal (SIAPE)...");
        if (new ServidoresFederais(pastaBrutos(), log).processar(todos, tse.getCpfPorSq())) {
            meta.marcarVerificada(Metadados.FONTE_SIAPE);
        }
        log.accept(baixar ? "Consultando gasto com pessoal de prefeituras e governos (SICONFI)..."
                : "Lendo dados fiscais já baixados (SICONFI)...");
        if (new GestaoFiscalSiconfi(pastaBrutos(), baixar ? new Downloader(log) : null, log).processar(todos, anoEleicao)) {
            meta.marcarVerificada(Metadados.FONTE_SICONFI);
        }

        Path destino = pastaProcessada(uf);
        RepositorioArquivos repositorio = new RepositorioArquivos();
        List<PosicaoUsuario> posicoesAntigas = repositorio.lerPosicoes(destino);
        base.setDiretorio(destino);
        for (PosicaoUsuario p : posicoesAntigas) {
            if (base.getVotacao(p.getIdVotacao()) != null) {
                base.definirPosicao(p);
            }
        }
        repositorio.salvar(base, destino);
        gravarConferencia(destino, cruzador.getParaConferir());
        log.accept("Base gravada em " + destino.toAbsolutePath());
        return base;
    }

    private void baixarTudo(int anoEleicao, int anoAnterior, int[] anos) throws ColetaException {
        Downloader downloader = new Downloader(log);
        Path pasta = pastaBrutos();
        log.accept("Baixando arquivos do TSE (podem ter centenas de MB)...");
        // sem as candidaturas atuais não há o que analisar: essa falha interrompe a coleta
        try {
            downloader.baixar(FontesDados.candidatos(anoEleicao), pasta, false);
        } catch (ColetaException e) {
            throw new ColetaException(e.getMessage() + "\nSe o site do TSE recusar o download, abra "
                    + FontesDados.paginaTse(anoEleicao) + " no navegador, baixe os arquivos .zip e coloque em "
                    + pasta.toAbsolutePath() + " (sem renomear). Depois colete sem download.", e);
        }
        List<String> opcionais = new ArrayList<>(List.of(FontesDados.bens(anoEleicao),
                FontesDados.candidatosComplementar(anoEleicao),
                FontesDados.candidatos(anoAnterior), FontesDados.bens(anoAnterior), FontesDados.deputados()));
        for (int ano : ProcessadorTrajetoria.anosAnteriores(anoEleicao)) {
            if (ano != anoAnterior) {
                opcionais.add(FontesDados.candidatos(ano)); // trajetória (anoAnterior já está na lista)
                opcionais.add(FontesDados.bens(ano)); // série do patrimônio
            }
            opcionais.add(FontesDados.motivoCassacao(ano));
        }
        opcionais.add(ContasIrregularesTcu.URL);
        opcionais.add(FontesDados.prestacaoContas(anoEleicao));
        opcionais.add(FontesDados.emendas());
        opcionais.add(FontesDados.municipiosTseIbge());
        // legislatura anterior (resumo do mandato de ex-deputados federais) + atual
        List<Integer> anosCamara = new ArrayList<>();
        for (int ano : MandatoAnteriorCamara.anos(anoEleicao)) {
            anosCamara.add(ano);
        }
        for (int ano : anos) {
            anosCamara.add(ano);
        }
        for (int ano : anosCamara) {
            opcionais.add(FontesDados.votacoes(ano));
            opcionais.add(FontesDados.votos(ano));
            opcionais.add(FontesDados.votacoesProposicoes(ano));
            opcionais.add(FontesDados.cota(ano));
            opcionais.add(FontesDados.proposicoes(ano));
            opcionais.add(FontesDados.autores(ano));
        }
        log.accept("Baixando arquivos da Câmara...");
        for (String url : opcionais) {
            try {
                downloader.baixar(url, pasta, false);
            } catch (ColetaException e) {
                log.accept("  aviso: " + e.getMessage() + " - seguindo sem este arquivo");
            }
        }
    }

    /** dados/vinculos_manuais.csv (sq;idDeputado) corrige cruzamentos; idDeputado 0 = sem vínculo. */
    private Map<String, Integer> lerVinculosManuais() throws DadosException {
        Map<String, Integer> manuais = new HashMap<>();
        Path arquivo = raiz.resolve("vinculos_manuais.csv");
        if (!Files.exists(arquivo)) {
            return manuais;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iDep = csv.indice("idDeputado");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                if (id != null) {
                    manuais.put(LeitorCsv.campo(l, iSq), id);
                }
            }
        }
        log.accept("  " + manuais.size() + " vínculos manuais lidos de " + arquivo.getFileName());
        return manuais;
    }

    private void gravarConferencia(Path destino, List<String> linhas) throws DadosException {
        try (EscritorCsv csv = new EscritorCsv(destino.resolve("vinculos_para_conferir.csv"),
                "sq", "candidato", "idDeputado", "deputado", "criterio")) {
            for (String linha : linhas) {
                csv.escrever((Object[]) linha.split(";", 5));
            }
        }
    }

}
