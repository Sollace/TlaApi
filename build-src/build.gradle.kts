plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("mapped-dependency-helper") {
            id = "org.mapped-dependency"
            implementationClass = "MappedDependencyPlugin"
        }
    }
}
