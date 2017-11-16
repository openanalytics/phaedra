package eu.openanalytics.phaedra.base.util.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

/**
 * A collection of Java reflection utilities.
 */
public class ReflectionUtils {

	/**
	 * See {@link ReflectionUtils#invoke(String, Object, Object[], Class[])}
	 */
	public static Object invoke(String methodName, Object object) {
		return invoke(methodName, object, (Object[])null);
	}

	/**
	 * See {@link ReflectionUtils#invoke(String, Object, Object[], Class[])}
	 */
	public static Object invoke(String methodName, Object object, Object... args) {
		Class<?>[] argClasses = null;
		if (args != null && args.length > 0) {
			argClasses = new Class[args.length];
			for (int i=0; i<args.length; i++) {
				argClasses[i] = args[i].getClass();
			}
		}
		return invoke(methodName, object, args, argClasses);
	}

	/**
	 * Invoke a method on an object using reflection.
	 * Also works on protected/private methods and classes that are not exposed to the caller.
	 * 
	 * @param methodName The name of the method to invoke.
	 * @param object The object to invoke the method on.
	 * @param args The arguments to pass to the method.
	 * @param argClasses The classes of the arguments.
	 * @return The return value of the method invokation.
	 */
	public static Object invoke(String methodName, Object object, Object[] args, Class<?>[] argClasses) {
		Object result = null;
		if (object == null) return result;
		Class<?> objectClass = object.getClass();

		Method method = null;
		while (objectClass != null && method == null) {
			try {
				method = objectClass.getDeclaredMethod(methodName, argClasses);
			} catch (NoSuchMethodException e) {
				// Method not here. Check the superclass.
				objectClass = objectClass.getSuperclass();
			}
		}
		try {
			if (method != null) {
				if (!method.isAccessible()) method.setAccessible(true);
				result = method.invoke(object, args);
			}
		} catch (Exception e) {}
		return result;
	}

	/**
	 * Return true if the field is compatible with the given parameters.
	 *
	 * @param field				The field to check.
	 * @param allowStatic		When false, non-static fields are incompatible.
	 * @param wrapPrimitives	When true primitives are wrapped before determining compatibility.
	 * @param allowSubclass		When true the given field may be a subclass of one of the field classes
	 * @param fieldClasses		The field classes to which the field must be compatible.
	 */
	public static boolean isCompatibleField(Field field, boolean allowStatic, boolean wrapPrimitives, boolean allowSubclass, List<Class<?>> fieldClasses) {
		Class<?> fieldType = (wrapPrimitives && field.getType().isPrimitive()) ? ClassUtils.primitiveToWrapper(field.getType()) : field.getType();
		return (allowStatic || !Modifier.isStatic(field.getModifiers()))
				&& (fieldClasses.contains(fieldType) || (allowSubclass && fieldType.getSuperclass() != null && fieldClasses.contains(fieldType.getSuperclass())));
	}

	/**
	 * Returns the fields from the given class which are compatible with the given parameters.
	 */
	public static Collection<Field> getCompatibleFields(final Class<?> clazz, final boolean allowStatic, final boolean allowSubclass, final boolean wrapPrimitives, final List<Class<?>> fieldClasses) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> isCompatibleField(field, allowStatic, wrapPrimitives, allowSubclass, fieldClasses))
				.collect(Collectors.toList());
	}

	/**
	 * Returns the static fields on the given class.
	 */
	public static Collection<Field> getDeclaredStaticFields(Class<?> clazz, final boolean isStatic) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()) == isStatic)
				.collect(Collectors.toList());
	}

	/**
	 * Gets the static or non-static fields from the given class which are from the given fieldClass (or its wrapped counterpart for primitives).
	 * If allowChild is true, also fields from classes which inherit from the given field class are allowed.
	 */
	public static Collection<Field> getDeclaredWrappedFields(final Class<?> clazz, final Class<?> fieldClass, final boolean isStatic, final boolean allowChild) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> {
					Class<?> wrappedFieldClass = field.getType().isPrimitive() ? ClassUtils.primitiveToWrapper(field.getType()) : field.getType();
					return Modifier.isStatic(field.getModifiers()) == isStatic && (fieldClass.equals(wrappedFieldClass) || (allowChild && fieldClass.equals(wrappedFieldClass.getSuperclass())));
				}).collect(Collectors.toList());
	}

	public static Collection<Field> getDeclaredInheritedFields(Class<?> clazz, final Class<?> parentClass) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> parentClass.equals(field.getType().getSuperclass()))
				.collect(Collectors.toList());
	}

	public static Collection<Field> getDeclaredInterfaceFields(Class<?> clazz, final Class<?> declaredFieldInterface) {
		return Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> Arrays.asList(field.getType().getInterfaces()).contains(declaredFieldInterface))
				.collect(Collectors.toList());
	}

	public static Collection<String> getFieldNames(Collection<Field> fields) {
		return fields.stream()
				.map(field -> field.getName())
				.collect(Collectors.toList());
	}

	public static <T> T getFieldObject(Object object, String fieldName, Class<T> clazz) {
		Class<?> objectClass = object.getClass();
		Field field = null;
		while (field == null && objectClass != null) {
			try { field = objectClass.getDeclaredField(fieldName); } catch (NoSuchFieldException e) {}
			objectClass = objectClass.getSuperclass();
		}
		if (field != null) {
			try {
				return getFieldObject(object, field, clazz);
			} catch (SecurityException | IllegalAccessException e) {
				EclipseLog.error("Reflection lookup failed: field " + fieldName + " in " + object, e, Activator.getDefault());
			}
		}
		return null;
	}
	
	public static <T> T getFieldObject(Object object, Field field, Class<T> clazz) throws IllegalAccessException {
		Object fieldObject = getFieldObject(object, field);
		return SelectionUtils.getAsClass(fieldObject, clazz);
	}

	public static Object getFieldObject(Object object, Field field) throws IllegalAccessException {
		boolean accessible = field.isAccessible();
		if (!accessible) field.setAccessible(true);
		Object fieldValue = field.get(object);
		if (!accessible) field.setAccessible(false);
		return fieldValue;
	}

}
