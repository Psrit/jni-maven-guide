# jni-maven-guide

本仓库复刻自[ wzt3309 的仓库](https://github.com/wzt3309/jni-maven-guide)。相较于原仓库，本仓库使用 CMake 构建 native 动态链接库，且支持将动态链接库打包进入 jar 包中，jar 包可自动加载动态库。

## 1. 目录结构

```
.
├── CMakeLists.txt
├── README.md
├── autobuild.sh  # 执行CMake配置、生成和构建的辅助脚本，供mvn调用（参见 pom.xml）
├── build  # CMake的构建输出目录
│   ├── CMakeCache.txt
│   ├── CMakeFiles
│   ├── Makefile
│   ├── cmake_install.cmake
│   ├── copy_libs.cmake
│   └── libjni-lib.so
├── clib  # CMake构建得到的动态库将集中于此文件夹下，供mvn打包使用
│   └── linux-x64  # 动态库依生成平台和CPU架构集中
│       └── libjni-lib.so
├── dependency-reduced-pom.xml  # 由maven-shade-plugin生成
├── pom.xml
├── src  # 源文件目录
│   └── main
│       ├── c
│       └── java
└── target  # mvn的构建输出目录
    ├── classes
    ├── generated-sources
    ├── headers
    ├── jni-1.0.0.jar
    ├── maven-archiver
    ├── maven-status
    └── original-jni-1.0.0.jar
```

## 2. 构建顺序

### 2.1 为 JNI 生成头文件
将 maven-compiler-plugin 的 compile 目标附加到 validate 阶段上。在 `configuration/compilerArgs` 中指定 `javac` 的编译参数 `-h target/headers` 以使 `javac` 为 JNI 接口在 target/headers 目录下生成头文件。
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals>
        <goal>compile</goal>
      </goals>
      <configuration>
        <compilerArgs>
          <arg>-h</arg>
          <arg>target/headers</arg>
        </compilerArgs>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 2.2 编写 C/C++ 代码并编译

C/C++ 部分动态链接库的编译由 CMake 管理。需要注意两点：

1. 将上一步 JNI 头文件的保存目录 target/headers 加入生成目标的包含目录（例如，通过 `target_include_directories`）；

2. 自动将编译得到的库文件放入 clib/\${OS_TYPE}-\${PROCESSOR_TYPE} 目录下，通过如下代码实现：

    ```cmake
    # 生成复制命令并缓存到 copy_libs.cmake 中
    file(GENERATE OUTPUT copy_libs.cmake
        CONTENT "file(COPY $<TARGET_FILE:jni-lib> DESTINATION ${PROJECT_SOURCE_DIR}/clib/${OS_TYPE}-${PROCESSOR_TYPE})"
    )
    # 调用 copy_libs.cmake，将动态链接库复制到 clib/${OS_TYPE}-${PROCESSOR_TYPE} 目录
    add_custom_command(TARGET jni-lib
        POST_BUILD COMMAND ${CMAKE_COMMAND} -P copy_libs.cmake)
    ```
  
为了让动态库的 CMake 构建过程包含在 Java 构建过程中，编写 autobuild.sh 文件并由 mvn 自动调用（实现于 pom.xml）：
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <id>build-c</id>
      <phase>compile</phase>
      <goals>
        <goal>exec</goal>
      </goals>
      <configuration>
        <executable>${project.basedir}/autobuild.sh</executable>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 2.3 动态库打包

生成的动态链接库将作为资源文件被打包进 jar 中。为了实现这一目标，首先添加与资源文件操作相关的插件 maven-resources-plugin，在 compile 阶段执行目标 copy-resources，将 clib 目录下的内容（\${OS_TYPE}-\${PROCESSOR_TYPE}/libjni-lib.so，含目录层次结构）复制到 target/classes（对应于编译后 jar 的 CLASSPATH）。

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-resources-plugin</artifactId>
  <executions>
    <execution>
      <id>copy-clib</id>
      <phase>compile</phase>
      <goals>
        <goal>copy-resources</goal>
      </goals>
      <configuration>
        <resources>
          <resource>
            <directory>${project.basedir}/clib</directory>
          </resource>
        </resources>
        <outputDirectory>${project.build.outputDirectory}</outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

然后添加 maven-shade-plugin 自定义打包内容。实际上，下面的配置仅仅指定了将哪些第三方包打包进入 jar 中，动态库的打包已经自动执行了。
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.0</version>
  <executions>
    <execution>
      <id>package-with-third-parties</id>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <artifactSet>
      <includes>
        <include>org.apache.commons:commons-lang3</include>
      </includes>
      <excludes>
        <exclude>classworlds:classworlds</exclude>
        <exclude>junit:junit</exclude>
        <exclude>jmock:*</exclude>
        <exclude>*:xml-apis</exclude>
        <exclude>org.apache.maven:lib:tests</exclude>
        <exclude>log4j:log4j:jar:</exclude>
      </excludes>
    </artifactSet>
  </configuration>
</plugin>
```

### 2.4 动态库调用

为了解决 `System.load` 只能接受动态链接库的绝对路径而无法加载 jar 包内部动态库的问题，实现了 `learn.jni.LibLoader` 类，该类提供 public 方法 `load`，可接受 jar 包内的动态库名（不包含任何父目录、无后缀）作为参数，自动完成动态库的加载。例如，在 x64 Linux 系统下 `LibLoader.load("libjni-lib")` 将加载 jar 包中 linux-x64/libjni-lib.so 路径指定的动态库。

该方法的原理是复制 jar 包内的动态库到某临时文件，然后通过 `System.load` 加载该临时文件。具体如下：

1. 使用 `LibLoader.class.getResourceAsStream` 读取 jar 包内动态库的二进制内容并写入到一个临时文件（通过 `File.createTempFile` 创建）中。

2. 假设该临时文件对象为 `libTempFile`，`System.load(libTempFile.getCanonicalPath())` 调用将加载该临时文件作为实际使用的动态库。

更详细的内容参见源代码。
