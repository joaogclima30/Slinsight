package io.github.joaogclima30.slinsight.usuario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;
    @Column(nullable = false)
    private Boolean ativo = true;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    private void antesDeInserir() {
        criadoEm = Instant.now();
    }


}
