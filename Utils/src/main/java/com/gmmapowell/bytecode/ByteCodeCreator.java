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

public class ByteCodeCreator implements ByteCodeSink {
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

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#getCreatedName()
	 */
	@Override
	public String getCreatedName() {
		return qualifiedName;
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#superclass(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#generate()
	 */
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

	private MethodDefiner createAnyMethod(boolean isStatic, String returnType, String string) {
		MethodCreator ret = new MethodCreator(this, bcf, isStatic, returnType, string);
		bcf.addMethod(ret);
		return ret;
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#ctor()
	 */
	@Override
	public MethodDefiner ctor() {
		return createAnyMethod(false, "void", "<init>");
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#sctor()
	 */
	@Override
	public MethodDefiner sctor() {
		return createAnyMethod(true, "void", "<clinit>");
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#method(boolean, java.lang.String, java.lang.String)
	 */
	@Override
	public MethodDefiner method(boolean isStatic, String returns, String name)
	{
		if (name.contains("."))
			throw new UtilException("Cannot create method name: " + name);
		return createAnyMethod(isStatic, returns, name);
	}
	
	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#getSuperClass()
	 */
	@Override
	public String getSuperClass() {
		return superclass;
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#toString()
	 */
	@Override
	public String toString() {
		return "Creating " + qualifiedName;
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#defineField(boolean, com.gmmapowell.bytecode.JavaInfo.Access, java.lang.String, java.lang.String)
	 */
	@Override
	public void defineField(boolean isFinal, Access access, String type, String name) {
		defineField(isFinal, access, new JavaType(type), name);
	}
	
	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#defineField(boolean, com.gmmapowell.bytecode.JavaInfo.Access, com.gmmapowell.bytecode.JavaType, java.lang.String)
	 */
	@Override
	public void defineField(boolean isFinal, Access access, JavaType type, String name) {
		fields.put(name, new FieldObject(access.isStatic(), getCreatedName(), type, name));
		FieldInfo field = new FieldInfo(bcf, isFinal, access, type.getActual(), name);
		bcf.addField(field);
		GenericAnnotator.annotateField(field, type);
	}
	
	// TODO: we need others for statics & inherited members
	
	@Override
	public FieldExpr getField(NewMethodDefiner meth, String name)
	{
		if (!fields.containsKey(name))
			throw new UtilException("There is no field " + name + " in " + getCreatedName());
		return fields.get(name).use(meth);
	}
	
	public FieldExpr getInheritedField(NewMethodDefiner meth, String ofType, String name)
	{
//		if (fields.containsKey(name))
//			return getField(meth, name);
		return new FieldExpr(meth, meth.myThis(), getCreatedName(), ofType, name);
	}

	@Override
	public FieldExpr getField(NewMethodDefiner meth, Expr obj, String name)
	{
		if (!fields.containsKey(name))
			throw new UtilException("There is no field " + name + " in " + getCreatedName());
		return fields.get(name).useOn(meth, obj);
	}

	public FieldExpr getInheritedField(NewMethodDefiner meth, Expr obj, String ofType, String name)
	{
//		if (fields.containsKey(name))
//			return getField(meth, name);
		return new FieldExpr(meth, obj, obj.getType(), ofType, name);
	}


	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#makeAbstract()
	 */
	@Override
	public void makeAbstract() {
		bcf.makeAbstract();
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#makeInterface()
	 */
	@Override
	public void makeInterface() {
		bcf.makeInterface();
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#implementsInterface(java.lang.String)
	 */
	@Override
	public void implementsInterface(String intf) {
		bcf.addInterface(intf);
	}
	
	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#signatureAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	public void signatureAttribute(String name, String sig) {
		int u8 = bcf.pool.requireUtf8(sig);
		byte[] data = new byte[2];
		data[0] = (byte) ((u8>>8)&0xff);
		data[1] = (byte) (u8&0xff);
		addAttribute(name, data);
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#addAttribute(java.lang.String, byte[])
	 */
	@Override
	public void addAttribute(String name, byte[] data) {
		AttributeInfo attr = bcf.newAttribute(name, data);
		bcf.attributes.add(attr);
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#addRTVAnnotation(java.lang.String)
	 */
	@Override
	public Annotation addRTVAnnotation(String attrClass) {
		return bcf.addAnnotation(AnnotationType.RuntimeVisibleAnnotations, new Annotation(bcf, attrClass));
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#newAnnotation(java.lang.String)
	 */
	@Override
	public Annotation newAnnotation(String attrClass) {
		return new Annotation(bcf, attrClass);
	}

	/* (non-Javadoc)
	 * @see com.gmmapowell.bytecode.ByteCodeSink#addInnerClassReference(com.gmmapowell.bytecode.JavaInfo.Access, java.lang.String, java.lang.String)
	 */
	@Override
	public void addInnerClassReference(Access access, String parentClass, String inner) {
		bcf.innerClasses.add(new InnerClass(bcf, access, parentClass+"$"+inner, parentClass, inner));
	}
}
