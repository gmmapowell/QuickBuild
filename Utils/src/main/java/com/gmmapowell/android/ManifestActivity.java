package com.gmmapowell.android;

import com.gmmapowell.xml.XMLElement;
import com.gmmapowell.xml.XMLNamespace;

public class ManifestActivity {

	private final XMLElement activity;
	private final XMLNamespace android;

	public ManifestActivity(XMLNamespace android, XMLElement appl, String id, String name, String label) {
		this.android = android;
		activity = appl.addElement("activity");
		activity.setAttribute(android.attr("name"), "." + id);
		activity.setAttribute(android.attr("label"), label);
	}

	public void launch()
	{
		XMLElement intent = activity.addElement("intent-filter");
		intent.addElement("action").setAttribute(android.attr("name"), "android.intent.action.MAIN");
		intent.addElement("category").setAttribute(android.attr("name"), "android.intent.category.LAUNCHER");
	}
	/*    <application android:icon="@drawable/icon" android:label="@string/app_name" android:debuggable="true">
        
        <activity android:name=".HelloAndroid"
                  android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
*/
}
