package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.ContaIrregular;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lista do TCU de responsáveis com contas julgadas irregulares com implicação eleitoral (enviada ao TSE
 * para a Lei da Ficha Limpa). O cabeçalho exato não está documentado publicamente, então as colunas são
 * localizadas por trechos do nome, e separador e codificação são detectados.
 * Só liga um processo a uma candidatura quando o CPF confere: nome sozinho não basta para algo tão sério.
 */
public class ContasIrregularesTcu {

    public static final String URL =
            "https://sites.tcu.gov.br/dados-abertos/inidoneos-irregulares/arquivos/resp-contas-julgadas-irreg-implicacao-eleitoral.csv";

    private final Path pasta;
    private final Consumer<String> log;

    public ContasIrregularesTcu(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    /** @return true se a lista foi lida (a verificação foi feita) */
    public boolean processar(List<Candidato> candidatos, Map<String, String> cpfPorSq) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(URL));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: lista do TCU ausente - verificação de contas irregulares não feita");
            return false;
        }
        Map<String, Candidato> porCpf = new HashMap<>();
        for (Candidato c : candidatos) {
            String cpf = cpfPorSq.get(c.getSq());
            if (cpf != null && cpf.length() == 11) {
                porCpf.put(cpf, c);
            }
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
        Charset charset = detectarCharset(bytes);
        String primeiraLinha = new String(bytes, 0, Math.min(bytes.length, 4000), charset).split("\\R", 2)[0];
        char separador = contar(primeiraLinha, ';') >= contar(primeiraLinha, ',') ? ';' : ',';
        int ligados = 0;
        try (LeitorCsv csv = new LeitorCsv(new ByteArrayInputStream(bytes), charset, separador, arquivo.getFileName().toString())) {
            int iNome = csv.indiceContendo("NOME");
            int iCpf = csv.indiceContendo("CPF");
            if (iCpf < 0) {
                iCpf = csv.indiceContendo("NUM", "INSCRI");
            }
            if (iNome < 0 || iCpf < 0) {
                throw new ArquivoInvalidoException("Lista do TCU sem colunas de nome e CPF reconhecíveis. Colunas: "
                        + csv.descreverCabecalho());
            }
            int iUf = csv.indiceContendo("UF");
            int iMun = csv.indiceContendo("MUNIC");
            int iProc = csv.indiceContendo("PROCESSO");
            int iDelib = csv.indiceContendo("DELIBERA");
            if (iDelib < 0) {
                iDelib = csv.indiceContendo("ACORD");
            }
            int iTransito = csv.indiceContendo("TRANSIT");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String cpfLinha = LeitorCsv.campo(l, iCpf).replace(".", "").replace("-", "").replace(" ", "");
                String nome = Texto.normalizar(LeitorCsv.campo(l, iNome));
                Candidato c = null;
                String criterio = null;
                String digitos = Texto.somenteDigitos(cpfLinha);
                if (digitos.length() == 11 && cpfLinha.length() == 11) {
                    c = porCpf.get(digitos);
                    criterio = "CPF";
                } else if (cpfLinha.length() == 11) {
                    c = porCpfParcial(porCpf, cpfLinha, nome);
                    criterio = "CPF parcial + nome";
                }
                if (c == null) {
                    continue;
                }
                String municipio = LeitorCsv.campo(l, iMun);
                String uf = LeitorCsv.campo(l, iUf);
                c.adicionarContaIrregular(new ContaIrregular(LeitorCsv.campo(l, iProc), LeitorCsv.campo(l, iDelib),
                        LeitorCsv.campo(l, iTransito), municipio.isEmpty() ? uf : municipio + "/" + uf, criterio));
                ligados++;
            }
        }
        log.accept("  TCU: " + ligados + " processo(s) de contas irregulares ligados a candidatos pelo CPF");
        return true;
    }

    /** CPF mascarado (ex.: "***456789**"): exige 6+ dígitos visíveis iguais e o mesmo nome completo. */
    private static Candidato porCpfParcial(Map<String, Candidato> porCpf, String mascarado, String nome) {
        Candidato achado = null;
        for (Map.Entry<String, Candidato> e : porCpf.entrySet()) {
            int iguais = 0;
            boolean conflito = false;
            for (int i = 0; i < 11; i++) {
                char m = mascarado.charAt(i);
                if (Character.isDigit(m)) {
                    if (m == e.getKey().charAt(i)) {
                        iguais++;
                    } else {
                        conflito = true;
                        break;
                    }
                }
            }
            if (!conflito && iguais >= 6 && nome.equals(Texto.normalizar(e.getValue().getNome()))) {
                if (achado != null) {
                    return null; // ambíguo
                }
                achado = e.getValue();
            }
        }
        return achado;
    }

    static Charset detectarCharset(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    private static int contar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
