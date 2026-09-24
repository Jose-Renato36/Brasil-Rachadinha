package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Metadados;

import java.time.LocalDate;

/** Constituição, art. 14, § 3º, VI: idade mínima para cada cargo, conferida na data da posse. */
public class IdadeMinima extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return REQUISITOS;
    }

    @Override
    public String getTitulo() {
        return "Idade mínima";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        Cargo cargo = c.getTipoCargo();
        int minimo = cargo.getIdadeMinima();
        if (minimo == 0) {
            return null;
        }
        LocalDate posse = cargo.dataPosse(meta.getAnoEleicao());
        Integer idade = c.getIdade(posse);
        String regra = "A Constituição exige " + minimo + " anos para " + cargo.getRotulo().toLowerCase()
                + ", contados na data da posse (" + posse.getDayOfMonth() + "/" + posse.getMonthValue() + "/"
                + posse.getYear() + ").";
        if (idade == null) {
            return item(Avaliacao.SEM_DADOS, "Data de nascimento não informada", regra, "TSE");
        }
        if (idade < minimo) {
            return item(Avaliacao.NEGATIVO, idade + " anos na posse (mínimo: " + minimo + ")", regra, "TSE");
        }
        return item(Avaliacao.POSITIVO, idade + " anos na posse (mínimo: " + minimo + ")", regra, "TSE");
    }
}
