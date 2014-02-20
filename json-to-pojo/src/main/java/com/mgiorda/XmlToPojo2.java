package com.mgiorda;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

public final class XmlToPojo2 {

	private static final String DEFAULT_FILE_PATH = "./src/main/java";

	private static final JCodeModel codeModel = new JCodeModel();

	public static void fromFile(String xmlFileName, String classPackage) {

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(xmlFileName);

		try {

			Document document = (Document) builder.build(xmlFile);
			Element root = document.getRootElement();

			generateCode(root, classPackage);
		} catch (Exception e) {

			throw new IllegalStateException(e);
		}
	}

	public static void fromJson(String xml, String classPackage) {

		SAXBuilder builder = new SAXBuilder();

		try {

			Document document = (Document) builder.build(xml);
			Element root = document.getRootElement();

			generateCode(root, classPackage);

		} catch (Exception e) {

			throw new IllegalStateException(e);
		}
	}

	private static void generateCode(Element root, String classPackage) {

		int lastIndexDot = classPackage.lastIndexOf(".");
		String packageName = classPackage.substring(0, lastIndexDot);
		String className = classPackage.substring(lastIndexDot + 1, classPackage.length());

		generateClass(packageName, className, root);
	}

	private static JClass generateClass(String packageName, String className, Element element) {

		JClass elementClass = null;

		if (isPrimitive(element)) {
			elementClass = getClassForPrimitive(packageName, className, element);
		} else if (isList(element)) {
			elementClass = getClassForArray(packageName, className, element);
		} else {
			elementClass = getClassForObject(packageName, className, element);
		}
		return elementClass;
	}

	private static boolean isPrimitive(Element element) {

		boolean isPrimitive = true;

		if (element.getAttributes().size() != 0) {
			isPrimitive = false;
		}

		@SuppressWarnings("unchecked")
		List<Element> children = element.getChildren();
		if (children.size() != 0) {
			isPrimitive = false;
		}

		return isPrimitive;
	}

	private static boolean isList(Element element) {

		boolean isList = true;

		if (element.getAttributes().size() == 0 && element.getChildren().size() > 0) {

			@SuppressWarnings("unchecked")
			List<Element> children = element.getChildren();

			String childNames = children.get(0).getName();

			for (int i = 1; i < children.size() && isList; i++) {

				if (!children.get(i).getName().equals(childNames)) {
					isList = false;
				}
			}
		} else {
			isList = false;
		}

		return isList;
	}

	private static JClass getClassForObject(String packageName, String className, Element obj) {

		Map<String, JClass> fields = new LinkedHashMap<String, JClass>();

		@SuppressWarnings("unchecked")
		List<Element> children = obj.getChildren();

		for (Element child : children) {

			String fieldName = child.getName();
			String fieldUppercase = getFirstUppercase(fieldName);

			JClass elementClass = generateClass(packageName, fieldUppercase, child);
			fields.put(fieldName, elementClass);
		}

		String classPackage = packageName + "." + className;
		generatePojo(classPackage, fields);

		JClass jclass = codeModel.ref(classPackage);
		return jclass;
	}

	private static JClass getClassForArray(String packageName, String className, Element array) {

		@SuppressWarnings("unchecked")
		List<Element> children = (List<Element>) array.getChildren();
		Element firstChild = children.get(0);
		String childrenName = firstChild.getName();

		JClass narrowClass = generateClass(packageName, childrenName, firstChild);

		String narrowName = narrowClass.name();
		Class<?> boxedClass = null;
		if (narrowName.equals("int")) {
			boxedClass = Integer.class;
		} else if (narrowName.equals("long")) {
			boxedClass = Long.class;
		} else if (narrowName.equals("double")) {
			boxedClass = Double.class;
		}
		if (boxedClass != null) {
			narrowClass = codeModel.ref(boxedClass);
		}

		JClass listClass = codeModel.ref(List.class).narrow(narrowClass);

		return listClass;
	}

	private static JClass getClassForPrimitive(String packageName, String className, Element xmlPrimitive) {

		JClass primitiveClass = null;

		String value = xmlPrimitive.getValue();

		if (value.equals("true") || value.equals("false")) {

			primitiveClass = codeModel.ref("boolean");
		} else {
			try {
				Double doubleValue = Double.valueOf(value);

				if (doubleValue != Math.round(doubleValue)) {
					primitiveClass = codeModel.ref("double");
				} else {
					long longValue = doubleValue.longValue();
					if (longValue >= Integer.MAX_VALUE) {
						primitiveClass = codeModel.ref("long");
					} else {
						primitiveClass = codeModel.ref("int");
					}
				}
			} catch (Exception ex) {
				primitiveClass = codeModel.ref(String.class);
			}
		}

		Map<String, JClass> fields = new LinkedHashMap<String, JClass>();
		fields.put(xmlPrimitive.getName(), primitiveClass);

		String classPackage = packageName + "." + className;
		generatePojo(classPackage, fields);

		JClass jclass = codeModel.ref(classPackage);
		return jclass;
	}

	public static void generatePojo(String className, String filePath, Map<String, JClass> fields) {

		try {
			JDefinedClass definedClass = codeModel._class(className);

			for (Map.Entry<String, JClass> field : fields.entrySet()) {

				addGetterSetter(definedClass, field.getKey(), field.getValue());
			}
			codeModel.build(new File(filePath));

		} catch (Exception e) {
			throw new IllegalStateException("Couldn't generate Pojo", e);
		}
	}

	public static void generatePojo(String className, Map<String, JClass> fields) {
		generatePojo(className, DEFAULT_FILE_PATH, fields);
	}

	private static void addGetterSetter(JDefinedClass definedClass, String fieldName, JClass fieldType) {

		String fieldNameWithFirstLetterToUpperCase = getFirstUppercase(fieldName);

		JFieldVar field = definedClass.field(JMod.PRIVATE, fieldType, fieldName);

		String getterPrefix = "get";
		String fieldTypeName = fieldType.fullName();
		if (fieldTypeName.equals("boolean") || fieldTypeName.equals("java.lang.Boolean")) {
			getterPrefix = "is";
		}
		String getterMethodName = getterPrefix + fieldNameWithFirstLetterToUpperCase;
		JMethod getterMethod = definedClass.method(JMod.PUBLIC, fieldType, getterMethodName);
		JBlock block = getterMethod.body();
		block._return(field);

		String setterMethodName = "set" + fieldNameWithFirstLetterToUpperCase;
		JMethod setterMethod = definedClass.method(JMod.PUBLIC, Void.TYPE, setterMethodName);
		String setterParameter = fieldName;
		setterMethod.param(fieldType, setterParameter);
		setterMethod.body().assign(JExpr._this().ref(fieldName), JExpr.ref(setterParameter));
	}

	private static String getFirstUppercase(String word) {

		String firstLetterToUpperCase = word.substring(0, 1).toUpperCase();
		if (word.length() > 1) {
			firstLetterToUpperCase += word.substring(1, word.length());
		}
		return firstLetterToUpperCase;
	}
}
