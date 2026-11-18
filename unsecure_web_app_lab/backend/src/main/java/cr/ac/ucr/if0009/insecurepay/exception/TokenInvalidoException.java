package cr.ac.ucr.if0009.insecurepay.exception;

/**
 * Excepcion lanzada cuando la cabecera Authorization no existe, no usa el
 * esquema Bearer o el JWT es invalido / expirado (HTTP 401).
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
