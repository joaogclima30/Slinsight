package io.github.joaogclima30.slinsight.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaResponse<T>(
        List<T> content,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}