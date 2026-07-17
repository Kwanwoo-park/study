package spring.study.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaBatchConfig {
    private final int chatBatchSize;

    public JpaBatchConfig(@Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") int chatBatchSize) {
        this.chatBatchSize = chatBatchSize;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernateBatchPropertiesCustomizer() {
        return properties -> {
            properties.put("hibernate.jdbc.batch_size", chatBatchSize);
            properties.put("hibernate.order_inserts", true);
            properties.put("hibernate.order_updates", true);
        };
    }

    @Bean
    public static BeanPostProcessor mysqlBatchStatementRewriter() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource dataSource) {
                    dataSource.addDataSourceProperty("rewriteBatchedStatements", true);
                }
                return bean;
            }
        };
    }
}
