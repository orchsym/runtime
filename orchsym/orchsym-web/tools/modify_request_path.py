# -*- coding: utf-8 -*-

# 修改js文件中的所有请求路径与跳转路径
def handleJsRequestPath(path):
    for x in os.listdir(path):
        file = os.path.join(path, x)
        fileName, ext = os.path.splitext(x)
        if os.path.isdir(file):
            handleJsRequestPath(file)
        elif ext == '.js':
            with open(file, 'r') as f:
                text = f.read()
                newText = re.sub("[\'\"](.*?)(/(nifi-api|nifi-docs).*?)[\'\"]", _jsRequestPathReg, text)
                newText = re.sub("[\'\"](.*?)/runtime[\'\"]", "\"index.html\"", newText)
                newText = re.sub("[\'\"](.*?)/runtime/(.+?)[\'\"]", _jsJumpPathReg, newText)
                with open(file, 'w') as w:
                    w.write(newText)
            print('# ' + file + ' 文件转换完成')

handleJsRequestPath('../js')
