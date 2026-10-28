package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe criação e limites de localização")
class LocalizacaoRequestTests extends RequestSpecSupport {
    Cliente autor;
    @BeforeEach void preparar() { usuario("autor@example.com", "CIDADAO"); autor = login("autor@example.com"); }

    @ParameterizedTest(name = "quando latitude={0} e longitude={1}, então aceita os limites")
    @CsvSource({"0,0", "-90,-180", "90,180"})
    void limitesValidos(double latitude, double longitude) {
        var corpo = new HashMap<>(endereco()); corpo.put("latitude", latitude); corpo.put("longitude", longitude);
        esperar(autor.enviar("POST", "/localizacoes", corpo), 201);
        assertThat(contar("localizacoes")).isEqualTo(1);
    }

    @Test @DisplayName("it quando não informa coordenadas, então permite cadastrar endereço")
    void semCoordenadas() {
        var corpo = new HashMap<>(endereco()); corpo.remove("latitude"); corpo.remove("longitude");
        esperar(autor.enviar("POST", "/localizacoes", corpo), 201);
    }

    @ParameterizedTest(name = "quando falta {0}, então retorna 400 sem persistir")
    @ValueSource(strings = {"latitude", "longitude"})
    void coordenadaIncompleta(String campo) {
        var corpo = new HashMap<>(endereco()); corpo.remove(campo);
        esperar(autor.enviar("POST", "/localizacoes", corpo), 400);
        assertThat(contar("localizacoes")).isZero();
    }

    @ParameterizedTest(name = "quando {0}={1}, então retorna 400 sem persistir")
    @CsvSource({"latitude,90.000001", "latitude,-90.000001", "longitude,180.000001", "longitude,-180.000001", "latitude,1.1234567"})
    void coordenadaInvalida(String campo, String valor) {
        var corpo = new HashMap<>(endereco()); corpo.put(campo, new java.math.BigDecimal(valor));
        esperar(autor.enviar("POST", "/localizacoes", corpo), 400);
        assertThat(contar("localizacoes")).isZero();
    }

    @ParameterizedTest(name = "quando {0} é inválido, então retorna 400 sem persistir")
    @CsvSource({"uf,sp", "uf,SSP", "cep,01001-000", "cep,abcdefgh", "cidade,' '"})
    void enderecoInvalido(String campo, String valor) {
        var corpo = new HashMap<>(endereco()); corpo.put(campo, valor);
        esperar(autor.enviar("POST", "/localizacoes", corpo), 400);
        assertThat(contar("localizacoes")).isZero();
    }

    @Test @DisplayName("it quando repete o endereço, então permite duas localizações distintas")
    void duplicacaoPermitida() {
        long primeira = localizacao(autor); long segunda = localizacao(autor);
        assertThat(segunda).isNotEqualTo(primeira);
        assertThat(contar("localizacoes")).isEqualTo(2);
    }

    @Test @DisplayName("it quando cria sem CSRF, então retorna 403 sem persistir")
    void csrfObrigatorio() {
        esperar(autor.enviar("POST", "/localizacoes", endereco(), false, java.util.Map.of()), 403);
        assertThat(contar("localizacoes")).isZero();
    }

    @Test @DisplayName("it quando anônimo tenta criar com CSRF válido, então exige autenticação e não persiste")
    void anonimoNaoCria() {
        var c = anonimo(); c.csrf(); esperar(c.enviar("POST", "/localizacoes", endereco()), 401);
        assertThat(contar("localizacoes")).isZero();
    }
}
