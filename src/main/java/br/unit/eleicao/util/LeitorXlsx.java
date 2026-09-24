package br.unit.eleicao.util;

import br.unit.eleicao.excecao.ArquivoInvalidoException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Leitor mínimo de planilhas .xlsx sem bibliotecas externas. Um .xlsx é um zip com XMLs:
 * xl/workbook.xml (nomes das abas), xl/_rels/workbook.xml.rels (arquivo de cada aba),
 * xl/sharedStrings.xml (textos) e xl/worksheets/sheetN.xml (células). Lê tudo em fluxo (StAX).
 */
public class LeitorXlsx {

    private final Map<String, byte[]> partes = new HashMap<>();
    private final List<String> textos = new ArrayList<>();
    private final Map<String, String> arquivoPorAba = new LinkedHashMap<>();

    /** @param xlsx conteúdo do arquivo .xlsx */
    public LeitorXlsx(InputStream xlsx) throws ArquivoInvalidoException {
        try (ZipInputStream zip = new ZipInputStream(xlsx)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                String nome = e.getName();
                if (nome.equals("xl/workbook.xml") || nome.equals("xl/_rels/workbook.xml.rels")
                        || nome.equals("xl/sharedStrings.xml") || nome.startsWith("xl/worksheets/sheet")) {
                    partes.put(nome, zip.readAllBytes());
                }
            }
            lerTextos();
            lerAbas();
        } catch (IOException | XMLStreamException ex) {
            throw new ArquivoInvalidoException("Planilha .xlsx inválida: " + ex.getMessage(), ex);
        }
    }

    /** Abre o primeiro .xlsx de dentro de um .zip (como os pacotes do INEP). */
    public static LeitorXlsx deZip(InputStream zipExterno) throws ArquivoInvalidoException {
        try (ZipInputStream zip = new ZipInputStream(zipExterno)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.getName().toLowerCase().endsWith(".xlsx")) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    zip.transferTo(bytes);
                    return new LeitorXlsx(new ByteArrayInputStream(bytes.toByteArray()));
                }
            }
        } catch (IOException ex) {
            throw new ArquivoInvalidoException("Zip inválido: " + ex.getMessage(), ex);
        }
        throw new ArquivoInvalidoException("Nenhuma planilha .xlsx dentro do zip");
    }

    public List<String> getAbas() {
        return new ArrayList<>(arquivoPorAba.keySet());
    }

    /**
     * Linhas da aba, cada uma como vetor de textos pela posição da coluna (A = 0). Células vazias = "".
     */
    public List<String[]> linhas(String aba) throws ArquivoInvalidoException {
        String arquivo = arquivoPorAba.get(aba);
        byte[] xml = arquivo == null ? null : partes.get(arquivo);
        if (xml == null) {
            throw new ArquivoInvalidoException("Aba não encontrada: " + aba);
        }
        List<String[]> linhas = new ArrayList<>();
        try {
            XMLStreamReader r = fabrica().createXMLStreamReader(new ByteArrayInputStream(xml));
            List<String> atual = null;
            String ref = null;
            String tipo = null;
            StringBuilder valor = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("row")) {
                        atual = new ArrayList<>();
                    } else if (n.equals("c")) {
                        ref = r.getAttributeValue(null, "r");
                        tipo = r.getAttributeValue(null, "t");
                        valor = new StringBuilder();
                    } else if ((n.equals("v") || n.equals("t")) && valor != null) {
                        valor.append(r.getElementText());
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("c") && atual != null && valor != null) {
                        int col = ref == null ? atual.size() : coluna(ref);
                        while (atual.size() < col) {
                            atual.add("");
                        }
                        String v = valor.toString();
                        if ("s".equals(tipo)) {
                            int i = Integer.parseInt(v.strip());
                            v = i < textos.size() ? textos.get(i) : "";
                        }
                        atual.add(v);
                        valor = null;
                    } else if (n.equals("row") && atual != null) {
                        linhas.add(atual.toArray(new String[0]));
                        atual = null;
                    }
                }
            }
        } catch (XMLStreamException | NumberFormatException ex) {
            throw new ArquivoInvalidoException("Erro lendo a aba " + aba + ": " + ex.getMessage(), ex);
        }
        return linhas;
    }

    /** "A1" → 0, "B7" → 1, "AA3" → 26. */
    static int coluna(String ref) {
        int c = 0;
        for (int i = 0; i < ref.length() && Character.isLetter(ref.charAt(i)); i++) {
            c = c * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
        }
        return c - 1;
    }

    private static XMLInputFactory fabrica() {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false); // proteção contra XXE
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f;
    }

    private void lerTextos() throws XMLStreamException {
        byte[] xml = partes.get("xl/sharedStrings.xml");
        if (xml == null) {
            return;
        }
        XMLStreamReader r = fabrica().createXMLStreamReader(new ByteArrayInputStream(xml));
        StringBuilder atual = null;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                if (r.getLocalName().equals("si")) {
                    atual = new StringBuilder();
                } else if (r.getLocalName().equals("t") && atual != null) {
                    atual.append(r.getElementText());
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("si") && atual != null) {
                textos.add(atual.toString());
                atual = null;
            }
        }
    }

    private void lerAbas() throws XMLStreamException {
        Map<String, String> alvoPorId = new HashMap<>();
        byte[] rels = partes.get("xl/_rels/workbook.xml.rels");
        if (rels != null) {
            XMLStreamReader r = fabrica().createXMLStreamReader(new ByteArrayInputStream(rels));
            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("Relationship")) {
                    String alvo = r.getAttributeValue(null, "Target");
                    alvo = alvo.startsWith("/") ? alvo.substring(1) : "xl/" + alvo;
                    alvoPorId.put(r.getAttributeValue(null, "Id"), alvo);
                }
            }
        }
        byte[] wb = partes.get("xl/workbook.xml");
        if (wb == null) {
            return;
        }
        XMLStreamReader r = fabrica().createXMLStreamReader(new ByteArrayInputStream(wb));
        int n = 0;
        while (r.hasNext()) {
            if (r.next() == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("sheet")) {
                n++;
                String id = null;
                for (int i = 0; i < r.getAttributeCount(); i++) {
                    if (r.getAttributeLocalName(i).equals("id")) {
                        id = r.getAttributeValue(i);
                    }
                }
                String arquivo = id == null ? null : alvoPorId.get(id);
                arquivoPorAba.put(r.getAttributeValue(null, "name"),
                        arquivo == null ? "xl/worksheets/sheet" + n + ".xml" : arquivo);
            }
        }
    }
}
