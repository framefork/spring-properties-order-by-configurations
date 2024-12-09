package org.framefork.testing;

import org.framefork.testing.moduleApp.App;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
        App.class,
    },
    properties = {
        "testing.different-property=tests"
    }
)
class AppTest
{

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void springVersion()
    {
        assertThat(SpringBootVersion.getVersion()).isEqualTo("3.2.0");
    }

    @Test
    public void configsResolution()
    {
        var sources = environment.getPropertySources();
        var resourceSources = sources.stream()
            .filter(source -> source instanceof ResourcePropertySource || source instanceof OriginTrackedMapPropertySource)
            .map(PropertySource::getName)
            .toList();

        assertThat(resourceSources).containsExactly(
            "class path resource [config5.properties]",
            "class path resource [config1.properties]",
            "class path resource [config3.properties]",
            "Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'"
        );

        String testingProperty = environment.getProperty("testing.property");
        assertThat(testingProperty).isEqualTo("5");
    }

}
