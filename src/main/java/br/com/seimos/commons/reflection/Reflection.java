package br.com.seimos.commons.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.LazyInitializationException;

public class Reflection {
	
	public Collection<String> entityDeepPath(Class<?> clazz) {
		
		Collection<String> paths = new ArrayList<String>();

		createPaths(paths, clazz);
		
		for (String string : paths) {
			System.out.println(string);
		}

		return paths;
	}

	private void createPaths(Collection<String> paths, Class<?> clazz) {
		createPaths(paths, clazz, "");
	}

	private void createPaths(Collection<String> paths, Class<?> clazz, String embeddedField) {
		//Class<? extends Object> clazz = entidade.getClass();
//		embeddedField = (embeddedField.equals("") ? "" : embeddedField + ".");

		for (Field field : clazz.getDeclaredFields()) {
			
			if (isEntity(field.getType()))
			{
				createPaths(paths, clazz);
			}
			else
			{
				System.out.println(field.getName());
			}
			String fieldName = field.getName();
			Method method = null;
//			Object result = null;
			try {
				method = clazz.getMethod(Reflection.getGetter(field));
				
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} 
			
			System.out.println(fieldName);
			System.out.println(method.getName());

			/*/////////
				if (Reflection.isEntity(result.getClass())) {
					Criteria subCriteria = criteria.createCriteria(embeddedField + fieldName, JoinFragment.LEFT_OUTER_JOIN);
					createPaths(subCriteria, result);
				} else if (Reflection.isEmbedded(result.getClass())) {
					createCriteria(criteria, result, embeddedField + fieldName);
				} else { // adiciona criterio
					if (field.getType() == Boolean.class || field.getType() == Integer.class) {
						criteria.add(Restrictions.eq(embeddedField + fieldName, result));
					} else if (field.getType() == String.class && !result.equals("")) {
						criteria.add(Restrictions.ilike(embeddedField + fieldName, (String) result, MatchMode.START));
					} else if (field.getType() == Date.class) {
						criteria.add(Restrictions.eq(embeddedField + fieldName, (Date) result));
					}
				}
			}
			 */
		}
	}
	
	public static boolean isEntity(Class<?> clazz) {
		boolean result = false;
		for (int i = 0; i < clazz.getAnnotations().length; i++) {
			Annotation annotation = clazz.getAnnotations()[i];
			Class<?> type = annotation.annotationType();
			if (type == Entity.class) {
				return true;
			}
		}
		return result;
	}

	public static String getGetter(Field field) {
		String fieldName = field.getName();
		String methodName = ((field.getType() == Boolean.TYPE) ? "is" : "get") + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		return methodName;
	}

	public static String getGetter(String property) {
		return "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
	}

	public static String getSetter(String property) {
		return "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
	}

	public static boolean isEmbedded(Class<?> clazz) {
		boolean result = false;
		for (int i = 0; i < clazz.getAnnotations().length; i++) {
			Annotation annotation = clazz.getAnnotations()[i];
			Class<?> type = annotation.annotationType();
			if (type == Embeddable.class) {
				return true;
			}
		}
		return result;
	}

	public static Field[] getNoTransientFields(Class<?> clazz) {
		Field[] notTransientFields = {};
		Collection<Field> fieldsCollection = new ArrayList<Field>();
		Field[] declaredFields = clazz.getDeclaredFields();
		for (int i = 0; i < declaredFields.length; i++) {
			boolean isTransient = false;
			Field field = clazz.getDeclaredFields()[i];
			Annotation[] annotations = field.getAnnotations();
			for (int j = 0; j < annotations.length; j++) {
				if (annotations[j].annotationType() == Transient.class)
					isTransient = true;
			}
			if (!isTransient) {
				fieldsCollection.add(field);
			}
		}
		notTransientFields = (Field[]) fieldsCollection.toArray(notTransientFields);
		return notTransientFields;
	}

	public static Object invoke(Object entity, String property) {
		Class<?> clazz = entity.getClass();

		Object invocation = null;
		try {
			Method method = null;
			method = clazz.getMethod(Reflection.getGetter(property));
			invocation = method.invoke(entity);
		} catch (LazyInitializationException e) {
			throw new LazyInitializationException("");
		} catch (Exception e) {
			// em caso de não existir o método. Praticamente impossível, já que
			// estou trazendo um m�todo preexistente
			e.printStackTrace();
		}
		return invocation;
	}

	public static boolean isEmbedded(Class<?> clazz, String attributePath) {
		Field field = null;

		try {
			String embeddedCandidateName = "";
			if (attributePath.contains(".")) {
				embeddedCandidateName = attributePath.substring(0, attributePath.indexOf("."));
				Field fieldCandidate = clazz.getDeclaredField(embeddedCandidateName);
				Class<?> clazzCandidate = fieldCandidate.getType(); // getGetter(fieldCandidate).getClass();
				int length = embeddedCandidateName.length();
				String associationPathCandidate = attributePath.substring(length + 1);
				return isEmbedded(clazzCandidate, associationPathCandidate);
			} else {
				embeddedCandidateName = attributePath;
			}
			field = clazz.getDeclaredField(embeddedCandidateName);
			for (int i = 0; i < field.getAnnotations().length; i++) {
				Annotation annotation = field.getAnnotations()[i];
				if (annotation.annotationType() == EmbeddedId.class) {
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

}
