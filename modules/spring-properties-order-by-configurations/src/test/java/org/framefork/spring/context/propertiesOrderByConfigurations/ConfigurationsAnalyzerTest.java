package org.framefork.spring.context.propertiesOrderByConfigurations;

import org.framefork.spring.context.propertiesOrderByConfigurations.SpringConfigurationUtils.PropertySourceResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
        ConfigurationsAnalyzerTest.App.class,
    }
)
class ConfigurationsAnalyzerTest
{

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    ResourceLoader resourceLoader;

    @Test
    public void sorting()
    {
        var analyzer = new ConfigurationsAnalyzer(beanFactory, environment, resourceLoader);

        var sortedConfigurations = analyzer.getSortedConfigurationClasses()
            .stream()
            .filter(config -> config.getBeanClass().getName().startsWith(ConfigurationsAnalyzerTest.class.getPackageName() + "."))
            .toList();
        assertThat(sortedConfigurations)
            .map(config -> config.getBeanClass().getName())
            .containsExactly(
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$AppConfiguration5",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$AppConfiguration6",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$AppConfiguration7",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$AppConfiguration4",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$CoreConfiguration1",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$CoreConfiguration3",
                "org.framefork.spring.context.propertiesOrderByConfigurations.ConfigurationsAnalyzerTest$App$CoreConfiguration2",
                "org.framefork.spring.context.propertiesOrderByConfigurations.PropertySourcesOverridePrecedenceAutoConfiguration"
            );

        var orderedPropertySources = analyzer.getPropertySourcesOrder();
        assertThat(orderedPropertySources)
            .map(PropertySourceResource::name)
            .containsExactly(
                "class path resource [config5.properties]",
                "class path resource [config62.properties]",
                "class path resource [config61.properties]",
                "class path resource [config72.properties]",
                "class path resource [config71.properties]",
                "class path resource [application.properties]",
                "class path resource [config1.properties]",
                "class path resource [config3.properties]"
            );
    }

    @SpringBootApplication
    @Import({
        App.AppConfiguration5.class,
        App.AppConfiguration6.class,
        App.AppConfiguration7.class,
    })
    @SuppressWarnings("PrivateConstructorForUtilityClass")
    public static class App
    {

        @Configuration
        @Import({AppConfiguration4.class})
        @PropertySource(value = "classpath:config5.properties", ignoreResourceNotFound = true)
        public static class AppConfiguration5
        {

        }

        @Configuration
        @Import({AppConfiguration4.class})
        @PropertySources({
            @PropertySource(value = "classpath:config61.properties", ignoreResourceNotFound = true),
            @PropertySource(value = "classpath:config62.properties", ignoreResourceNotFound = true),
        })
        public static class AppConfiguration6
        {

        }

        @Configuration
        @PropertySource(value = "classpath:config71.properties", ignoreResourceNotFound = true)
        @PropertySource(value = "classpath:config72.properties", ignoreResourceNotFound = true)
        public static class AppConfiguration7
        {

        }

        @Configuration
        @Import({CoreConfiguration1.class})
        @PropertySource("classpath:application.properties")
        public static class AppConfiguration4
        {

        }

        @Configuration
        @Import({
            CoreConfiguration3.class,
            CoreConfiguration2.class,
        })
        @PropertySource(value = "classpath:config1.properties", ignoreResourceNotFound = true)
        public static class CoreConfiguration1
        {

        }

        @Configuration
        public static class CoreConfiguration2
        {

        }

        @Configuration
        @PropertySource(value = "classpath:config3.properties", ignoreResourceNotFound = true)
        public static class CoreConfiguration3
        {

        }

    }

}
