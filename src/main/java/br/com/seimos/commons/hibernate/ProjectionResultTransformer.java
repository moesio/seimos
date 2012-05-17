/**
 * 
 */
package br.com.seimos.commons.hibernate;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.PropertyAccessException;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;

/**
 * @author Moésio Medeiros
 * @date: 09/10/2007 15:50:34
 */
public class ProjectionResultTransformer implements ResultTransformer {
	private boolean distinct = false;
	private Class<?> resultClass;
	private ChainedPropertyAccessor propertyAccessor;

	public ProjectionResultTransformer(Class<?> resultClass) {
		distinct = false;
		if (resultClass == null) {
			throw new IllegalArgumentException("resultClass cannot be null");
		}
		this.resultClass = resultClass;
		propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] { new ProjectionPropertyAcessor() });
	}

	public ProjectionResultTransformer(Class<?> resultClass, boolean distinct) {
		this(resultClass);
		this.distinct = distinct;
	}

	public List<?> transformList(List collection) {
		return collection;
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result = null;
		try {
			result = resultClass.newInstance();
			for (int i = 0; i < tuple.length; i++) {
				if (aliases[i] != null)
					set(result, tuple[i], aliases[i]);
			}
		} catch (Exception e) {
			throw new RuntimeException("Impossível transform de " + resultClass.getName() + " devido a " + e.getMessage() + ". Verifique se " + resultClass.getSimpleName()
					+ " tem construtor padrão");
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
			Setter setter = propertyAccessor.getSetter(result.getClass(), alias);

			if (alias.indexOf(".") >= 0) {
				String propertyRoot = alias.substring(0, alias.indexOf("."));
				Getter getter = propertyAccessor.getGetter(result.getClass(), propertyRoot);

				Class<?> type = getter.getReturnType();
				Object instance;
				if (getter.get(result) == null) {
					instance = type.newInstance();
					setter.set(result, instance, null);
				} else {
					instance = getter.get(result);
				}

				set(instance, tuple, alias.substring(alias.indexOf(".") + 1));
			} else {
				setter.set(result, tuple, null);
			}
		} catch (IllegalAccessException iae) {
			throw new PropertyAccessException(iae, "IllegalAccessException occurred while calling", false, result.getClass(), alias);
		} catch (IllegalArgumentException iae) {
			throw new PropertyAccessException(iae, "IllegalArgumentException occurred calling", false, result.getClass(), alias);
		} catch (InstantiationException ie) {
			throw new PropertyAccessException(ie, "InstantiationException occurred inside", false, result.getClass(), alias);
		}
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

}
