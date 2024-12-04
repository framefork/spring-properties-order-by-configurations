package org.framefork.testing.moduleCore;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({
    Configuration2.class,
    Configuration3.class,
})
@PropertySource("classpath:config1.properties")
public class Configuration1
{

}
