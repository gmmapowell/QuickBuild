package com.gmmapowell.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.exceptions.UtilException;

public class RuntimeVisibleAnnotations {

	/*
	        I added a space to all these so they don't show up when I'm searching
			@ Test(expected=Exception.class)
			@ Ignore
			@ Deprecated
			@ SuppressWarnings("unused")   // source only

		00000130 73 74 3B                                        st;               20 => U8: Lorg/junit/Test;
		00000133 01 00 08 65 78 70 65 63 74 65 64                ...expected       21 => U8: expected
		0000013E 01 00 15 4C 6A 61 76 61 2F 6C 61 6E 67 2F 45 78 ...Ljava/lang/Ex
		0000014E 63 65 70 74 69 6F 6E 3B                         ception;          22 => U8: Ljava/lang/Exception;
		00000156 01 00 12 4C 6F 72 67 2F 6A 75 6E 69 74 2F 49 67 ...Lorg/junit/Ig
		00000166 6E 6F 72 65 3B                                  nore;             23 => U8: Lorg/junit/Ignore;
		0000016B 01 00 16 4C 6A 61 76 61 2F 6C 61 6E 67 2F 44 65 ...Ljava/lang/De
		0000017B 70 72 65 63 61 74 65 64 3B                      precated;         24 => U8: Ljava/lang/Deprecated;

		00000206 00 13 00 00 00 13                               ......            idx = 19: U8: RuntimeVisibleAnnotations len=19
		0000020C 00 03 00 14 00 01 00 15 63 00 16 00 17 00 00 00 ........c.......
		0000021C 18 00 00                                        ...               

		00 03 : there are three of them
		  00 14 => 20 => Lorg/junit/Test
		    00 01 : it has one argument
		      00 15 => 21 => the argument is "expected"
		      63 - '@' - it's of type "class"
		      00 16 => 22 => Ljava/lang/Exception
		  00 17 => 23 => Ljava/lang/Ignore
		    00 00 : no arguments
		  00 18 => 24 => Ljava/lang/Deprecated
		    00 00 : no arguments
	 */

	private final List<Annotation> annotations = new ArrayList<Annotation>();
	
	public RuntimeVisibleAnnotations(ByteCodeFile bcf, AttributeInfo ai) {
		try
		{
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ai.getBytes()));
			int cnt = dis.readUnsignedShort();
			for (int i=0;i<cnt;i++)
			{
				int nameIdx = dis.readUnsignedShort();
				Annotation annotation = new Annotation(bcf, ((Utf8Info)bcf.pool.get(nameIdx)).asString());
				annotations.add(annotation);
				int nargs = dis.readUnsignedShort();
				for (int j=0;j<nargs;j++)
				{
					dis.readUnsignedShort();
					dis.readByte();
					dis.readUnsignedShort();
				}
			}
		} catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public boolean has(String mapped) {
		for (Annotation a : annotations)
			if (a.name.equals(mapped))
				return true;
		return false;
	}

}
