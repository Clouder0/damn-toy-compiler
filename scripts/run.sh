#!/usr/bin/bash

cd src
javac cn/edu/hitsz/compiler/*.java
cd ..
java -cp src cn.edu.hitsz.compiler.Main
