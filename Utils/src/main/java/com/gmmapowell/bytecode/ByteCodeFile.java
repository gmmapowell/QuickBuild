package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.gmmapowell.bytecode.CPInfo.ClassInfo;
import com.gmmapowell.bytecode.CPInfo.DoubleEntry;
import com.gmmapowell.bytecode.CPInfo.DoubleInfo;
import com.gmmapowell.bytecode.CPInfo.FloatInfo;
import com.gmmapowell.bytecode.CPInfo.IntegerInfo;
import com.gmmapowell.bytecode.CPInfo.LongInfo;
import com.gmmapowell.bytecode.CPInfo.NTInfo;
import com.gmmapowell.bytecode.CPInfo.RefInfo;
import com.gmmapowell.bytecode.CPInfo.StringInfo;
import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ByteCodeFile implements AnnotationHolder {
	public final static int   javaMagic        = 0xCAFEBABE;
	public final static short javaMajorVersion = 50; // Java 1.6?
	public final static short javaMinorVersion = 0;
	public final static short ACC_PUBLIC       = 0x0001;
	public final static short ACC_PRIVATE      = 0x0002;
	public final static short ACC_PROTECTED    = 0x0004;
	public final static short ACC_STATIC       = 0x0008;
	public final static short ACC_FINAL        = 0x0010;
	public final static short ACC_SUPER        = 0x0020;
	public final static short ACC_SYNCHRONIZED = 0x0020;
	public final static short ACC_TRANSIENT    = 0x0080;
	public final static short ACC_NATIVE       = 0x0100;
	public final static short ACC_INTERFACE    = 0x0200;
	public final static short ACC_ABSTRACT     = 0x0400;
	public final static short ACC_STRICT       = 0x0800;
	public final static short ACC_ACCESSMETH   = 0x1000;
	public final static short ACC_ENUM         = 0x4000;

	public final static byte CONSTANT_UTF8         = 1;
	public final static byte CONSTANT_Integer      = 3;
	public final static byte CONSTANT_Float        = 4;
	public final static byte CONSTANT_Long         = 5;
	public final static byte CONSTANT_Double       = 6;
	public final static byte CONSTANT_Class        = 7;
	public final static byte CONSTANT_String       = 8;
	public final static byte CONSTANT_Fieldref     = 9;
	public final static byte CONSTANT_Methodref    = 10;
	public final static byte CONSTANT_Interfaceref = 11;
	public final static byte CONSTANT_NameAndType  = 12;
	
	protected ConstPool pool;
	protected List<Integer> interfaces = new ArrayList<Integer>();
	private int access_flags = -1;
	private int this_idx = -1;
	private int super_idx = -1;
	private List<FieldInfo> fields = new ArrayList<FieldInfo>();
	private List<MethodInfo> methods = new ArrayList<MethodInfo>();
	List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
	private final String qualifiedName;
	private ListMap<AnnotationType, Annotation> annotations = new ListMap<AnnotationType, Annotation>();
	final TreeSet<InnerClass> innerClasses = new TreeSet<InnerClass>();
	private boolean completed = false;


	protected ByteCodeFile()
	{
		this((String)null);
	}
	
	protected ByteCodeFile(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
		pool = new ConstPool();
	}

	public ByteCodeFile(File from, String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(from);
			read(fis);
			fis.close();
		}
		catch (IOException ex)
		{
			if (fis != null)
				try { fis.close(); } catch (IOException e2) { }
			throw UtilException.wrap(ex);
		}
	}
	
	public ByteCodeFile(InputStream fis)
	{
		read(fis);
		qualifiedName = null;
	}

	private void read(InputStream fis) {
		try
		{
			DataInputStream dis = new DataInputStream(fis);
			int magic = dis.readInt();
			if (magic != javaMagic)
				throw new UtilException("This is not a bytecode file");
			dis.readUnsignedShort(); //minor
			dis.readUnsignedShort(); // major
			readConstantPool(dis);
			access_flags = dis.readUnsignedShort(); // access_flags
			this_idx = dis.readUnsignedShort(); // this_class
			super_idx = dis.readUnsignedShort(); // super_class
			readInterfaces(dis);
			readFields(dis);
			readMethods(dis);
			readAttributes(dis, attributes, this);
			if (dis.available() != 0)
				throw new UtilException("There are still " + dis.available() + " bytes available on the stream");
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public void makeInterface()
	{
		access_flags = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
	}

	public void makeAbstract() {
		access_flags = ACC_PUBLIC | ACC_ABSTRACT;
	}

	public String getName() {
		return ((ClassInfo)pool.get(this_idx)).justName();
	}

	public RefInfo getRefInfoIfValidIdx(int idx) {
		if (idx < 1 || idx >= pool.size())
			return null;
		CPInfo ret = pool.get(idx);
		if (ret instanceof RefInfo)
			return (RefInfo) ret;
		return null;
	}
	
	public String getString(int idx) {
		if (idx < 1 || idx >= pool.size())
			return null;
		CPInfo ret = pool.get(idx);
		if (ret instanceof StringInfo)
			return ((StringInfo)ret).asString();
		return null;
	}

	public boolean isConcrete() {
		return (access_flags & (ACC_ABSTRACT | ACC_INTERFACE)) == 0;
	}

	public void write(DataOutputStream dos) throws IOException {
		if (!completed)
		{
			if (access_flags == -1)
				access_flags = ACC_SUPER | ACC_PUBLIC;
			if (this_idx == -1)
				throw new UtilException("You must specify a this class");
			if (super_idx == -1)
				super_idx = pool.requireClass("java/lang/Object");
	
			for (FieldInfo fi : fields)
				fi.complete();
			for (MethodInfo mi : methods)
				((MethodDefiner)mi).complete();
			complete();
		}
		
		dos.writeInt(javaMagic);
		dos.writeShort(0);
		dos.writeShort(50);
		pool.writeConstantPool(dos);
		dos.writeShort(access_flags);
		dos.writeShort(this_idx);
		dos.writeShort(super_idx);
		writeInterfaces(dos);
		writeFields(dos);
		writeMethods(dos);
		writeAttributes(dos, attributes);
	}

	private void complete() throws IOException {
		for (int i=0;i<methods.size();i++)
		{
			MethodInfo first = methods.get(i);
			if ((first.access_flags &ACC_ABSTRACT) != 0)
				this.access_flags |= ACC_ABSTRACT;
			for (int j=i+1;j<methods.size();j++)
			{
				MethodInfo second = methods.get(j);
				if (first.getName().equals(second.getName()) && first.getSignature().equals(second.getSignature()))
					throw new UtilException("Duplicate method: " + first.getName() + first.getSignature() + " in class " + getName());
			}
		}

		if (!innerClasses.isEmpty())
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(innerClasses.size());
			for (InnerClass i : innerClasses)
				i.write(dos);
			attributes.add(new AttributeInfo(this, "InnerClasses", baos.toByteArray()));
		}
		for (AnnotationType at : annotations)
		{
			at.addTo(this, attributes, annotations.get(at), -1);
		}
		
		this.completed  = true;
	}

	private void readConstantPool(DataInputStream dis) throws IOException {
		int poolCount = dis.readUnsignedShort();
		pool = new ConstPool(poolCount);

		// This is weird offsetting ...
		for (int idx=1;idx<poolCount;idx++)
		{
			pool.setPoolEntry(idx, readPoolEntry(dis));
			if (pool.get(idx) instanceof DoubleEntry)
				idx++; // skip the second entry
//				System.out.println(idx + " = " + pool.get(idx));
		}
	}

	private void readInterfaces(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
			interfaces.add(dis.readUnsignedShort()); // the pool id of the interface
	}

	private void writeInterfaces(DataOutputStream dos) throws IOException {
		dos.writeShort(interfaces.size());
		for (int ci : interfaces)
			dos.writeShort(ci);
	}

	private void readFields(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of fields = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int access = dis.readUnsignedShort(); // access_flags
			int name = dis.readUnsignedShort(); // name idx
			int descriptor = dis.readUnsignedShort(); // descriptor idx
			FieldInfo fi = new FieldInfo(this, access, name, descriptor);
			readAttributes(dis, fi.attributes, fi);
			fields.add(fi);
		}
		
	}

	private void writeFields(DataOutputStream dos) throws IOException {
		dos.writeShort(fields.size());
		for (FieldInfo fi : fields)
		{
			fi.write(dos);
		}
	}

	private void readMethods(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of methods = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			MethodInfo mi = new MethodInfo(this);
			mi.access_flags = (short) dis.readUnsignedShort();
			mi.nameIdx = (short) dis.readUnsignedShort();
			mi.descriptorIdx = (short) dis.readUnsignedShort();
			readAttributes(dis, mi.attributes, mi);
			methods.add(mi);
		}
		
	}

	private void writeMethods(DataOutputStream dos) throws IOException {
		dos.writeShort(methods.size());
		for (MethodInfo mi : methods)
		{
			mi.write(dos);
		}
	}

	private void readAttributes(DataInputStream dis, List<AttributeInfo> attrs, AnnotationHolder holder) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort();
			int len = dis.readInt();
			byte[] bytes = new byte[len];
			readBytes(dis, bytes);
			AttributeInfo attr = new AttributeInfo(pool, idx, bytes);
			attrs.add(attr);
			if (attr.hasName("RuntimeVisibleAnnotations"))
			{
				for (Annotation ann : Annotation.parse(this, attr))
					holder.addAnnotation(AnnotationType.RuntimeVisibleAnnotations, ann);
			}
			else if (attr.hasName("RuntimeInvisibleAnnotations"))
			{
				for (Annotation ann : Annotation.parse(this, attr))
					holder.addAnnotation(AnnotationType.RuntimeInvisibleAnnotations, ann);
			}
		}
		return;
	}

	void writeAttributes(DataOutputStream dos, List<AttributeInfo> attrs) throws IOException {
		dos.writeShort(attrs.size());
		for (AttributeInfo ai : attrs)
		{
			dos.writeShort(ai.nameIdx);
			dos.writeInt(ai.getBytes().length);
			dos.write(ai.getBytes());
		}
	}

	private CPInfo readPoolEntry(DataInputStream dis) throws IOException {
		int tag = dis.readUnsignedByte();
		switch (tag)
		{
		case CONSTANT_UTF8:
			return readUtf8Entry(dis);
		case CONSTANT_Integer:
			return readIntegerEntry(dis);
		case CONSTANT_Float:
			return readFloatEntry(dis);
		case CONSTANT_Long:
			return readLongEntry(dis);
		case CONSTANT_Double:
			return readDoubleEntry(dis);
		case CONSTANT_Class:
			return readClassEntry(dis);
		case CONSTANT_String:
			return readStringEntry(dis);
		case CONSTANT_Fieldref:
		case CONSTANT_Methodref:
		case CONSTANT_Interfaceref:
			return readRefEntry(dis, tag);
		case CONSTANT_NameAndType:
			return readNameAndType(dis);
		default:
		{
			/*
			for (int i=0;i<pool.length;i++)
				System.out.println(i + ": " + pool.get(i));
				*/
			throw new UtilException("There is no handler for tag " + tag);
		}
		}
	}

	private Utf8Info readUtf8Entry(DataInputStream dis) throws IOException {
		int len = dis.readUnsignedShort();
		byte[] bytes = new byte[len];
		readBytes(dis, bytes);
		return new Utf8Info(bytes);
	}
	
	private IntegerInfo readIntegerEntry(DataInputStream dis) throws IOException {
		return new IntegerInfo(dis.readInt());
	}
	
	private FloatInfo readFloatEntry(DataInputStream dis) throws IOException {
		return new FloatInfo(dis.readFloat());
	}

	private LongInfo readLongEntry(DataInputStream dis) throws IOException {
		int high = dis.readInt();
		int low = dis.readInt();
		return new LongInfo(high, low);
	}

	private DoubleInfo readDoubleEntry(DataInputStream dis) throws IOException {
		double val = dis.readDouble();
		return new DoubleInfo(val);
	}
	
	private ClassInfo readClassEntry(DataInputStream dis) throws IOException {
		int idx = dis.readUnsignedShort();
		return new ClassInfo(pool, idx);
	}

	private StringInfo readStringEntry(DataInputStream dis) throws IOException {
		int idx = dis.readUnsignedShort();
		return new StringInfo(pool, idx);
	}

	private RefInfo readRefEntry(DataInputStream dis, int tag) throws IOException {
		int clz = dis.readUnsignedShort();
		int nt = dis.readUnsignedShort();
		return new RefInfo(pool, clz, nt, tag);
	}

	private NTInfo readNameAndType(DataInputStream dis) throws IOException {
		int name = dis.readUnsignedShort();
		int descriptor = dis.readUnsignedShort();
		return new NTInfo(pool, name, descriptor);
	}

	public void addInterface(String intf) {
		int idx = pool.requireClass(intf);
		interfaces.add(idx);
	}

	public boolean extendsClass(String clzName) {
		String name = FileUtils.convertDottedToSlashPath(clzName);
		return name.equals(((ClassInfo)this.pool.get(this.super_idx)).justName());
	}

	public boolean nestedExtendsClass(JavaRuntimeReplica jrr, String clzName) {
		try
		{
			if (extendsClass(clzName))
				return true;
			String parentName = FileUtils.convertToDottedName(new File(((ClassInfo)this.pool.get(this.super_idx)).justName()));
			if (parentName.equals("java.lang.Object"))
				return false;
			return jrr.getClass(parentName).nestedExtendsClass(jrr, clzName);
		}
		catch (UtilException ex)
		{
			System.out.println("Error: " + ex.getMessage());
			if (ex.getMessage().startsWith("JRR cannot"))
				return false;
			throw ex;
		}
	}

	public boolean implementsInterface(Class<?> class1) {
		String name = FileUtils.convertDottedToSlashPath(class1.getCanonicalName());
		for (int idx : interfaces)
		{
			ClassInfo c = (ClassInfo)pool.get(idx);
			if (c.equals(name))
				return true;
		}
		return false;
	}

	protected void readBytes(DataInputStream dis, byte[] bytes) throws IOException {
		int off = 0;
		while (off < bytes.length)
		{
			int cnt = dis.read(bytes, off, bytes.length-off);
			off += cnt;
		}
	}

	public void thisClass(String name) {
		if (this_idx != -1)
			throw new UtilException("Cannot define 'this' class twice");
		this_idx = pool.requireClass(name);
	}

	public void superClass(String name) {
		if (super_idx != -1)
			throw new UtilException("Cannot define 'super' class twice");
		super_idx = pool.requireClass(name);
	}

	public void addField(FieldInfo field) {
		fields.add(field);
	}

	public void addMethod(MethodCreator ret) {
		// TODO: it would be good if we could spot duplicate methods here,
		// but at the moment we can't because we can't be guaranteed that any given method
		// creator has a signature.  When we fully move over to using GenAnnotation, then we can
		// do that.
		if (methods.contains(ret))
			throw new UtilException("Cannot add the same method twice");
		methods.add(ret);
	}

	@Override
	public String toString() {
		if (qualifiedName != null)
			return "BCF[" + qualifiedName + "]";
		return super.toString();
	}

	public Iterable<FieldInfo> allFields() {
		return fields;
	}

	public Iterable<MethodInfo> allMethods() {
		return methods;
	}
	
	public boolean hasMethodsWithAnnotation(String string) {
		String mapped = JavaInfo.map(string);
		for (MethodInfo mi : methods)
		{
			for (AttributeInfo ai : mi.attributes)
			{
				if (ai.hasName("RuntimeVisibleAnnotations"))
				{
					RuntimeVisibleAnnotations rva = new RuntimeVisibleAnnotations(this, ai);
					if (rva.has(mapped))
						return true;
				}
			}
		}
		return false;
	}

	public AttributeInfo newAttribute(String named, byte[] data) {
		return new AttributeInfo(pool, pool.requireUtf8(named), data);
	}

	public Annotation addAnnotation(AnnotationType type, Annotation annotation) {
		annotations.add(type, annotation);
		return annotation;
	}

	public Annotation getClassAnnotation(String ann) {
		for (AnnotationType i : annotations)
		{
			for (Annotation j : annotations.get(i))
				if (j.name.equals(ann))
					return j;
		}
		return null;
	}
}
