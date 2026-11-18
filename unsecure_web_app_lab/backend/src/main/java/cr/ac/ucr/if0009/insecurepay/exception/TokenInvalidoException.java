package cr.ac.ucr.if0009.insecurepay.exception;

public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
