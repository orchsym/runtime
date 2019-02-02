# -*- coding: utf-8 -*-

import os
import re
import sys

def _regRepalce(matched):
    global version
    attribute, path = matched.groups()[:2]
    return attribute + "=" + "\"" + path + "?" + version +"\""

def modify(path):
    global version
    for x in os.listdir(path):
        file = os.path.join(path, x)
        fileName, ext = os.path.splitext(x)
        if os.path.isfile(file) and ext == '.html':
            with open(file, 'r') as f:
                text = f.read()
                newText = re.sub("(href|src)=[\'\"](.+?\.(css|js))(.*?)[\'\"]", _regRepalce, text)
                with open(file, 'w') as w:
                    w.write(newText)
            print('# ' + file + ' File version modification completed')
    print("\033[0;32;40mThe front end version was successfully changed to " + version + "\033[0m")

try:
    version = sys.argv[1]
except BaseException as e:
    version = ''

modify(os.path.abspath('../'))
