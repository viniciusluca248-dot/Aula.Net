package br.uema.aulanet.exception;

/** O usuário está autenticado, mas não é dono do recurso (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
