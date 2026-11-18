package cr.ac.ucr.if0009.insecurepay.exception;

/**
 * Excepcion lanzada cuando el recurso solicitado no existe (HTTP 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
