package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monta o quadro "Preparo para o cargo": aplica cada critério ao candidato (polimorfismo: a lista
 * guarda {@link CriterioPreparo} e cada subclasse sabe se avaliar).
 */
public class QuadroPreparo {

    private final List<CriterioPreparo> criterios = new ArrayList<>();

    public QuadroPreparo() {
        criterios.add(new IdadeMinima());
        criterios.add(new RegistroCandidatura());
        criterios.add(new Formacao());
        criterios.add(new ExperienciaEletiva());
        criterios.add(new ExperienciaMesmaFuncao());
        criterios.add(new AlertaContasTcu());
        criterios.add(new AlertaCassacao());
        criterios.add(new AlertaSancoes());
    }

    public List<CriterioPreparo> getCriterios() {
        return Collections.unmodifiableList(criterios);
    }

    public List<ItemPreparo> avaliar(Candidato c, Metadados meta) {
        List<ItemPreparo> itens = new ArrayList<>();
        for (CriterioPreparo criterio : criterios) {
            ItemPreparo item = criterio.avaliar(c, meta);
            if (item != null) {
                itens.add(item);
            }
        }
        return itens;
    }
}
