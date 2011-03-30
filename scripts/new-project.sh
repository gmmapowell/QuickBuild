#!/bin/sh

if [ $# -lt 1 ] ; then
  echo "Usage: new-project <name>" >&2
  exit 1
fi

name=$1

mkdir -p $name/src/main/java
mkdir -p $name/src/main/resources
mkdir -p $name/src/test/java
mkdir -p $name/src/test/resources

touch $name/src/main/java/.gitsmj
touch $name/src/main/resources/.gitsmr
touch $name/src/test/java/.gitstj
touch $name/src/test/resources/.gitstr

cat << HERE > $name/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>$name</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
HERE

cat << HERE > $name/.classpath
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="src/main/java"/>
	<classpathentry kind="src" path="src/test/java" output="bin/testclasses"/>
	<classpathentry kind="src" path="src/main/resources"/>
	<classpathentry kind="src" path="src/test/resources" output="bin/testclasses"/>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
	<classpathentry kind="output" path="bin/classes"/>
</classpath>
HERE
