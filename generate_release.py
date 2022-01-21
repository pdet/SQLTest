import os

def generate_gradle_build():
    f = open("release.config", "r")
    version_number = int(f.readline().split("\n")[0]) + 1
    gradle_intellij_version = f.readline().split("\n")[0]
    f.close()

    f = open("release.config", "w")
    f.write(str(version_number))
    f.close()

    build_gradle = """plugins {
        id 'java'
        id 'org.jetbrains.intellij' version '%s'
    }

    group 'pdet.github.io'
    version '1.0.%d'

    repositories {
        mavenCentral()
    }

    // See https://github.com/JetBrains/gradle-intellij-plugin/
    intellij {
        version =  'LATEST-EAP-SNAPSHOT'
        type = 'CL'
    }

    sourceSets.main.java.srcDirs 'src/main/gen' """ % (gradle_intellij_version, version_number)

    f = open("build.gradle", "w")
    f.write(build_gradle)
    f.close()

generate_gradle_build()
os.system('gradle :buildPlugin')