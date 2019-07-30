# Orchsym Studio

## 简介

Orchsym Studio， 即数聚蜂巢集成编排平台，基于Apache NiFi项目进行了定制开发。

该平台是易用使用、强大且易于扩展的数据处理和分发平台。


## 要求

- JDK 1.8 (暂可能不支持高版本，比如JDK 12)
- Apache Maven 3.5+


## 打包

不再使用原始nifi-assembly中的打包功能。将采用单独的定制打包。

在当前库的根目录中执行：

```
mvn package
```

打包后的zip或tar.gz产品包位于orchsym-assembly目录的target子目录下。


## 运行

将zip 或 tar.gz 解压到指定目录，比如 `/opt/orchsym`。

进入解压产品的bin目录执行：

```
./orchsym.sh start
```

或 Win下：

```
run-orchsym.bat
```

### 查看状态

```
./orchsym.sh status
```

或 Win下：

```
status-orchsym.bat
```

### 访问

第一次启动可能稍慢，需要解压部分组件包，耐心等待片刻后。

可通过该URL访问： 

 http://localhost:8080/runtime

由于没有修改任何配置，默认端口为`8080`， 如果有冲突， 可修改conf目录下 `orchsym.properties` 文件:

```
orchsym.web.http.port=8080
```


### 问题

1. 如果打包时有JUnit单元测试不通过，导致打包失败，可添加参数跳过测试，但仍旧编译单元测试类`-DskipTests`。
2. 如果是在Windows下，可能由于编码问题，导致前端js，css压缩出现问题，可添加编码参数`-Dfile.encoding=UTF-8`。
3. 同样是在Windows下，如果使用的是PowerShell来打包，会出现“.encoding=UTF-8”不能识别错误，则需要加引号，比如加双引号：` -D"file.encoding=UTF-8"`。



## License

Except as otherwise noted this software is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


