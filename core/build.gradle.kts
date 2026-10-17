loom {
      splitEnvironmentSourceSets()
}

/*
sourceSets {
    testmod {
        compileClasspath += sourceSets.main.compileClasspath + sourceSets.main.output
        runtimeClasspath += sourceSets.main.runtimeClasspath + sourceSets.main.output
        compileClasspath += sourceSets.client.compileClasspath + sourceSets.client.output
        runtimeClasspath += sourceSets.client.runtimeClasspath + sourceSets.client.output
    }
}

loom {
    createRemapConfigurations(sourceSets.testmod)
    mods {
      tlaapi_testmod {
          sourceSet(sourceSets.testmod)
      }
    }
    
    splitEnvironmentSourceSets()
}
*/