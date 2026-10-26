package io.github.joaogclima30.slinsight.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException exception) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(
                        erro.getField(),
                        erro.getDefaultMessage()
                ));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Verifique os campos enviados"
        );

        problema.setTitle("Dados inválidos");
        problema.setProperty("campos", campos);

        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> tratarErroDoServico(ResponseStatusException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() != null
                        ? exception.getReason()
                        : "Não foi possível concluir a operação"
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarIntegridade(DataIntegrityViolationException exception) {
        if (possuiSqlState(exception, "23505")) {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Já existe um registro com os dados únicos informados"
            );

            problema.setTitle("Registro duplicado");

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(problema);
        }

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade dos dados"
        );

        problema.setTitle("Conflito de integridade");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problema);
    }

    private boolean possuiSqlState(Throwable exception, String sqlState) {
        Throwable causa = exception;

        while (causa != null) {
            if (causa instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            causa = causa.getCause();
        }

        return false;
    }
}