package cr.ac.ucr.if0009.insecurepay.controller;

import cr.ac.ucr.if0009.insecurepay.exception.AccesoDenegadoException;
import cr.ac.ucr.if0009.insecurepay.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.insecurepay.exception.TokenInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Manejador global de excepciones. Todas las respuestas de error siguen el
 * contrato RFC 7807 (application/problem+json) mediante ProblemDetail de Spring 6.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 403 - Intento de acceso a un recurso ajeno (IDOR / BOLA)
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccesoDenegadoException ex) {
        return construir(HttpStatus.FORBIDDEN,
                "Acceso no autorizado a recurso ajeno",
                ex.getMessage(),
                "https://insecurepay.cr/errors/403");
    }

    // 404 - Recurso inexistente
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                ex.getMessage(),
                "https://insecurepay.cr/errors/404");
    }

    // 401 - Token ausente, mal formado, con firma invalida o expirado
    @ExceptionHandler({TokenInvalidoException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ProblemDetail> manejarNoAutenticado(Exception ex) {
        return construir(HttpStatus.UNAUTHORIZED,
                "Autenticacion requerida",
                "Token de autorizacion ausente, invalido o expirado.",
                "https://insecurepay.cr/errors/401");
    }

    private ResponseEntity<ProblemDetail> construir(HttpStatus status, String titulo,
                                                    String detalle, String tipo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        pd.setType(URI.create(tipo));
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(pd);
    }
}
