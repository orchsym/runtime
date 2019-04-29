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

### 问题

1. 如果打包时有JUnit单元测试不通过，导致打包失败，可添加参数跳过测试，但仍旧编译单元测试类`-DskipTests`。
2. 如果是在Windows下，可能由于编码问题，导致前端js，css压缩出现问题，可添加编码参数`-Dfile.encoding=UTF-8`。

## Headless打包

所谓headless，即没有前端UI的产品，只有后台运行服务器功能。

在库根目录下执行：

```
mvn package -Dheadless
```

## 发布产品

### 版本说明
发布产品规范安装major.minor_buildnumber规范，比如`2.0_05000`，然后每发布一个版本通过buildnumber的变更来表示。


由于该库基于nifi，则版本保留与官方同步的版本规范，比如`1.7.1-SNAPSHOT`。发布产品将SNAPSHOT替换为产品版本号`2.0_05000`,最终nifi库的打包版本号将为`1.7.1-2.0_05000`。

在打包发布版本之前，需先修改Release Notes文件`orchsym/orchsym-resources/src/main/resources/Release Notes.md`，在JIRA上由Sprint生成的“Release Notes”筛选后拷贝到该文件中，并提交修改。

### 发布流程
1.基于master创建一个发布分支，比如`release/2.0_05000`:

```
git checkout -b release/2.0_05000
```

2.修改所有工程pom为发布版本号，比如 `1.7.1-2.0_05000`:

```
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=1.7.1-2.0_05000
```

3.修改当前目录下pom.xml的 `orchsym.product.version` 属性值为 `2.0_05000`; 以及修改根文件`Dockerfile`中`VERSION_NAME="1.7.1-SNAPSHOT"`为`VERSION_NAME="1.7.1-2.0_05000"`。

4.提交修改，打tag，并推送发布分支：

```
git add .
git commit -m "修改版本为2.0_05000"
git push origin release/2.0_05000
```

**注**：可先只提交，然后尝试打包，确认没有问题后，最后再统一推送。

5.执行打包：

```
mvn clean install
```

6.如果打包验证没有问题后，在Bitbucket的远程库`release/2.0_05000`分支的最后一次提交上打tag：`2.0_05000`，并删除本地以及远程发布分支`release/2.0_05000`。

7.最后回到master，将`orchsym.product.version` 属性修改为 `2.0_06000-SNAPSHOT`，并提交及推送，为下次发布做好准备。
