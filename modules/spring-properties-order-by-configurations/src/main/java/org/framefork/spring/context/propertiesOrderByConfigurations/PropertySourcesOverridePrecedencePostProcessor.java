package org.framefork.spring.context.propertiesOrderByConfigurations;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;

import java.util.Objects;

/**
 * Finds all registered {@link org.springframework.context.annotation.Configuration} classes,
 * that define {@link org.springframework.context.annotation.PropertySource}'s
 * and uses their {@link org.springframework.context.annotation.Import}'s to establish precedence.
 */
public class PropertySourcesOverridePrecedencePostProcessor implements BeanFactoryPostProcessor, PriorityOrdered, EnvironmentAware, ResourceLoaderAware
{

    private static final Logger log = LoggerFactory.getLogger(PropertySourcesOverridePrecedencePostProcessor.class);

    @Nullable
    private ConfigurableEnvironment environment;

    @Nullable
    private ResourceLoader resourceLoader;

    @Override
    public int getOrder()
    {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public void setEnvironment(final Environment environment)
    {
        this.environment = ConfigurableEnvironment.class.cast(environment);
    }

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory)
    {
        Objects.requireNonNull(environment, "environment must not be null");

        MutablePropertySources propertySources = environment.getPropertySources();

        // the MutablePropertySources works based on PropertySource's names... :garbage-fire:
        var allPropertySources = SpringConfigurationUtils.getPropertySources(propertySources);
        var resourcePropertySources = SpringConfigurationUtils.getResourcePropertySources(allPropertySources);
        if (resourcePropertySources.isEmpty()) {
            return; // no resources were loaded => ignore
        }

        // construct dependency graph and figure out resource priorities
        var configurations = new ConfigurationsAnalyzer(
            beanFactory,
            Objects.requireNonNull(environment, "environment must not be null"),
            Objects.requireNonNull(resourceLoader, "resourceLoader must not be null")
        );

        // we will use the first resource as a cursor and will start adding other resources after it
        String previousName = allPropertySources.get(Math.max(0, allPropertySources.indexOf(resourcePropertySources.getFirst()) - 1)).getName();
        for (var nextResource : configurations.getPropertySourcesOrder()) {
            String nextResourceName = nextResource.name();

            PropertySource<?> nextPropertySource = propertySources.get(nextResourceName);
            if (nextPropertySource == null) {
                log.debug("Skipping \"{}\", because it was not loaded into the current Context", nextResourceName);
                continue;
            }

            log.info("Moving \"{}\" after \"{}\"", nextResourceName, previousName);
            propertySources.addAfter(previousName, nextPropertySource);
            previousName = nextResourceName;
        }
    }

}
