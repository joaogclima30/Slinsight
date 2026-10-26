package io.github.joaogclima30.slinsight.ocorrencia.specification;

import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OcorrenciaSpecification {

    private OcorrenciaSpecification() {
    }

    public static Specification<Ocorrencia> comFiltros(Long usuarioId, Long categoriaId, StatusOcorrencia status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (usuarioId != null) {
                condicoes.add(criteriaBuilder.equal(
                        root.get("usuario").get("id"),
                        usuarioId));
            }

            if (categoriaId != null) {
                condicoes.add(criteriaBuilder.equal(
                        root.get("categoria").get("id"),
                        categoriaId));
            }

            if (status != null) {
                condicoes.add(criteriaBuilder.equal(
                        root.get("status"),
                        status));
            }

            return criteriaBuilder.and(
                    condicoes.toArray(new Predicate[0]));
        };
    }
}