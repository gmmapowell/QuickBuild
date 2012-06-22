#!/bin/sh

usage() {
  echo "Usage: new-project [--android package launchActivity] <name>" >&2
  exit 1
}

NAME=
userlib="org.eclipse.jdt.launching.JRE_CONTAINER";
while [ $# -gt 0 ] ; do
  case $1 in
    --android)
      android=1
      version=4
      case "$2" in
        --version)
           version="$3"
           shift 2
           ;;
      esac
      userlib="org.eclipse.jdt.USER_LIBRARY/android-$version"
      if [ $# -lt 3 ] ; then
        usage
      fi
      package=$2
      pkgPath="`echo $package | sed 's%\\.%/%'g`"
      launch=$3
      srcpaths="src/android/gen"
      shift 3
      ;;
    --name)
      NAME=$2
      shift 2
      ;;
    --*)
      usage
      ;;
    *)
      folder=$1
      shift
      ;;
  esac
done

if [ -z "$folder" ] ; then
  usage
fi

if [ -e "$folder" ] ; then
  echo "The project $folder already exists" >&2
  exit 1
fi

if [ "$NAME" = "" ] ; then
  NAME=$folder
fi

mkdir -p $folder/src/main/java
mkdir -p $folder/src/main/resources
mkdir -p $folder/src/test/java
mkdir -p $folder/src/test/resources

touch $folder/src/main/java/.gitsmj
touch $folder/src/main/resources/.gitsmr
touch $folder/src/test/java/.gitstj
touch $folder/src/test/resources/.gitstr

if [ "$android" = "1" ] ; then
  mkdir -p $folder/src/android/res
  touch $folder/src/android/res/.gitsar
  mkdir -p $folder/src/android/lib
  touch $folder/src/android/lib/.gitsal
fi

cat << HERE > $folder/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>$NAME</name>
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

cat << HERE > $folder/.classpath
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="src/main/java"/>
	<classpathentry kind="src" path="src/test/java" output="bin/testclasses"/>
HERE
for p in $srcpaths
do
  echo '	<classpathentry kind="src" path="'"$p"'"/>' >> $folder/.classpath
done
cat << HERE >> $folder/.classpath
	<classpathentry kind="src" path="src/main/resources"/>
	<classpathentry kind="src" path="src/test/resources" output="bin/testclasses"/>
	<classpathentry kind="con" path="$userlib"/>
	<classpathentry kind="output" path="bin/classes"/>
</classpath>
HERE

if [ "$android" = "1" ] ; then
  cat << HERE >> $folder/src/android/AndroidManifest.xml
<?xml version="1.0" encoding="ISO-8859-1"?>
<manifest android:minSdkVersion="4" android:versionCode="1"
    android:versionName="1.0" package="$package" xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity android:label="$launch" android:name=".$launch">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
    <uses-sdk/>
</manifest>
HERE
mkdir -p $folder/src/main/java/$pkgPath
  cat << HERE >> $folder/src/main/java/$pkgPath/$launch.java
package $package;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class $launch extends Activity {
	public $launch() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("$launch", "Starting $launch ...");
	}
}
HERE
fi
