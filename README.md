# Spring properties order-by-configurations

This library reorders `*.properties` files priorities depending on which `@Configuration` classes they're defined on and how those classes are linked via `@Import`.
This enables you to define a stable ordering, making property resolution predictable.

## Installation

Just install the latest version of `org.framefork:spring-properties-order-by-configurations` dependency, and everything should start working - the configuration autoloads in a Spring Boot project.

Currently, the oldest supported Spring Boot is 3.2+

## Motivation

The author of this library typically structures their Spring Boot applications with the following illustrative modules:
* `app` (depends on `core-business-logic`)
* `core-business-logic` (depends on `common` and `core-separate-module`)
* `core-separate-module` (depends on `common`)
* `commons`

Where
* The `app` adds "entrypoints" (controllers, cli commands, etc.) and may also add some `.properties` file(s)
* The `core-*` have separate tests, and somewhat separate configurations with some `.properties` file(s)
* The `.properties` are loaded with `@PropertySource(...)` on a `@Configuration` class
* The `core-*` may want to define some "defaults" like `spring.main.web-application-type=none`, but the `app` would like it to be `spring.main.web-application-type=servlet`

With this, you might end up with working `core-*` but broken `app` because Spring may give the `.properties` in `core-*` a higher priority which causes the `spring.main.web-application-type=servlet` to be ignored.

This happens because Spring sorts the classes based on some arbitrary rule (or even better - randomly),
and the only ordering you can rely on is when you put both `@PropertySource`'s on the same `@Configuration` class.
But that kinda defeats the purpose of splitting the application into modules.

I think that it's reasonable to assume, that if `app` depends on `core-*` it might want to be able to **override** some default values provided by the `core-*`, and this library solves that.
It sorts the `.properties` based on the `@Import()`'s on your `@Configuration` classes, so that in `app` you can add `@Import` on some `core-*` `@Configuration` to say "I depend on this, allow me to override its properties".

For working example, look into the `testing/` directory.
