package io.github.joaogclima30.slinsight.localizacao.dto;

import java.math.BigDecimal;

public record LocalizacaoResponse(
        Long id,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep,
        BigDecimal latitude,
        BigDecimal longitude
) {
}