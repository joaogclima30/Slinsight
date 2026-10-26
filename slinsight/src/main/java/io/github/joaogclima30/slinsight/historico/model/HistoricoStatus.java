package io.github.joaogclima30.slinsight.historico.model;

import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false, updatable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30, updatable = false)
    private StatusOcorrencia statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30,
            updatable = false)
    private StatusOcorrencia statusNovo;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String observacao;

    @Column(name = "alterado_em", nullable = false, updatable = false)
    private Instant alteradoEm;

    protected HistoricoStatus() {
    }

    @PrePersist
    private void antesDeInserir() {
        alteradoEm = Instant.now();
    }

}