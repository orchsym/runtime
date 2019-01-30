# Orchsym Studio

## 简介

Orchsym Studio， 即数聚蜂巢集成编排平台，基于Apache NiFi项目进行了定制开发。

## 打包

不再使用原始nifi-assembly中的打包功能。将采用单独的定制打包。

在库的根目录（即上一层目录）中执行：

```
mvn package
```

打包后的产品zip或tar.gz位于orchsym-assembly目录的target子目录下。

## HeadLess打包

所谓headless，即没有前端UI的产品，只有后台运行服务器功能。

在库根目录下执行：

```
mvn package -Dheadless
```

## 发布产品

### 版本说明
发布产品规范安装major.minor.patch规范，比如2.2.0，然后每发布一个版本则minor+1。


由于该库基于nifi，则版本保留与官方同步的版本规范，比如1.7.1-SNAPSHOT。发布产品将SNAPSHOT替换为产品版本号2.2.0,最终nifi的打包版本号将为1.7.1-2.2.0。

在打包发布版本之前，需先修改Release Notes文件`orchsym/orchsym-resources/src/main/resources/Release Notes.md`，在JIRA上由Sprint生成的“Release Notes”筛选后拷贝到该文件中，并提交修改。

### 发布流程
1.基于master创建一个分支，比如release/2.2.0:

```
git checkout -b release/2.2.0
```

2.修改所有工程pom为发布版本号，比如 `1.7.1-2.2.0`:

```
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=1.7.1-2.2.0
```

3.修改当前目录下pom.xml的 `orchsym.product.version` 属性值为 `2.2.0`

4.提交修改，打tag，并推送tag：

```
git commit -m "release version 2.2.0"
git tag -a 2.2.0 -m "release version 2.2.0"
git push origin 2.2.0
```

**注**：可先只提交，然后打包，确认没有问题后，再打tag，推送tag。

5.执行打包：

```
mvn clean install
```

如果由于JUnit失败导致，打包不成功，可添加`-DskipTests`，跳过执行单元测试，但仍旧进行编译。

6.最后回到master，将`orchsym.product.version` 属性修改为 `2.3.0-SNAPSHOT`，并提交，为下次发布做好准备。
