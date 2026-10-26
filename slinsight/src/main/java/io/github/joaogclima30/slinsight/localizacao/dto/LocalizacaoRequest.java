package io.github.joaogclima30.slinsight.localizacao.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LocalizacaoRequest(

        @Size(max = 200)
        String logradouro,

        @Size(max = 20)
        String numero,

        @Size(max = 150)
        String complemento,

        @Size(max = 100)
        String bairro,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 100)
        String cidade,

        @NotBlank(message = "A UF é obrigatória")
        @Pattern(regexp = "[A-Z]{2}", message = "A UF deve conter duas letras maiúsculas")
        String uf,

        @Pattern(regexp = "[0-9]{8}", message = "O CEP deve conter oito números, sem hífen")
        String cep,

        @DecimalMin("-90")
        @DecimalMax("90")
        @Digits(integer = 3, fraction = 6)
        BigDecimal latitude,

        @DecimalMin("-180")
        @DecimalMax("180")
        @Digits(integer = 3, fraction = 6)
        BigDecimal longitude

) {

    @AssertTrue(message = "Latitude e longitude devem ser informadas juntas")
    public boolean isCoordenadasConsistentes() {
        return (latitude == null) == (longitude == null);
    }
}