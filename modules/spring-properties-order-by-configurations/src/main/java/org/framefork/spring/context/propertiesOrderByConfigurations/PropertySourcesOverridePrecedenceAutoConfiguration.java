package org.framefork.spring.context.propertiesOrderByConfigurations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class PropertySourcesOverridePrecedenceAutoConfiguration
{

    @Bean
    public PropertySourcesOverridePrecedencePostProcessor propertySourcesOverridePrecedencePostProcessor()
    {
        return new PropertySourcesOverridePrecedencePostProcessor();
    }

}
