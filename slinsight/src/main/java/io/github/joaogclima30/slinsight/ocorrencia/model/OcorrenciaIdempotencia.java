package io.github.joaogclima30.slinsight.ocorrencia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ocorrencia_idempotencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ocorrencia_idempotencia_usuario_chave",
                columnNames = {"usuario_id", "chave"}))
@Getter
@Setter
@NoArgsConstructor
public class OcorrenciaIdempotencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String chave;

    @Column(nullable = false, length = 64)
    private String assinatura;

    @Column(name = "ocorrencia_id", nullable = false)
    private Long ocorrenciaId;
}