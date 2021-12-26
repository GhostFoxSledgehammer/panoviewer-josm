## Setting up your local git-repo

```shell
git clone git@gitlab.com:GhostFoxSledgehammer/panoviewer-josm.git
cd panoviewer-josm
```

## Building the plugin with Gradle

This project uses the so-called Gradle wrapper. That means you have to install nothing on your machine in order
to build the project. The wrapper consists of the two scripts `gradlew` (for UNIX-based systems like Mac and Linux)
and `gradlew.bat` (for systems running Windows). The following examples shows the commands for Linux/Mac users,
Windows users can simply replace `./gradlew` with `./gradlew.bat`.

If you develop using the Eclipse IDE, run the following command before opening the project in Eclipse. This will download the dependencies and tells Eclipse about where these dependencies are located on your machine:
```shell
./gradlew eclipse
```

For just building the jar-file for the plugin, run
```shell
./gradlew jar
```

If you also want to run the unit tests, create a FindBugs report and a code coverage report, then the following command is for you:
```shell
./gradlew build
```
(look for the reports in the directory `build/reports` and for the packaged `panoviewer.jar` in the directory `build/libs`)

And finally, you can execute the following to build the plugin from source, and run the latest JOSM with the Mapillary plugin already loaded.
This works regardless if you have JOSM installed, or which version of it. Any already present JOSM-installation stays untouched by the following command.
```shell
./gradlew runJosm
```

For info about other available tasks you can run
```shell
./gradlew tasks
```
