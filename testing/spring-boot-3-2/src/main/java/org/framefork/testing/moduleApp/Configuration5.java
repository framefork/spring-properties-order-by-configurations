package org.framefork.testing.moduleApp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({Configuration4.class})
@PropertySource("classpath:config5.properties")
public class Configuration5
{

}
