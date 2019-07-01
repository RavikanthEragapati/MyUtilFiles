package us.mi.state.dhs.fw.web.service.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.xml.sax.SAXException;

public class WFUtility {

	/**
	 * Method <i>listAllFields</i> provides with java.lang.reflect.Field[] of
	 * all the fields including <i>private</i> fields ina given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>List<String></tt> A List<String> containing all the Field
	 *         Names.
	 */
	public Function<Class<?>, Field[]> listAllFields = c -> {
		return c.getDeclaredFields();
	};

	/**
	 * Method <i>listAllFieldNames</i> provides with list of all the fields
	 * including <i>private</i> fields ina given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>List<String></tt> A List<String> containing all the Field
	 *         Names.
	 */
	public Function<Class<?>, List<String>> listAllFieldNames = c -> {
		List<String> lst = new ArrayList<String>();
		Field[] fields = this.listAllFields.apply(c);
		for (Field field : fields)
			lst.add(field.getName().toLowerCase());

		return lst;
	};

	/**
	 * Method <i>listAllSetters</i> provides with an array of all Methods
	 * (java.lang.reflect.Method) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>Method[]</tt> A java.lang.reflect.Method[] containing all the
	 *         method.
	 */
	public Function<Class<?>, Method[]> listAllMethods = c -> {
		return c.getMethods();
	};

	/**
	 * Method <i>listAllGettersNames</i> provides with list of all the Getter
	 * Names (String) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>lst</tt> A java.util.List<String> containing all the
	 *         <i>GETTER</i> method
	 */
	public Function<Class<?>, List<String>> listAllGettersNames = c -> {
		List<Method> lst = new ArrayList<Method>();
		List<String> lstNames = new ArrayList<String>();
		lst = this.listAllGetters.apply(c);
		lst.forEach((ls) -> {
			lstNames.add(ls.getName().toString());
		});

		return lstNames;
	};

	/**
	 * Method <i>listAllGetters</i> provides with list of all the Getter
	 * Methods(java.lang.reflect.Method) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>lst</tt> A java.util.List<java.lang.reflect.Method>
	 *         containing all the <i>GETTER</i> method
	 */
	public Function<Class<?>, List<Method>> listAllGetters = c -> {
		Method[] methods = this.listAllMethods.apply(c);
		List<Method> lst = new ArrayList<Method>();
		for (Method m : methods) {
			if (this.isGetter.apply(m)) {
				lst.add(m);
			}
		}
		return lst;
	};

	/**
	 * Method <i>listAllSettersNames</i> provides with list of all the Setter
	 * Names (String) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>lst</tt> A java.util.List<String> containing all the
	 *         <i>GETTER</i> method
	 */
	public Function<Class<?>, List<String>> listAllSettersNames = c -> {
		List<Method> lst = new ArrayList<Method>();
		List<String> lstNames = new ArrayList<String>();
		lst = this.listAllSetters.apply(c);
		lst.forEach((ls) -> {
			lstNames.add(ls.getName().toString());
		});

		return lstNames;
	};

	/**
	 * Method <i>listAllSetters</i> provides with list of all the Setter
	 * Methods(java.lang.reflect.Method) in a given class.
	 * 
	 * @param c
	 *            The parent pathname java.lang.Class<T>
	 * @return <tt>lst</tt> A java.util.List<java.lang.reflect.Method>
	 *         containing all the <i>SETTER</i> method
	 */
	public Function<Class<?>, List<Method>> listAllSetters = c -> {
		Method[] methods = this.listAllMethods.apply(c);
		List<Method> lst = new ArrayList<Method>();
		for (Method m : methods) {
			if (this.isSetter.apply(m)) {
				lst.add(m);
			}
		}
		return lst;
	};

	/**
	 * Method <i>isGetter</i> checks whether a method is a getter method of a
	 * class based on the following rule:
	 * <p>
	 * A getter method have its name start with "get" or "is", take 0
	 * parameters, and returns a value.
	 * </p>
	 * 
	 * @param m
	 *            The parent pathname java.lang.reflect.Method
	 * @return <tt>true</tt> if this Method is a Getter Method
	 */
	public Function<Method, Boolean> isGetter = m -> {
		if (!(m.getName().startsWith("get") || m.getName().startsWith("is")))
			return false;
		if (m.getParameterTypes().length != 0)
			return false;
		if (void.class.equals(m.getReturnType()))
			return false;
		return true;
	};

	/**
	 * Method <i>isSetter</i> checks whether a method is a setter method of a
	 * class based on the following rule:
	 * <p>
	 * A setter method have its name start with "set", and takes 1 parameter.
	 * </p>
	 * 
	 * @param m
	 *            The parent pathname java.lang.reflect.Method
	 * @return <tt>true</tt> if this Method is a Setter Method
	 */
	public Function<Method, Boolean> isSetter = m -> {
		if (!m.getName().startsWith("set"))
			return false;
		if (m.getParameterTypes().length != 1)
			return false;
		return true;
	};

	/**
	 * 
	 */
	public Function<String, String> xmlToJSON = s -> {
		JSONObject j = null;
		try {
			if (!s.isEmpty())
				j = XML.toJSONObject(s);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return j.toString();
	};

	/**
	 * 
	 * @param j
	 *            -
	 * @return s -
	 */
	public Function<String, String> jsonToXML = j -> {
		String s = null;
		JSONObject obj = null;
		try {
			if (!j.isEmpty())
				obj = new JSONObject(j);
			s = XML.toString(obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return s;
	};

	/**
	 * To validate XML with XSD
	 * 
	 * @param xmlString
	 *            - String holding XML content
	 * @param xsdPath
	 *            - String holding location of XSD file
	 * @return
	 */
	public BiFunction<String, String, Boolean> validateXMLToXSD = (xmlString, xsdPath) -> {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new File(xsdPath));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlString));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	};

}
