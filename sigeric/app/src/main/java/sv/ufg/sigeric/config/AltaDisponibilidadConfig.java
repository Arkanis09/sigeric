package sv.ufg.sigeric.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.TransactionException;

import java.util.Set;

@Configuration
@EnableRetry
public class AltaDisponibilidadConfig {

    private static final Logger log = LoggerFactory.getLogger(AltaDisponibilidadConfig.class);
    private static final Set<String> OPERACIONES_SESION = Set.of("save", "findById", "deleteById", "findByIndexNameAndIndexValue");

    /** Deja constancia en el log de cada reintento (evidencia de que el sistema se recuperó solo). */
    @Bean
    RetryListener registroDeReintentos() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext ctx, RetryCallback<T, E> cb, Throwable e) {
                log.warn("Reintento {} tras fallo de BD ({}): {}", ctx.getRetryCount(), ctx.getAttribute(RetryContext.NAME), e.getMessage());
            }
        };
    }

    /**
     * Las sesiones de usuario se guardan en el clúster en cada petición. Si un nodo cae justo en ese
     * momento, se reintenta la operación para que el usuario no vea un error ni pierda su sesión.
     */
    @Bean
    static BeanPostProcessor reintentoEnSesiones() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof JdbcIndexedSessionRepository)) {
                    return bean;
                }
                ProxyFactory proxy = new ProxyFactory(bean);
                proxy.setProxyTargetClass(true);
                proxy.addAdvice((MethodInterceptor) inv -> {
                    if (!OPERACIONES_SESION.contains(inv.getMethod().getName())) {
                        return inv.proceed();
                    }
                    for (int intento = 1; ; intento++) {
                        try {
                            return ((ProxyMethodInvocation) inv).invocableClone().proceed();
                        } catch (DataAccessException | TransactionException e) {
                            if (intento >= 4) {
                                throw e;
                            }
                            log.warn("Sesión: reintento {} de {} tras fallo de BD: {}", intento, inv.getMethod().getName(), e.getMessage());
                            Thread.sleep(300L * intento);
                        }
                    }
                });
                return proxy.getProxy();
            }
        };
    }
}
