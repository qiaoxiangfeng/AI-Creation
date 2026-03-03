# JDK21 配置说明

本项目使用JDK21进行编译和运行。

## 系统要求

- JDK 17或21 (当前系统已安装JDK17，推荐升级到JDK21)
- Maven 3.6+

## 快速开始（当前系统）

```bash
# 临时设置（在当前终端会话中有效）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 编译项目
mvn clean compile
```

## 当前系统状态

✅ **JDK17已安装**：`/Library/Java/JavaVirtualMachines/jdk-17.jdk`
❌ **JDK21未安装**：需要下载安装JDK21才能使用

## Maven编译时指定JDK的方法

### 方法1：设置JAVA_HOME和PATH环境变量（推荐）

```bash
# 设置JAVA_HOME为JDK17的安装路径（当前系统）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 或者在.zshrc中永久设置
echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# 编译项目
mvn clean compile
```

**JDK21安装说明**：
1. 下载JDK21：https://adoptium.net/temurin/releases/
2. 安装到 `/Library/Java/JavaVirtualMachines/`
3. 修改JAVA_HOME路径为JDK21的路径
4. 修改pom.xml中的版本为21

### 方法2：命令行临时指定

```bash
# 直接在命令行中指定JAVA_HOME
JAVA_HOME=/path/to/jdk21 mvn clean compile

# 或者使用env命令
env JAVA_HOME=/path/to/jdk21 mvn clean compile
```

### 方法3：使用Maven Toolchains

在`~/.m2/toolchains.xml`中配置：

```xml
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>21</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk21</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

然后在pom.xml中添加：

```xml
<plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-toolchains-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
      <execution>
        <goals>
          <goal>toolchain</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <toolchains>
        <jdk>
          <version>21</version>
          <vendor>openjdk</vendor>
        </jdk>
      </toolchains>
    </configuration>
  </plugin>
</plugins>
```

## 验证JDK版本

```bash
# 检查当前Java版本
java -version

# 检查Maven使用的Java版本
mvn --version

# 在项目中检查
mvn help:system | grep java.home
```

## 常见问题

### 编译错误："无效的目标发行版: 21"

**原因**：系统使用的JDK版本不是21

**解决**：
1. 检查`java -version`输出
2. 确认JAVA_HOME指向JDK21
3. 重新启动终端或IDE

### 依赖版本冲突

**原因**：某些依赖是为旧版JDK编译的

**解决**：
1. 清理Maven缓存：`rm -rf ~/.m2/repository`
2. 重新下载依赖：`mvn clean dependency:resolve`

### IDE配置

**确保IDE使用JDK21**：
- IntelliJ IDEA: File -> Project Structure -> Project SDK
- Eclipse: Window -> Preferences -> Java -> Installed JREs
- VS Code: 设置java.home路径