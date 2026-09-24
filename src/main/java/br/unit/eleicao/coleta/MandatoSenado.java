package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.ResumoMandato;
import br.unit.eleicao.util.Texto;
import br.unit.eleicao.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Senadores em exercício da UF, pela API de dados abertos do Senado (XML): matérias de autoria e
 * votos nominais. Tags conferidas na biblioteca DadosAbertosBrasil, que usa a mesma API.
 */
public class MandatoSenado {

    private static final String API = "https://legis.senado.leg.br/dadosabertos/senador/";
    private static final String XML = "application/xml";

    private final Path pasta;
    private final String uf;
    private final Downloader downloader;
    private final Consumer<String> log;
    private Map<String, Double> gastoMensal = Map.of();

    /** Gasto médio mensal da verba de gabinete (CEAPS) por nome parlamentar normalizado. */
    public void setGastoMensal(Map<String, Double> gastoMensal) {
        this.gastoMensal = gastoMensal == null ? Map.of() : gastoMensal;
    }

    /** @param downloader null = usar só o que já está em cache no disco */
    public MandatoSenado(Path pasta, String uf, Downloader downloader, Consumer<String> log) {
        this.pasta = pasta.resolve("senado");
        this.uf = uf.toUpperCase();
        this.downloader = downloader;
        this.log = log;
    }

    public void processar(List<Candidato> candidatos) {
        try {
            String filtro = ArquivosBrutos.isNacional(uf) ? "" : "?uf=" + uf;
            Document lista = Xml.ler(obter(API + "lista/atual" + filtro, "lista-" + uf + ".xml"));
            Map<String, List<Candidato>> porNome = new HashMap<>();
            for (Candidato c : candidatos) {
                porNome.computeIfAbsent(Texto.normalizar(c.getNome()), k -> new ArrayList<>()).add(c);
            }
            int n = 0;
            for (Element p : Xml.elementos(lista, "Parlamentar")) {
                String codigo = Xml.texto(p, "CodigoParlamentar");
                String nomeCompleto = Xml.texto(p, "NomeCompletoParlamentar");
                if (codigo.isEmpty()) {
                    continue;
                }
                Candidato c = localizar(porNome.get(Texto.normalizar(nomeCompleto)), codigo);
                if (c == null) {
                    continue;
                }
                ResumoMandato r = resumir(codigo, Xml.texto(p, "UrlPaginaParlamentar"));
                r.setGastoMensal(gastoMensal.get(Texto.normalizar(Xml.texto(p, "NomeParlamentar"))));
                c.adicionarAtuacao(r);
                n++;
            }
            log.accept("  Senado: " + n + " candidatos são senadores(as) em exercício"
                    + (ArquivosBrutos.isNacional(uf) ? "" : " por " + uf));
        } catch (DadosException e) {
            log.accept("  aviso: dados do Senado indisponíveis (" + e.getMessage() + ")");
        }
    }

    /** Aceita o homônimo só se a data de nascimento do cadastro do Senado confirmar. */
    private Candidato localizar(List<Candidato> mesmosNomes, String codigo) throws DadosException {
        if (mesmosNomes == null || mesmosNomes.isEmpty()) {
            return null;
        }
        Document detalhe = Xml.ler(obter(API + codigo, codigo + ".xml"));
        LocalDate nascimento = Texto.parseData(Xml.texto(detalhe.getDocumentElement(), "DataNascimento"));
        for (Candidato c : mesmosNomes) {
            if (nascimento != null && nascimento.equals(c.getDataNascimento())) {
                return c;
            }
        }
        return null;
    }

    private ResumoMandato resumir(String codigo, String url) throws DadosException {
        ResumoMandato r = new ResumoMandato("Senado Federal", "Senador(a)", "mandato atual", url);
        Document votacoes = Xml.ler(obter(API + codigo + "/votacoes", codigo + "-votacoes.xml"));
        List<Element> votos = Xml.elementos(votacoes, "Votacao");
        int votou = 0;
        String primeira = null;
        String ultima = null;
        for (Element v : votos) {
            String sigla = Texto.normalizar(Xml.texto(v, "SiglaDescricaoVoto"));
            if (sigla.equals("SIM") || sigla.equals("NAO") || sigla.equals("ABSTENCAO")) {
                votou++;
            }
            String data = Xml.texto(v, "DataSessao");
            if (!data.isEmpty()) {
                primeira = primeira == null || data.compareTo(primeira) < 0 ? data : primeira;
                ultima = ultima == null || data.compareTo(ultima) > 0 ? data : ultima;
            }
        }
        if (!votos.isEmpty()) {
            r.setPresenca(100.0 * votou / votos.size(), "Votou (Sim, Não ou Abstenção) em " + votou + " de "
                    + votos.size() + " votações nominais registradas pela API do Senado"
                    + (primeira == null ? "" : " (" + Texto.formatarData(Texto.parseData(primeira)) + " a "
                    + Texto.formatarData(Texto.parseData(ultima)) + ")")
                    + "; os demais registros são licenças, missões oficiais ou ausências");
        }
        Document autorias = Xml.ler(obter(API + codigo + "/autorias", codigo + "-autorias.xml"));
        List<Element> materias = Xml.elementos(autorias, "Autoria");
        int principal = 0;
        for (Element a : materias) {
            if ("SIM".equals(Texto.normalizar(Xml.texto(a, "IndicadorAutorPrincipal")))) {
                principal++;
            }
        }
        // a API de autorias não informa se a matéria foi aprovada
        r.setProjetos(materias.size(), 0);
        materias.sort(Comparator.comparing((Element a) -> Xml.texto(a, "Data")).reversed());
        for (Element a : materias) {
            r.adicionarDestaque(Xml.texto(a, "DescricaoIdentificacao") + " – " + Xml.texto(a, "Ementa"));
        }
        r.setObservacaoProjetos(principal + " como autor(a) principal");
        return r;
    }

    private Path obter(String url, String nomeArquivo) throws DadosException {
        Path arquivo = pasta.resolve(nomeArquivo);
        if (Files.exists(arquivo)) {
            return arquivo;
        }
        if (downloader == null) {
            throw new DadosException("não baixado (colete com download para obter)");
        }
        return downloader.baixar(url, arquivo, false, XML);
    }
}
