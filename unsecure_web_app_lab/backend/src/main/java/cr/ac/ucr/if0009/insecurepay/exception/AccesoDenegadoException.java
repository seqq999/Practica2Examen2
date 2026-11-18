package cr.ac.ucr.if0009.insecurepay.exception;

/**
 * Excepcion de negocio lanzada cuando un usuario autenticado intenta acceder
 * a un recurso que no le pertenece (mitigacion IDOR / BOLA - OWASP A01:2021).
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
