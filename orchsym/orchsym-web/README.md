## 启动服务
在当前目录下搭建静态文件服务器即可

例如可用http-server服务器(需提前安装http-server)
```
http-server -a 127.0.0.1 -p 1111
```
然后浏览器输入地址访问 127.0.0.1:1111 即可


## 更新版本号
需要先安装Python3环境,然后在当前目录下执行以下命令
```
python3 modify_version <version>
```

例如
```
python3 modify_version 1.7.0-SNAPSHOT
```

## 从jsp转换为HTML的整个实现过程如下


#### Python处理jsp，处理成gulp可以直接合并打包的格式(init.py)

分析jsp代码可以发现
- 在 WEB-INF/pages/ 文件夹下的jsp文件对应于一个个的页面，在这些页面中引入了大量的js,css,还引入了许多 WEB-INF/partials/ 文件夹下的jsp页面。
- 再观察 WEB-INF/partials/ 文件夹下的jsp文件，发现这些jsp文件里面都是html代码，相关的js,css文件均是在 WEB-INF/pages/ 下的jsp文件里面引入的。

Python处理
- 去除所有jsp文件的头部jsp声明信息
```
<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
```

- 处理jsp中js,css引入路径
之前是由后端来控制引入的路径，现在分离之后目录结构那些都不一样，所以引入的路径都需要修改

- 修改jsp中引用其他jsp的路径以及后缀，我们会将所有的jsp文件都修改为html，所有对应的引入也得进行修改

修改前
```
  <jsp:include page="/WEB-INF/partials/canvas/navigation.jsp"/>
  <jsp:include page="/WEB-INF/partials/canvas/component-panel.jsp"/>
  <jsp:include page="/WEB-INF/partials/canvas/settings-content.jsp"/>
```
修改后
```
  <jsp:include page="../../WEB-INF/partials/canvas/navigation.html"/>
  <jsp:include page="../../WEB-INF/partials/canvas/component-panel.html"/>
  <jsp:include page="../../WEB-INF/partials/canvas/settings-content.html"/>
```

- 将以上修改的所有内容存储为html文件

- 将所有的html所依赖的js,css,assets,images,views,fonts文件夹移copy一份到build文件夹下,build文件夹也就是我们最后分离出来的前端代码。


#### gulp进行合并打包(gulpfile.js)
通过以上Python脚本的处理，现在就可以用gulp进行愉快的打包了。gulp将遍历 WEB-INF/pages/ 下的html文件，并根据我们设置的正则规则匹配 /<jsp:include\s+page="([^"]+)"\s*\/>/g 匹配文件中引入的其它html页面，将其合并到当前文件来生成一个个完整的html页面，最后再将这些html文件拷贝到 build/文件夹下，至此，一个完成的 build文件夹就出来了，这就是我们分离出来的前端代码，也就是你现在看到的orchsym-web文件夹


#### 修改js文件中的所有请求路径与跳转路径(modify_request_path.py)
由于现在前后端分离了，之前项目里面采用的相对路径的请求路径与跳转路径就有问题了
- 处理请求路径，通过分析发现，所有的请求路径都带有形如 **"/nifi-api"**, **"/nifi-docs"** 的字符.然后定义一个全局的 apiHost 表示后端地址，再遍历所有js文件，
将所有的 请求路径替换为形如 **apiHost + "/nifi-api/flow/cluster/summary"** 的字符。与此同时，后端需要处理跨域问题

- 处理跳转路径，先手动添加入口文件index.html,访问该页面让其自动跳转到canvas.html就成。直接之久匹配 **/runtime** ，之前的/runtime/canvas 替换成对应的 canvas.html。
/runtime替换成 index.html


#### 前端增加版本控制(modify_version.py)
在html文件中，会有许多的js,css引入后缀加上了形如 **?1.7.0-SNAPSHOT** 的版本号后缀,现在就是写死在文件里面了，现在就新增了脚本对这些后缀版本号进行动态控制
```
python3 modify_version 1.8.0-SNAPSHOT
```


#### html中涉及的变量渲染全部修改成前端统一控制(modify_html_message.py)
在html文件中会有许多形如 **<fmt:message key="partials.message-pane.logout">** 动态变量渲染，这些渲染之前是后端通过两个配置文件进行动态渲染进去的，现在分离之后就不能有效的进行渲染了。接下来就要将由后端控制的变量渲染改为前端控制渲染

- 将后端配置文件提取到前端变量配置 **js/nf/globalization/resources.js** 里面采用的方式就是逐行读取后端的变量配置文件，然后在前端的变量配置需要插入数据的地方加入替换标记，然后将读取出来的配置数据一行一行的与该插入标记进行替换，替换一行的同时在改替换出的下一行再写入替换标记，方便下一行数据替换。

下面的//en-message-end即是替换标记
```
  "partials.canvas.save-flow-version-dialog.version-comment": "Version Comments",
  //en-message-end
};
```

- 替换html里面的 <fmt:message... 为前端渲染。
这里的 <fmt:message 分为两种模式
第一种形如
```
<fmt:message key="partials.message-pane.logout">
```
这种容易处理，直接匹配将 <fmt:message key="partials.message-pane.logout"> 改为
```
{{ appCtrl.serviceProvider.globalMenuCtrl.constant["partials.message-pane.logout"] }}
```

还有一种形如
```
<fmt:message key="partials.cancas.upload-template-dialog.template-file-field" var="file-field"></fmt:message>
<button class="fa fa-search" id="template-file-field-button" title="${file-field}"></button>
```
它是通过 <fmt:message...来定义了一个 file-field 的变量，然后下面再采用 ${file-field} 的方式来渲染变量
这个需要两步来处理
- 拿到一个html文件，匹配所有的 第一行格式那样的数据 把其中的 var="file-field",key="partials.cancas.upload-template-dialog.template-file-field"
拿出来，作为 key,value存入一个词典中，然后再将这行数据替换成空字符。这样一个html文件下来就得到了一个由很多 key-value组成的词典。
- 然后在用我们上一步生成的字典的信息去替换 形如 ${file-field} 的变量。

然后就生成了以下的格式
```
<button class="fa fa-search" id="template-file-field-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant["partials.cancas.upload-template-dialog.template-file-field"] }}"></button>
```


#### 解决 iframe 无法载入后端页面
虽然进行了前后端分离，但是项目里面还是有些页面是通过后端进行iframe动态载入的，比如说一些组件的高级配置页面，虽然已经进行了跨域处理，但是在iframe进行动态载入的时候还是会被拒绝
解决方案是后端注释掉 **response.setHeader(FRAME_OPTIONS, SAME_ORIGIN);** 的iframe引入限制。但是为了安全着想，后期等项目正式上线后可能需要再加上，将SAME_ORIGIN设置
为前端部署的地址。
