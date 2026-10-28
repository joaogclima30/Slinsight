package io.github.joaogclima30.slinsight.suporte;

import java.util.List;
import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/** Impede que qualquer contexto de teste use DB_URL ou credenciais do ambiente. */
public final class BancoIsoladoCustomizerFactory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> type,
            List<ContextConfigurationAttributes> attributes) {
        return BancoIsolado.INSTANCE;
    }

    private enum BancoIsolado implements ContextCustomizer {
        INSTANCE;

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration configuration) {
            String url = "jdbc:tc:postgresql:17:///slinsight_test?TC_DAEMON=true";
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "postgres-exclusivo-testes", Map.of(
                    "spring.datasource.url", url,
                    "spring.datasource.username", "test",
                    "spring.datasource.password", "test",
                    "spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver",
                    "spring.flyway.url", url,
                    "spring.flyway.user", "test",
                    "spring.flyway.password", "test",
                    "spring.flyway.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver",
                    "spring.jpa.hibernate.ddl-auto", "validate")));
        }
    }
}
