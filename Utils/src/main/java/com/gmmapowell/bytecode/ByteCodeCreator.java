package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.bytecode.JavaInfo.Access;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ByteCodeCreator implements ByteCodeSink {
	private ByteCodeFile bcf;
//	private String name;
//	private String pkg;
	private final File file;
	private String superclass;
	private final String qualifiedName;
	private final Map<String, FieldObject> fields = new HashMap<String, FieldObject>();
	private final ByteCodeEnvironment env;

	public ByteCodeCreator(ByteCodeEnvironment env, String qualifiedName) {
		this.qualifiedName = qualifiedName;
		bcf = new ByteCodeFile(qualifiedName);
		File tmp = FileUtils.convertDottedToPath(qualifiedName);
		this.file = new File(tmp.getParentFile(), tmp.getName() + ".class");
//		pkg = FileUtils.getPackage(file);
//		name = FileUtils.getUnextendedName(file);
		bcf.thisClass(FileUtils.convertToDottedNameDroppingExtension(file));
		this.env = env;
		env.associate(this);
	}

	@Override
	public String getCreatedName() {
		return qualifiedName;
	}

	@Override
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

	@Override
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

	MethodDefiner createMethod(boolean isStatic, String returns, String name)
	{
		if (name.contains("."))
			throw new UtilException("Cannot create method name: " + name);
		MethodCreator ret = new MethodCreator(this, bcf, isStatic, returns, name);
		bcf.addMethod(ret);
		return ret;
	}
	
	@Override
	public String getSuperClass() {
		return superclass;
	}

	@Override
	public String toString() {
		return "Creating " + qualifiedName;
	}

	@Override
	public void defineField(boolean isFinal, Access access, String type, String name) {
		defineField(isFinal, access, new JavaType(type), name);
	}
	
	@Override
	public void defineField(boolean isFinal, Access access, JavaType type, String name) {
		fields.put(name, new FieldObject(access.isStatic(), getCreatedName(), type, name));
		FieldInfo field = new FieldInfo(bcf, isFinal, access, type.getActual(), name);
		bcf.addField(field);
		GenericAnnotator.annotateField(field, type);
	}
	
	@Override
	public void inheritsField(boolean isFinal, Access access, JavaType type, String name) {
		fields.put(name, new FieldObject(access.isStatic(), getCreatedName(), type, name));
	}
	
	@Override
	public void inheritsClass(String clz) {
		ByteCodeCreator byteCodeCreator = env.get(clz);
		if (byteCodeCreator == null)
			throw new UtilException("There is no class " + clz + " to inherit from");
		for (Entry<String, FieldObject> fo : byteCodeCreator.fields.entrySet())
			fields.put(fo.getKey(), fo.getValue().rewriteFor(qualifiedName));
	}

	@Override
	public FieldExpr getField(NewMethodDefiner meth, String name)
	{
		if (!fields.containsKey(name))
			throw new UtilException("There is no field " + name + " in " + getCreatedName());
		return fields.get(name).use(meth);
	}
	
	@Override
	public FieldExpr getField(NewMethodDefiner meth, Expr obj, String name)
	{
		ByteCodeCreator creatorFor = env.get(obj.getType());
		if (creatorFor == null)
			throw new UtilException("The class " + obj.getType() + " is not registered in the system");
		if (!creatorFor.fields.containsKey(name))
			throw new UtilException("There is no field " + name + " in " + name);
		return creatorFor.fields.get(name).useOn(meth, obj);
	}

	@Override
	public void makeAbstract() {
		bcf.makeAbstract();
	}

	@Override
	public void makeInterface() {
		bcf.makeInterface();
	}

	@Override
	public void implementsInterface(String intf) {
		bcf.addInterface(intf);
	}
	
	@Override
	public void signatureAttribute(String name, String sig) {
		int u8 = bcf.pool.requireUtf8(sig);
		byte[] data = new byte[2];
		data[0] = (byte) ((u8>>8)&0xff);
		data[1] = (byte) (u8&0xff);
		addAttribute(name, data);
	}

	@Override
	public void addAttribute(String name, byte[] data) {
		AttributeInfo attr = bcf.newAttribute(name, data);
		bcf.attributes.add(attr);
	}

	@Override
	public Annotation addRTVAnnotation(String attrClass) {
		return bcf.addAnnotation(AnnotationType.RuntimeVisibleAnnotations, new Annotation(bcf, attrClass));
	}

	@Override
	public Annotation newAnnotation(String attrClass) {
		return new Annotation(bcf, attrClass);
	}

	@Override
	public void addInnerClassReference(Access access, String parentClass, String inner) {
		bcf.innerClasses.add(new InnerClass(bcf, access, parentClass+"$"+inner, parentClass, inner));
	}
}
