# License头添加说明

## Java class类
针对 Java源代码的文件，即".java"后缀名的文件。

添加到第一行，即package ... 代码之前，采用是的Java的多行注释 `"/*   ... */"`:

```
/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## XML文件
- XML文件，主要针对Maven的`pom.xml`文件和XML配置文件等，所有 `".xml"` 结尾的文件。

采用注释`“<!-- -->”`，一般在顶部添加：

```
<!--
    Licensed to the Orchsym Runtime under one or more contributor license
    agreements. See the NOTICE file distributed with this work for additional
    information regarding copyright ownership.

    this file to You under the Orchsym License, Version 1.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

```

## 配置文件类
- conf文件 （比如 bootstrap.conf）
- properties文件， (比如 orchsym.properties)，包括国际化文件，位于`src/main/resources/i18n/`下。
- Java服务配置，比如组件、控制器等，诸如位于工程 `src/main/resources/META-INF/services/`下。
- Yaml配置，即以`".yml"`结尾的文件
- Dockerfile配置

均采用的注释方式 `“# ...”`:

```
#
#   Licensed to the Orchsym Runtime under one or more contributor license
#   agreements. See the NOTICE file distributed with this work for additional
#   information regarding copyright ownership.
#
#   this file to You under the Orchsym License, Version 1.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#   https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
```


## 启动脚本类

- Shell脚本文件 (比如 orchsym.sh) ，采用`"# ..."`的注释方式，同 **配置文件类**。
- Bat文件 （比如，run-orchsym.bat）, 采用bat的注释方式 `“rem ...”`:

```
rem
rem   Licensed to the Orchsym Runtime under one or more contributor license
rem   agreements. See the NOTICE file distributed with this work for additional
rem   information regarding copyright ownership.
rem
rem   this file to You under the Orchsym License, Version 1.0 (the "License");
rem   you may not use this file except in compliance with the License.
rem   You may obtain a copy of the License at
rem
rem   https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
rem
rem   Unless required by applicable law or agreed to in writing, software
rem   distributed under the License is distributed on an "AS IS" BASIS,
rem   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem   See the License for the specific language governing permissions and
rem   limitations under the License.
rem
```


## 网页类
- JS脚本文件，采用JS多行注释语法`“/* ... */”`放文件头部，同 **Java class类** 。
- css文件，采用多行注释语法`“/* ... */”`放文件头部，同 **Java class类** 。
- HTML文件，采用HTML注释语法`“<!-- ... -->”`放 `”<html ...>“`标签之后，同 **XML文件** 。
- JSP文件，采用JSP注释语法`"<%-- --%>"`, 放文件头部：

```
<%--
    Licensed to the Orchsym Runtime under one or more contributor license
    agreements. See the NOTICE file distributed with this work for additional
    information regarding copyright ownership.

    this file to You under the Orchsym License, Version 1.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
```

## Jenkinsfile

采用多行注释语法`“/* ... */”`放文件头部，同 **Java class类** 。

