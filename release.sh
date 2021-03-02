#! /bin/sh
mvn clean release:clean release:prepare
git commit -m'scm: updated README' README.rst
git push origin master
git checkout HEAD~1
mvn clean generate-sources
mvn github-release:release
git checkout master
mvn clean release:clean
