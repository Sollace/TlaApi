import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    id("io.github.dexman545.outlet")
}

outlet {
    if ("true".equals(rootProject.property("minecraft_version_allow_snapshot") as String)) {
        allowedReleaseTypes.add(ReleaseType.SNAPSHOT)
    } else {
        allowedReleaseTypes.remove(ReleaseType.SNAPSHOT)
    }
    mcVersionRange = rootProject.property("minecraft_version_range") as String
}

System.out.println("Outlet MC Versions: ${outlet.mcVersions()}")
