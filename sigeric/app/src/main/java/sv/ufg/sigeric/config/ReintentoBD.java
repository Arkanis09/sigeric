package sv.ufg.sigeric.config;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import java.lang.annotation.*;

/**
 * Reintenta la operación completa (con una transacción nueva) si falla por la caída de un nodo
 * de la base de datos justo mientras se ejecutaba. El driver ya se habrá cambiado a otro nodo.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(retryFor = {TransientDataAccessException.class, DataAccessResourceFailureException.class,
        RecoverableDataAccessException.class, CannotCreateTransactionException.class, TransactionSystemException.class},
        maxAttempts = 4, backoff = @Backoff(delay = 300, multiplier = 2))
public @interface ReintentoBD {
}
