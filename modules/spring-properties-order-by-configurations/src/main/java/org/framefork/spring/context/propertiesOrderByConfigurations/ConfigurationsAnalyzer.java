package org.framefork.spring.context.propertiesOrderByConfigurations;

import org.framefork.spring.context.propertiesOrderByConfigurations.SpringConfigurationUtils.PropertySourceResource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

class ConfigurationsAnalyzer
{

    private final List<ConfigurationClass> configurations;
    private final List<ConfigurationClass> sortedConfigurationClasses;

    ConfigurationsAnalyzer(
        final ConfigurableListableBeanFactory beanFactory,
        final ConfigurableEnvironment environment,
        final ResourceLoader resourceLoader
    )
    {
        this.configurations = findConfigurations(beanFactory, environment, resourceLoader);
        this.sortedConfigurationClasses = getConfigurationsSortedByImports();
    }

    List<ConfigurationClass> getSortedConfigurationClasses()
    {
        return List.copyOf(sortedConfigurationClasses);
    }

    List<PropertySourceResource> getPropertySourcesOrder()
    {
        return sortedConfigurationClasses.stream()
            .flatMap(configurationClass -> configurationClass.propertySources().stream())
            .toList();
    }

    private List<ConfigurationClass> findConfigurations(
        final ConfigurableListableBeanFactory beanFactory,
        final ConfigurableEnvironment environment,
        final ResourceLoader resourceLoader
    )
    {
        List<ConfigurationClass> result = new ArrayList<>();

        Set<String> beanNames = new HashSet<>();
        beanNames.addAll(List.of(beanFactory.getBeanNamesForAnnotation(Configuration.class)));
        beanNames.addAll(List.of(beanFactory.getBeanNamesForAnnotation(AutoConfiguration.class)));

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            AnnotationMetadata annotations = SpringConfigurationUtils.getBeanDefinitionMetadata(beanDefinition);

            result.add(new ConfigurationClass(
                beanDefinition,
                annotations,
                SpringConfigurationUtils.getPropertySourcesLocations(annotations, environment, resourceLoader),
                SpringConfigurationUtils.getDirectImports(annotations)
            ));
        }

        // stable ordering, to remove the unpredictability of classpath
        result.sort(Comparator.comparing(config -> config.getBeanClass().getName(), Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    private List<ConfigurationClass> getConfigurationsSortedByImports()
    {
        var dependencyGraphRoots = collectGraphRoots();

        // sort configuration classes using BFS on the dependency graph
        Set<ConfigurationClass> result = new LinkedHashSet<>(configurations.size());
        for (var root : dependencyGraphRoots.values()) {
            root.breadthFirstSearch(node -> result.add(node.configurationClass()));
        }

        return List.copyOf(result);
    }

    private Map<ConfigurationClass, DependencyNode> collectGraphRoots()
    {
        var dependencies = collectDependencyGraph();

        // find all roots in the graph, preserving stable ordering
        var result = new LinkedHashMap<>(dependencies);
        for (DependencyNode dependencyNode : dependencies.values()) {
            // configurations that are being imported somewhere are not roots
            dependencyNode.children().stream()
                .map(DependencyNode::configurationClass)
                .forEach(result::remove);
        }

        return result;
    }

    private Map<ConfigurationClass, DependencyNode> collectDependencyGraph()
    {
        var configurationClassTypes = collectAllConfigurationTypes();

        // we want to preserve stable ordering
        Map<ConfigurationClass, DependencyNode> result = new LinkedHashMap<>();
        configurations.forEach(configurationClass -> result.put(configurationClass, new DependencyNode(configurationClass)));

        // build dependency graph using imports on individual configuration classes
        for (var entry : result.entrySet()) {
            ConfigurationClass configurationClass = entry.getKey();
            DependencyNode dependencyNode = entry.getValue();

            for (Class<?> directImport : configurationClass.directImports()) {
                ConfigurationClass importedConfigurationClass = configurationClassTypes.get(directImport);
                if (importedConfigurationClass == null) {
                    continue;
                }
                dependencyNode.addChild(
                    Objects.requireNonNull(result.get(importedConfigurationClass), "result.get(importedConfigurationClass) must not be null")
                );
            }
        }

        return result;
    }

    private Map<Class<?>, ConfigurationClass> collectAllConfigurationTypes()
    {
        Map<Class<?>, ConfigurationClass> result = new HashMap<>();
        Set<Class<?>> duplicates = new HashSet<>();

        for (ConfigurationClass configuration : configurations) {
            for (Class<?> beanType : configuration.getAllBeanTypes()) {
                if (result.containsKey(beanType)) {
                    duplicates.add(beanType);
                }
                result.put(beanType, configuration);
            }
        }

        duplicates.forEach(result::remove);

        return Map.copyOf(result);
    }

    record DependencyNode(
        ConfigurationClass configurationClass,
        List<DependencyNode> children
    )
    {

        DependencyNode(final ConfigurationClass configurationClass)
        {
            this(configurationClass, new ArrayList<>());
        }

        void addChild(final DependencyNode node)
        {
            children.add(Objects.requireNonNull(node, "node must not be null"));
        }

        void breadthFirstSearch(final Consumer<DependencyNode> visitor)
        {
            Set<DependencyNode> visited = new HashSet<>();
            Deque<DependencyNode> queue = new ArrayDeque<>();

            Consumer<DependencyNode> queueingVisitor = node -> {
                if (visited.add(node)) { // true if set did not already contain the specified element
                    queue.add(node);
                    visitor.accept(node);
                }
            };

            // start with root
            queueingVisitor.accept(this);

            // visit children and queue them for inspection
            while (!queue.isEmpty()) {
                queue.removeFirst()
                    .children()
                    .forEach(queueingVisitor);
            }
        }

    }

    record ConfigurationClass(
        BeanDefinition beanDefinition,
        AnnotationMetadata annotations,
        Set<PropertySourceResource> propertySources,
        Set<Class<?>> directImports
    )
    {

        public Set<Class<?>> getAllBeanTypes()
        {
            return SpringConfigurationUtils.getAllBeanTypes(beanDefinition);
        }

        public Class<?> getBeanClass()
        {
            return SpringConfigurationUtils.getBeanDefinitionClass(beanDefinition);
        }

        @Override
        public String toString()
        {
            return "ConfigurationClass[" + getBeanClass() + "]";
        }

    }

}
