package br.com.gestorAssinaturas.adapter.in.web;

import br.com.gestorAssinaturas.domain.exception.AssinaturaJaAtivaException;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.exception.EmailJaCadastradoException;
import br.com.gestorAssinaturas.domain.exception.EstadoInvalidoException;
import br.com.gestorAssinaturas.domain.exception.PagamentoRecusadoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioInativoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssinaturaJaAtivaException.class)
    public ProblemDetail tratarAssinaturaJaAtiva(AssinaturaJaAtivaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ProblemDetail tratarEstadoInvalido(EstadoInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AssinaturaNaoEncontradaException.class)
    public ProblemDetail tratarAssinaturaNaoEncontrada(AssinaturaNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PagamentoRecusadoException.class)
    public ProblemDetail tratarPagamentoRecusado(PagamentoRecusadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ProblemDetail tratarUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ProblemDetail tratarEmailJaCadastrado(EmailJaCadastradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UsuarioInativoException.class)
    public ProblemDetail tratarUsuarioInativo(UsuarioInativoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
