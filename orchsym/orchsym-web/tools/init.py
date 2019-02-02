# -*- coding: utf-8 -*-

import os
import re
from html.parser import HTMLParser
from bs4 import BeautifulSoup, Comment
import shutil

class JspText(object):
    def __init__(self, fileName, text):
        self.fileName = fileName
        self.text = text
    # 处理jsp页面头部的jsp标注信息
    def handlerJspHeader(self):
        self.text = re.sub(r"<%.*%>", "", self.text, flags=re.S)
        return self

    # 处理页面js的引入路径
    def linkJsp2Html(self):
        self.text = re.sub(r"\.jsp", ".html", self.text, flags=re.S)
        self.text=re.sub(r'page="/', 'page="../../', self.text, flags=re.S)
        # self.text = re.sub(r'href="', 'href="../../', self.text, flags=re.S)
        # self.text = re.sub(r'src="', 'href="../../', self.text, flags=re.S)
        return self

    # 在css引用前后添加打包后的路径
    def cssHandle(self):
        bsObj = BeautifulSoup(self.text, "html.parser")
        firCss = bsObj.find("link", {"rel": "stylesheet"})
        if firCss:
            lastCss = bsObj.findAll("link", {"rel": "stylesheet"})[-1]
            firCss.insert_before(bsObj.new_string('build:css static/css/'+self.fileName+'.css', Comment))
            lastCss.insert_after(bsObj.new_string('endbuild', Comment))
        self.text = str(bsObj.prettify())
        return self

    # 在js引用前后添加打包后的路径
    def jsHandle(self):
        bsObj = BeautifulSoup(self.text, "html.parser")
        firJs = bsObj.find("script")
        if firJs:
            lastJs = bsObj.findAll("script")[-1]
            firJs.insert_before(bsObj.new_string('build:js static/js/'+self.fileName+'.js', Comment))
            lastJs.insert_after(bsObj.new_string('endbuild', Comment))
        self.text = str(bsObj.prettify())
        return self

    def _jspIncludeRepl(self, matched):
        page = matched.groups()[0]
        print(page)
        return "<jsp:include "+page+"/>"

    def restoreJspInclude(self):
        self.text = re.sub("<jsp:include (.*?)>(.*?)</jsp:include>", self._jspIncludeRepl, self.text, flags=re.S)
        return self


# 将所有jsp文件进行标准化，处理成gulp可以直接进行合并打包的格式
def modify(path):
    for x in os.listdir(path):
        file = os.path.join(path, x)
        fileName, ext = os.path.splitext(x)
        if os.path.isdir(file):
            modify(file)
        elif ext == '.jsp':
            with open(file, 'r') as f:
                text = f.read()
                jspObj = JspText(fileName, text)
                newText = jspObj.handlerJspHeader().linkJsp2Html().restoreJspInclude().text
                with open(os.path.join(path, fileName+'.html'), 'w') as w:
                    w.write(newText)
            # re.remove(file)
            print('# '+file+' 文件转换完成')

def _jsRequestPathReg(matched):
    apiPath = matched.groups()[1]
    return 'apiHost' + ' + \"' + apiPath + '\"'

def _jsJumpPathReg(matched):
    jumpPath = matched.groups()[1]
    return '\"'+jumpPath + '.html' + '\"'


def initBuildDir(path, pathList):
    shutil.rmtree(path)
    os.mkdir(path)
    for old in pathList:
        shutil.copytree(old, os.path.join(path, os.path.split(old)[1]))



if __name__ == '__main__':
    modify(os.path.abspath('WEB-INF'))
    pathList = [os.path.abspath(path) for path in ['assets','css','fonts','images','js', 'views'] ]
    initBuildDir(os.path.abspath('build'), pathList)
    print('文件预处理完成')
