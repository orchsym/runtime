# Orchsym Studio

Put simply Studio was built to automate the flow of data between systems.  While
the term 'dataflow' is used in a variety of contexts, we use it here
to mean the automated and managed flow of information between systems.  

This runtime is the server part of **Orchsym Studio**.

## Topic

- [Getting Started](#getting-started)
- [Getting Help](#getting-help)
- [Requirements](#requirements)
- [License](#license)

## Getting Started

### Start
Before start *runtime*, need put the license file **lic** (without extension) in **conf** folder first.

Then, to start *runtime*, locate to **bin** folder, and run:
- [linux/osx] execute `./orchsym.sh start`
- [windows] execute `run-orchsym.bat start`

Finally, direct your browser to `http://localhost:8080/runtime/`. (by default the port is 8080, if https, maybe it's 9443 or others)

### Commands
The commands of *runtime*:
- `start`, try to lunch *runtime*.
- `stop`, stop the *runtime*.
- `restart`, stop the *runtime* first, then start it again.
- `status`, check status of the *runtime*.

## Getting Help
If you have questions, you can reach out to our mailing list: orchsym-support@baishancloud.com


## Requirements
* JDK 1.8 or higher

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
