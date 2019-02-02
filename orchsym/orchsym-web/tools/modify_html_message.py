# -*- coding: utf-8 -*-

import os
import re

def moveEnMessage(file1, file2, language):
    with open(file2, 'r') as r2:
        newText = r2.read()
    with open(file1, 'r') as r:
        textList = r.readlines()
        for text in textList:
            text = text.strip('\n')
            if re.match('^(\s*#.*|\s*)$', text):
                continue
            else:
                key, value = text.split('=', 1)
                value = value.encode("utf-8")
                value = value.decode("unicode_escape")
                print(key+': ' +value)
                tagStr = '//'+language+'-message-end'
                newText = re.sub(tagStr, '\"'+key+'\": \"'+value+'\",\n        '+tagStr, newText)
        with open(file2, 'w') as w2:
            w2.write(newText)

def _regDefineVariable(matched):
    global constant
    key = matched.groups()[3]
    value = matched.groups()[1]
    constant[key] = value
    return ''

def _regVariable(matched):
    global constant
    key = matched.groups()[0]
    return '{{ appCtrl.serviceProvider.globalMenuCtrl.constant[\"'+constant.get(key, 'empty')+'\"] }}'

def _regRepalce(matched):
    key = matched.groups()[1]
    return '{{ appCtrl.serviceProvider.globalMenuCtrl.constant[\"'+key+'\"] }}'

def modifyHtmlQuote(path):
    global constant
    for x in os.listdir(path):
        file = os.path.join(path, x)
        fileName, ext = os.path.splitext(x)
        if os.path.isfile(file) and ext == '.html':
            with open(file, 'r') as f:
                constant = dict()
                text = f.read()
                newText = re.sub('<fmt:message(\s+?)key=[\"\'](.+?)[\"\'](\s+?)var=[\'\"](.+?)[\'\"](\s*?)\/>', _regDefineVariable, text)
                newText = re.sub('\${(.+?)}', _regVariable, newText)
                newText = re.sub('<fmt:message(\s+?)key=[\"\'](.+?)[\"\'](\s*?)\/>', _regRepalce, newText)
                with open(file, 'w') as w:
                    w.write(newText)
                print('# ' + file + ' File version modification completed')



constant = dict()
print('该脚本禁止随意调用,当html文件中的变量引用出错时,可参考该脚本')
# moveEnMessage('./Messages_en.properties', './js/nf/globalization/resources.js', 'en')
# moveEnMessage('./Messages_cn.properties', './js/nf/globalization/resources.js', 'zh')
# modifyHtmlQuote(os.path.abspath('.'))
# print("\033[0;32;40mData migration succeeded\033[0m")
