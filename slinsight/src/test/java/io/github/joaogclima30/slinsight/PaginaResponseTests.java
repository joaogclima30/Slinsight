package io.github.joaogclima30.slinsight;

import io.github.joaogclima30.slinsight.common.dto.PaginaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginaResponseTests {

    @Test
    void deveConverterConteudoEMetadadosDaPagina() {
        var pagina = new PageImpl<>(List.of("A", "B"), PageRequest.of(1, 2), 6);

        var resposta = PaginaResponse.de(pagina);

        assertEquals(List.of("A", "B"), resposta.content());
        assertEquals(1, resposta.pagina());
        assertEquals(2, resposta.tamanho());
        assertEquals(6L, resposta.totalElementos());
        assertEquals(3, resposta.totalPaginas());
        assertFalse(resposta.primeira());
        assertFalse(resposta.ultima());
    }

    @Test
    void deveConverterPaginaVazia() {
        var pagina = new PageImpl<String>(
                List.of(),
                PageRequest.of(0, 10),
                0);

        var resposta = PaginaResponse.de(pagina);

        assertTrue(resposta.content().isEmpty());
        assertEquals(0, resposta.pagina());
        assertEquals(10, resposta.tamanho());
        assertEquals(0L, resposta.totalElementos());
        assertEquals(0, resposta.totalPaginas());
        assertTrue(resposta.primeira());
        assertTrue(resposta.ultima());
    }
}