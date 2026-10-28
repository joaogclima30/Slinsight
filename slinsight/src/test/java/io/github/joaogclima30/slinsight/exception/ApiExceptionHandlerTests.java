package io.github.joaogclima30.slinsight.exception;

import io.github.joaogclima30.slinsight.exception.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar400ParaParametroInvalido() throws Exception {
        mockMvc.perform(get("/teste/usuarios/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value(
                        "O parâmetro 'id' possui um valor inválido"
                ))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    record DadosTeste(
            @NotBlank(message = "O nome é obrigatório")
            String nome
    ) {
    }

    @RestController
    static class ControllerDeTeste {

        @GetMapping("/teste/usuarios/{id}")
        public String buscar(@PathVariable("id") Long id) {
            return "ok";
        }

        @PostMapping("/teste/usuarios")
        public String criar(@Valid @RequestBody DadosTeste dados) {
            return "ok";
        }

    }

    @Test
    void deveRetornar400ParaCampoInvalido() throws Exception {
        mockMvc.perform(post("/teste/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome": "   "}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos.nome").value(
                        "O nome é obrigatório"
                ))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void deveRetornar400ParaJsonInvalido() throws Exception {
        mockMvc.perform(post("/teste/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(
                        "Corpo da requisição inválido"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "O corpo da requisição está ausente, contém JSON inválido "
                                + "ou possui valores incompatíveis com os campos"
                ))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
