package io.github.joaogclima30.slinsight.localizacao.model;

import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "localizacoes")
public class Localizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(length = 200)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 150)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String uf;

    @Column(length = 8)
    private String cep;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

}
