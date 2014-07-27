/**
 * 
 */
package br.com.seimos.commons.hibernate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.transform.ResultTransformer;

/**
 * @author Moésio Medeiros
 * @date: 09/10/2007 15:50:34
 */
public class ProjectionResultTransformer implements ResultTransformer {
	private Class<?> resultClass;

	public ProjectionResultTransformer(Class<?> resultClass) {
		if (resultClass == null) {
			throw new IllegalArgumentException("resultClass cannot be null");
		}
		this.resultClass = resultClass;
	}

	@SuppressWarnings("rawtypes")
	public List<?> transformList(List collection) {
		return collection;
	}

	@SuppressWarnings("unchecked")
	public Object transformTuple(Object[] values, String[] aliases) {
		Object result = null;
		try {
			result = resultClass.newInstance();
			for (int i = 0; i < values.length; i++) {
				String alias = aliases[i];
				if (alias != null) {
					Object tuple = values[i];
					
					if (alias.contains(".")) {
						String association = alias.substring(0, alias.indexOf("."));
						Field field = result.getClass().getDeclaredField(association);
						field.setAccessible(true);

						String subAlias = alias.substring(alias.indexOf(".") + 1);
						
						ArrayList<Object> list = new ArrayList<Object>();
						if (field.getType().isInstance(list)) {
							Object instance;
							if (field.get(result) == null)
							{
								field.set(result, list);
								Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
								instance = ((Class<?>) type).newInstance();
								list.add(instance);
							} 
							else {
								list = (ArrayList<Object>) field.get(result);
								instance = list.get(list.size() - 1);
							}
							set(instance, tuple, subAlias);
						}
						else {
							Object instance;
							if (field.get(result) == null) {
								instance = field.getType().newInstance();
							} else {
								instance = field.get(result);
							}
							set(instance, tuple, subAlias);
							field.set(result, instance);
						}
						
					} else {
						set(result, tuple, alias);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Impossível transform de " + resultClass.getName() + " devido a "
					+ e.getMessage() + ". Verifique se " + resultClass.getSimpleName() + " tem construtor padrão");
		}

		return result;
	}

	/**
	 * Cria o ROOT_OBJECT (resultClass) baseado nos pares tupla/alias,
	 * percorrendo subaliases, associações, até a última propriedade
	 * 
	 * @param result
	 *            ROOT_OBJECT
	 * @param tuple
	 *            valor
	 * @param alias
	 *            alias :-)
	 * @throws HibernateException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void set(Object result, Object tuple, String alias) {
		try {
			
			String propertyRoot;
			if (alias.contains(".")) { 
				propertyRoot = alias.substring(0, alias.indexOf("."));
			} else {
				propertyRoot = alias;
			}

			Class<? extends Object> resultClazz = result.getClass();
			Field field;
			field = resultClazz.getDeclaredField(propertyRoot);
			field.setAccessible(true);

			if (alias.contains(".")) {
				Object instance = field.getType().newInstance();
				String substring = alias.substring(alias.indexOf(".") + 1);
				set(instance, tuple, substring);
				field.set(result, instance);
			} else {
				field.set(result, tuple);
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
