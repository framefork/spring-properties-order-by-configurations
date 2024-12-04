package org.framefork.spring.context.propertiesOrderByConfigurations;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class SpringConfigurationUtils
{

    private SpringConfigurationUtils()
    {
    }

    @SuppressWarnings({"unchecked"})
    static <T> Class<T> classForName(final String className)
    {
        try {
            return (Class<T>) Class.forName(className);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Cannot locale class '%s' on classpath: %s", className, e.getMessage()), e);
        }
    }

    static List<PropertySource<?>> getPropertySources(final MutablePropertySources propertySources)
    {
        List<PropertySource<?>> result = new ArrayList<>();
        propertySources.forEach(result::add);
        return result;
    }

    static List<ResourcePropertySource> getResourcePropertySources(final List<PropertySource<?>> propertySources)
    {
        return propertySources.stream()
            .filter(ResourcePropertySource.class::isInstance)
            .map(ResourcePropertySource.class::cast)
            .toList();
    }

    static AnnotationMetadata getBeanDefinitionMetadata(final BeanDefinition beanDefinition)
    {
        if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
            return annotatedBeanDefinition.getMetadata();
        }

        return AnnotationMetadata.introspect(getBeanDefinitionClass(beanDefinition));
    }

    static Class<?> getBeanDefinitionClass(final BeanDefinition beanDefinition)
    {
        if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
            var metadata = annotatedBeanDefinition.getMetadata();
            return unproxy(classForName(metadata.getClassName()));
        }

        if (beanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
            if (abstractBeanDefinition.hasBeanClass()) {
                return unproxy(abstractBeanDefinition.getBeanClass());
            }
        }

        return unproxy(classForName(Objects.requireNonNull(beanDefinition.getBeanClassName(), "beanDefinition.getBeanClassName() must not be null")));
    }

    static Class<?> unproxy(final Class<?> type)
    {
        // TODO
        return type;
    }

    static Set<Class<?>> getAllBeanTypes(final BeanDefinition beanDefinition)
    {
        Class<?> beanClass = getBeanDefinitionClass(beanDefinition);

        Set<Class<?>> beanTypes = new LinkedHashSet<>();
        beanTypes.add(beanClass);
        beanTypes.addAll(getAllSuperTypes(beanClass));

        return beanTypes;
    }

    static Set<Class<?>> getAllSuperTypes(final Class<?> type)
    {
        Set<Class<?>> types = new LinkedHashSet<>();

        Class<?> superclass = type.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            types.add(type);
            types.addAll(getAllSuperTypes(superclass));
        }

        for (Class<?> anInterface : type.getInterfaces()) {
            types.add(anInterface);
            types.addAll(getAllSuperTypes(anInterface));
        }

        return types;
    }

    static Set<Class<?>> getDirectImports(final AnnotationMetadata annotations)
    {
        return Optional.of(annotations)
            .map(metadata -> metadata.getAnnotationAttributes(Import.class.getName()))
            .map(attributes -> (Class<?>[]) attributes.get("value"))
            .map(classNames -> {
                Set<Class<?>> result = new LinkedHashSet<>();
                Collections.addAll(result, classNames);
                return Collections.unmodifiableSet(result); // we want it immutable, but want to preserve ordering
            })
            .orElseGet(Set::of);
    }

    static Set<PropertySourceResource> getPropertySourcesLocations(
        final AnnotationMetadata beanDefinitionMetadata,
        final PropertyResolver propertyResolver,
        final ResourceLoader resourceLoader
    )
    {
        List<PropertySourceResource> resources = new ArrayList<>();
        for (AnnotationAttributes propertySource : attributesForRepeatable(beanDefinitionMetadata, PropertySources.class, org.springframework.context.annotation.PropertySource.class)) {
            Optional<String> name = Optional.ofNullable(propertySource.getString("name")).filter(StringUtils::hasLength);

            for (String location : propertySource.getStringArray("value")) {
                String resolvedLocation = propertyResolver.resolveRequiredPlaceholders(location);
                Resource resource = resourceLoader.getResource(resolvedLocation);

                resources.add(new PropertySourceResource(
                    resource,
                    name.orElseGet(resource::getDescription),
                    location
                ));
            }
        }

        Collections.reverse(resources); // this is important to preserve Spring's precedence

        return Collections.unmodifiableSet(new LinkedHashSet<>(resources));
    }

    static Set<AnnotationAttributes> attributesForRepeatable(final AnnotationMetadata metadata, final Class<? extends Annotation> containerClass, final Class<? extends Annotation> annotationClass)
    {
        return metadata.getMergedRepeatableAnnotationAttributes(annotationClass, containerClass, false, false);
    }

    record PropertySourceResource(
        Resource resource,
        String name,
        String location
    )
    {

    }

}
