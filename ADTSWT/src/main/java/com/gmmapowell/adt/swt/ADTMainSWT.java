package com.gmmapowell.adt.swt;

import java.io.IOException;
import java.io.InputStream;

import com.gmmapowell.adt.ADTActivity;
import com.gmmapowell.bytecode.ByteCodeFile;
import com.gmmapowell.system.ClassPath;
import com.gmmapowell.system.ClassPathResource;
import com.gmmapowell.utils.DateUtils.Format;
import com.gmmapowell.utils.DateUtils.Timer;

public class ADTMainSWT {
	public static void main(String[] args)
	{
		System.out.println("Hello World");
		Timer t = new Timer();
		int cnt = 0;
		for (ClassPathResource f : ClassPath.iterate("*.class"))
		{
			try
			{
				InputStream str = f.asStream();
				ByteCodeFile bcf = new ByteCodeFile(str);
				if (bcf.implementsInterface(ADTActivity.class))
					System.out.println(f);
				str.close();
			}
			catch (Exception ex)
			{
				System.out.println("File " + f);
				ex.printStackTrace();
				break;
			}
		}
		System.out.println(t.getElapsed(Format.hhmmss3));
	}
}
