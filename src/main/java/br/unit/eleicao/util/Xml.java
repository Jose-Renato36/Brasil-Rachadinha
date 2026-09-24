package br.unit.eleicao.util;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Leitura de XML com o parser que já vem no Java (DOM), com proteção contra entidades externas. */
public final class Xml {

    private Xml() {
    }

    public static Document ler(Path arquivo) throws ArquivoInvalidoException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(arquivo.toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ArquivoInvalidoException("XML inválido em " + arquivo.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /** Todos os elementos com a tag, em qualquer nível do documento. */
    public static List<Element> elementos(Document doc, String tag) {
        return lista(doc.getElementsByTagName(tag));
    }

    public static List<Element> elementos(Element pai, String tag) {
        return lista(pai.getElementsByTagName(tag));
    }

    private static List<Element> lista(NodeList nos) {
        List<Element> r = new ArrayList<>();
        for (int i = 0; i < nos.getLength(); i++) {
            Node n = nos.item(i);
            if (n instanceof Element) {
                r.add((Element) n);
            }
        }
        return r;
    }

    /** Texto do primeiro descendente com a tag, ou "" se não existir. */
    public static String texto(Element pai, String tag) {
        NodeList nos = pai.getElementsByTagName(tag);
        return nos.getLength() == 0 ? "" : nos.item(0).getTextContent().strip();
    }
}
