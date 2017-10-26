# About


## Building 

This project uses [maven-toolchains-plugin][maven-toolchains-plugin], so you'll need to [setup toolchains][maven-toolchains-plugin-setup].  
Examples for various OS/architectures can be found [here][maven-central-earcam-toolchain] 

With toolchains configured, run `mvn clean install`.

When modifying the code beware/be-aware the build will fail if Maven POMs, Java source or Javascript source aren't formatted according to conventions (Apache 
Maven's standards for POMs, my own undocumented formatting for source).  To auto-format the lot, simply run `mvn -P '!strict,tidy'`.

To run [PiTest][pitest] use `mvn -P analyse`

To run against [SonarQube][sonarqube] use `mvn -P analyse,sonar`

### Building in Eclipse

The embedded maven instance in Eclipse fails as `${maven.home}` is not a directory.  To work around this, just switch to an external maven instance in Eclipse's run configuration/ 


### Building the site

As the plugins can't be used in the generation of their own site, build the site with:

		mvn -P 'analyse,report,site' clean install site   &&   mvn -P post-site   &&   mvn site:stage


## Roadmap

Lots TODO...


[maven-toolchains-plugin]: http://maven.apache.org/plugins/maven-toolchains-plugin/
[maven-toolchains-plugin-setup]: https://maven.apache.org/guides/mini/guide-using-toolchains.html
[maven-central-earcam-toolchain]: http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22io.earcam.maven.toolchain%22
[pitest]: http://pitest.org/
[sonarqube]: https://www.sonarqube.org/