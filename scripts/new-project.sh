#!/bin/sh

usage() {
  echo "Usage: new-project [--android package launchActivity] <name>" >&2
  exit 1
}

userlib="org.eclipse.jdt.launching.JRE_CONTAINER";
while [ $# -gt 0 ] ; do
  case $1 in
    --android)
      userlib="org.eclipse.jdt.USER_LIBRARY/android-4"
      android=1
      if [ $# -lt 3 ] ; then
        usage
      fi
      package=$2
      pkgPath="`echo $package | sed 's%\\.%/%'g`"
      launch=$3
      shift 2
      ;;
    --*)
      usage
      ;;
    *)
      name=$1
      ;;
  esac
  shift
done

if [ -z "$name" ] ; then
  usage
fi

if [ -e "$name" ] ; then
  echo "The project $name already exists" >&2
  exit 1
fi

mkdir -p $name/src/main/java
mkdir -p $name/src/main/resources
mkdir -p $name/src/test/java
mkdir -p $name/src/test/resources

touch $name/src/main/java/.gitsmj
touch $name/src/main/resources/.gitsmr
touch $name/src/test/java/.gitstj
touch $name/src/test/resources/.gitstr

if [ "$android" = "1" ] ; then
  mkdir -p $name/src/android/res
  touch $name/src/android/res/.gitsar
  mkdir -p $name/src/android/lib
  touch $name/src/android/lib/.gitsal
fi

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
	<classpathentry kind="con" path="$userlib"/>
	<classpathentry kind="output" path="bin/classes"/>
</classpath>
HERE

if [ "$android" = "1" ] ; then
  cat << HERE >> $name/src/android/AndroidManifest.xml
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
mkdir -p $name/src/main/java/$pkgPath
  cat << HERE >> $name/src/main/java/$pkgPath/$launch.java
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
