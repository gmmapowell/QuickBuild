package com.gmmapowell.adt.avm;

import java.io.File;
import java.io.InputStream;

import com.gmmapowell.adt.ADTActivity;
import com.gmmapowell.adt.ADTIntent;
import com.gmmapowell.adt.swt.AndroidScreen;
import com.gmmapowell.bytecode.ByteCodeFile;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.system.ClassPath;
import com.gmmapowell.system.ClassPathResource;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.DateUtils.Format;
import com.gmmapowell.utils.DateUtils.Timer;

public class ADTInitialize implements Runnable {
	private Repository repository;
	private final AndroidScreen androidScreen;

	public ADTInitialize(AndroidScreen androidScreen, String[] args) {
		this.androidScreen = androidScreen;
		repository = new Repository(); 
	}

	@Override
	public void run() {
		repository.setRClass("com.example.helloWorld.R");
		Timer t = new Timer();
		int cnt = 0;
		for (ClassPathResource f : ClassPath.iterate("*.class"))
		{
			try
			{
				cnt++;
				InputStream str = f.asStream();
				ByteCodeFile bcf = new ByteCodeFile(str);
				if (bcf.implementsInterface(ADTActivity.class))
				{
					System.out.println("Found Activity " + f);
					repository.addActivity(FileUtils.convertToDottedNameDroppingExtension(new File(f.getRelativeName())));
				}
				str.close();
			}
			catch (Exception ex)
			{
				System.out.println("Error in file " + f);
				ex.printStackTrace(System.out);
			}
		}
		System.out.println("Time spent analyzing " + cnt + " classes: " + t.getElapsed(Format.hhmmss3));
		
		
		try {
			ADTSWTContext context = new ADTSWTContext(repository, androidScreen);
			@SuppressWarnings("unchecked")
			Class<? extends ADTActivity> tmp = (Class<? extends ADTActivity>) Class.forName("com.example.helloWorld.HelloAndroid");
			repository.startActivity(new ADTIntent(context, tmp));
		} catch (ClassNotFoundException e) {
			throw UtilException.wrap(e);
		}
	}

}
