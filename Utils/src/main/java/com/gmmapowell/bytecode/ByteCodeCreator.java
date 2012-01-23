package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.bytecode.JavaInfo.Access;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ByteCodeCreator {
	private ByteCodeFile bcf;
//	private String name;
//	private String pkg;
	private final File file;
	private String superclass;
	private final String qualifiedName;
	private final Map<String, FieldObject> fields = new HashMap<String, FieldObject>();

	public ByteCodeCreator(String qualifiedName) {
		this.qualifiedName = qualifiedName;
		bcf = new ByteCodeFile(qualifiedName);
		File tmp = FileUtils.convertDottedToPath(qualifiedName);
		this.file = new File(tmp.getParentFile(), tmp.getName() + ".class");
//		pkg = FileUtils.getPackage(file);
//		name = FileUtils.getUnextendedName(file);
		bcf.thisClass(FileUtils.convertToDottedNameDroppingExtension(file));
	}

	public String getCreatedName() {
		return qualifiedName;
	}

	public void superclass(String string) {
		this.superclass = string;
		bcf.superClass(string);
	}

	public void addToJar(JarOutputStream jos) {
		try {
			JarEntry je = new JarEntry(file.getPath().replaceAll("\\\\", "/"));
			jos.putNextEntry(je);
			generateByteCodes(jos);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	public byte[] generate() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			generateByteCodes(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}
	
	private void generateByteCodes(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		bcf.write(dos);
		dos.flush();
	}

	private MethodCreator createAnyMethod(boolean isStatic, String returnType, String string) {
		MethodCreator ret = new MethodCreator(this, bcf, isStatic, returnType, string);
		bcf.addMethod(ret);
		return ret;
	}

	public MethodCreator ctor() {
		return createAnyMethod(false, "void", "<init>");
	}

	public MethodCreator sctor() {
		return createAnyMethod(true, "void", "<clinit>");
	}

	public MethodCreator method(boolean isStatic, String returns, String name)
	{
		if (name.contains("."))
			throw new UtilException("Cannot create method name: " + name);
		return createAnyMethod(isStatic, returns, name);
	}
	
	public String getSuperClass() {
		return superclass;
	}

	@Override
	public String toString() {
		return "Creating " + qualifiedName;
	}

	public FieldInfo field(boolean isFinal, Access access, String type, String var) {
		FieldInfo field = new FieldInfo(bcf, isFinal, access, type, var);
		bcf.addField(field);
		return field;
	}

	public void makeAbstract() {
		bcf.makeAbstract();
	}

	public void makeInterface() {
		bcf.makeInterface();
	}

	public void implementsInterface(String intf) {
		bcf.addInterface(intf);
	}
	
	public void signatureAttribute(String name, String sig) {
		int u8 = bcf.pool.requireUtf8(sig);
		byte[] data = new byte[2];
		data[0] = (byte) ((u8>>8)&0xff);
		data[1] = (byte) (u8&0xff);
		addAttribute(name, data);
	}

	public void addAttribute(String name, byte[] data) {
		AttributeInfo attr = bcf.newAttribute(name, data);
		bcf.attributes.add(attr);
	}

	public Annotation addRTVAnnotation(String attrClass) {
		return bcf.addClassAnnotation(AnnotationType.RuntimeVisibleAnnotations, new Annotation(bcf, attrClass));
	}

	public Annotation newAnnotation(String attrClass) {
		return new Annotation(bcf, attrClass);
	}

	public void addInnerClassReference(Access access, String parentClass, String inner) {
		bcf.innerClasses.add(new InnerClass(bcf, access, parentClass+"$"+inner, parentClass, inner));
	}

	// This is to help MethodCreator out
	public void defineField(String type, String name) {
		fields.put(name, new FieldObject(getCreatedName(), type, name));
	}
	
	// TODO: we need others for statics & inherited members
	
	public FieldExpr getField(MethodCreator meth, String name)
	{
		if (!fields.containsKey(name))
			throw new UtilException("There is no field " + name + " in " + getCreatedName());
		return fields.get(name).use(meth);
	}
}
