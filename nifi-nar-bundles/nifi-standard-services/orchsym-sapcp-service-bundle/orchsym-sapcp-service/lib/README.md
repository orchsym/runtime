- First, need prepare the SAP JCo `sapjco3.jar` and `sapjco3.dll` (for win) or `libsapjco3.so` (for linux) files.

- If you do dev, need put the `sapjco3.jar` here (in this `lib` folder), make sure this project to compile ok.

- If you do test or run SAP components (`InvokeSAPRFC` and `ListenSAPTCP`), need put the `sapjco3.jar` to `<runtime>/lib`.

- Also, in win, need put the `sapjco3.dll` into the `C:\Windows\System32` folder.