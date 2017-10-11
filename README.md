Dokany-Kotlin
======

## Introduction
Dokany-Kotlin is a Kotlin wrapper for [Dokany 1.x release](https://github.com/dokan-dev/dokany/releases) and above.

It is ported from [Dokany Java](https://github.com/dokan-dev/dokan-java)

## Runtime Dependencies
- [Java JRE 1.8](https://java.com/en/download/manual.jsp)

All dependencies can be seen [here](build.gradle).

- [JNA](https://github.com/java-native-access/jna) - provides access to [native Dokany functions](https://dokan-dev.github.io/dokany-doc/html/struct_d_o_k_a_n___o_p_e_r_a_t_i_o_n_s.html)
- [Kotlin](https://kotlinlang.org/)
- [SLF4J](https://www.slf4j.org/)
- [Logback](https://logback.qos.ch/)
	
## How to Build
Requires [Java JDK 1.8](http://www.azul.com/downloads/zulu/)

### Gradle
Add the following to your `build.gradle`:

```groovy
// deployed artifacts coming soon

repositories {
  jcenter() // or mavenCentral()
}
dependencies {
  compile 'com.staticbloc:dokany-kotlin:0.0.1'
}
```

## Development Examples
For an example on how to develop using this library, see the [samples module](samples/src/main/kotlin/com/dokany/kotlin/).
