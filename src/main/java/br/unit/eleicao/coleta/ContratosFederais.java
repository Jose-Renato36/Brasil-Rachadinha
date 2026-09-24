package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.ContratoPublico;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.file.DirectoryStream;
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
 * Contratos do governo federal com empresas de que os candidatos são sócios (Portal da Transparência,
 * arquivos mensais em dados/brutos/compras/AAAAMM.zip com a entrada AAAAMM_Compras.csv; colunas
 * "Código Contratado" (CNPJ), "Nome Contratado", "Objeto", "Valor Inicial Compra", "Nome Órgão",
 * "Data Assinatura Contrato"). Contratos de estados e prefeituras não entram: não há base nacional
 * com busca oficial por fornecedor.
 */
public class ContratosFederais {

    public static final String PASTA = "compras";

    private final Path pasta;
    private final Consumer<String> log;

    public ContratosFederais(Path brutos, Consumer<String> log) {
        this.pasta = brutos.resolve(PASTA);
        this.log = log;
    }

    /** @return true se havia arquivos de contratos para ler */
    public boolean processar(List<Candidato> candidatos) throws ArquivoInvalidoException {
        Map<String, List<Candidato>> porEmpresa = new HashMap<>();
        for (Candidato c : candidatos) {
            for (VinculoEmpresa e : c.getEmpresas()) {
                porEmpresa.computeIfAbsent(e.getCnpj(), k -> new ArrayList<>()).add(c);
            }
        }
        List<Path> arquivos = listar();
        if (arquivos.isEmpty()) {
            log.accept("  aviso: sem arquivos de contratos em " + pasta.toAbsolutePath() + " - contratos não verificados");
            return false;
        }
        if (porEmpresa.isEmpty()) {
            log.accept("Contratos federais: nenhum candidato com empresa ligada; nada a cruzar.");
            return true;
        }
        Set<String> vistos = new HashSet<>();
        int n = 0;
        for (Path zip : arquivos) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(zip, EmendasParlamentares.WINDOWS_1252, "", "_compras.csv", ';');
            if (aberto == null) {
                continue;
            }
            try (LeitorCsv csv = aberto) {
                int iCnpj = csv.indice("CÓDIGO CONTRATADO", "CODIGO CONTRATADO");
                int iNome = csv.indiceOpcional("NOME CONTRATADO");
                int iObjeto = csv.indiceOpcional("OBJETO");
                int iValor = csv.indiceOpcional("VALOR INICIAL COMPRA", "VALOR FINAL COMPRA");
                int iOrgao = csv.indiceOpcional("NOME ÓRGÃO", "NOME ORGAO", "NOME UG");
                int iData = csv.indiceOpcional("DATA ASSINATURA CONTRATO", "DATA INÍCIO VIGÊNCIA");
                int iNumero = csv.indiceOpcional("NÚMERO DO CONTRATO", "NUMERO DO CONTRATO");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    String cnpj = Texto.somenteDigitos(LeitorCsv.campo(l, iCnpj));
                    if (cnpj.length() != 14) {
                        continue;
                    }
                    List<Candidato> socios = porEmpresa.get(cnpj.substring(0, 8));
                    if (socios == null || !vistos.add(cnpj + "|" + LeitorCsv.campo(l, iNumero) + "|" + LeitorCsv.campo(l, iOrgao))) {
                        continue;
                    }
                    Double valor = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                    for (Candidato c : socios) {
                        c.adicionarContrato(new ContratoPublico(cnpj, LeitorCsv.campo(l, iNome), LeitorCsv.campo(l, iOrgao),
                                LeitorCsv.campo(l, iObjeto), valor == null ? 0 : valor, LeitorCsv.campo(l, iData)));
                        n++;
                    }
                }
            }
        }
        log.accept("Contratos federais: " + n + " contrato(s) de empresas de candidatos.");
        return true;
    }

    private List<Path> listar() {
        List<Path> lista = new ArrayList<>();
        if (!Files.isDirectory(pasta)) {
            return lista;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pasta, "*.zip")) {
            ds.forEach(lista::add);
        } catch (IOException e) {
            log.accept("  aviso: não foi possível listar " + pasta + ": " + e.getMessage());
        }
        lista.sort(null);
        return lista;
    }
}
