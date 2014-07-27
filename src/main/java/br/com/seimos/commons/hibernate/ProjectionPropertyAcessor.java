package br.com.seimos.commons.hibernate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.PropertyAccessException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

public class ProjectionPropertyAcessor implements PropertyAccessor
{
    private static final Log log = LogFactory.getLog(ProjectionPropertyAcessor.class);

	@SuppressWarnings("rawtypes")
	public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException
    {
        return new ProjectionGetter(theClass, getterMethod(theClass, propertyName), propertyName);
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException
    {
        Method getter = getterMethod(theClass, propertyName);

        if (getter != null)
        {
            String capitalizedProperty;
            if (getter.getName().startsWith("is"))
                capitalizedProperty = getter.getName().substring(2);
            else
                capitalizedProperty = getter.getName().substring(3);
            try
            {
                Method setter = theClass.getDeclaredMethod("set" + capitalizedProperty, getter.getReturnType());
                return new ProjectionSetter(theClass, setter, propertyName);
            }
            catch (SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (NoSuchMethodException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

	private Method getterMethod(Class<?> theClass, String propertyName)
    {
        String firstLevelPropertyName = null;
        if (propertyName.indexOf(".") >= 0)
            firstLevelPropertyName = propertyName.substring(0, propertyName.indexOf("."));

        String property;
        if (firstLevelPropertyName != null)
        {
            property = firstLevelPropertyName;
        }
        else
        {
            property = propertyName;
        }

        Method[] methods = theClass.getDeclaredMethods();
        Method result = null;
        for (Method method : methods)
        {
            if (method.getName().equals("get" + capitalize(property)) || method.getName().equals("is" + capitalize(property)))
            {
                result = method;
            }
        }

        if (result == null)
            throw new PropertyNotFoundException("Verifique a existência da propriedade " + propertyName + " na classe " + theClass.getCanonicalName());
        else
            return result;
    }

    private String capitalize(String string)
    {
        return string.substring(0, 1).toUpperCase().concat(string.substring(1));
    }

    private final class ProjectionSetter implements Setter
    {
        private static final long serialVersionUID = -6341143292189995150L;
        private Class<?> clazz;
        private Method method;
        private String propertyName;

		@SuppressWarnings("rawtypes")
		public ProjectionSetter(Class theClass, Method method, String propertyName)
        {
            this.clazz = theClass;
            this.method = method;
            this.propertyName = propertyName;
        }

        public Method getMethod()
        {
            return method;
        }

        public String getMethodName()
        {
            return method.getName();
        }

        public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException
        {
            try
            {
                // TODO As funções agregadas retornam valores inteiros que serão populadas nos modelos com o tipo Long.
                // Recomenda-se mudar TODOS os atributos dos modelos que utilizam números com o objeto Long par Integer
                if (method.getParameterTypes()[0] == Integer.class && value != null)
                    value = Integer.valueOf(value.toString());
                else if (method.getParameterTypes()[0] == Long.class && value != null)
                    value = Long.valueOf(value.toString());
                method.invoke(target, value);
            }
            catch (NullPointerException npe)
            {
                if (value == null && method.getParameterTypes()[0].isPrimitive())
                {
                    throw new PropertyAccessException(npe, "Null value was assigned to a property of primitive type", true, clazz, propertyName);
                }
                else
                {
                    throw new PropertyAccessException(npe, "NullPointerException occurred while calling", true, clazz, propertyName);
                }
            }
            catch (InvocationTargetException ite)
            {
                throw new PropertyAccessException(ite, "Exception occurred inside", true, clazz, propertyName);
            }
            catch (IllegalAccessException iae)
            {
                throw new PropertyAccessException(iae, "IllegalAccessException occurred while calling", true, clazz, propertyName);
                //cannot occur
            }
            catch (IllegalArgumentException iae)
            {
                if (value == null && method.getParameterTypes()[0].isPrimitive())
                {
                    throw new PropertyAccessException(iae, "Null value was assigned to a property of primitive type", true, clazz, propertyName);
                }
                else
                {
                    log.error("IllegalArgumentException in class: " + clazz.getName() + ", setter method of property: " + propertyName);
                    log.error("expected type: " + method.getParameterTypes()[0].getName() + ", actual value: " + (value == null ? null : value.getClass().getName()));
                    throw new PropertyAccessException(iae, "IllegalArgumentException occurred while calling", true, clazz, propertyName);
                }
            }
        }
    }

    private final class ProjectionGetter implements Getter
    {
        private static final long serialVersionUID = 5005950045711155460L;
        private Class<?> clazz;
        private Method method;
        private String propertyName;

		ProjectionGetter(Class<?> clazz, Method method, String propertyName)
        {
            this.clazz = clazz;
            this.method = method;
            this.propertyName = propertyName;
        }

        public Object get(Object target) throws HibernateException
        {
            try
            {
                return method.invoke(target, (Object[]) null);
            }
            catch (InvocationTargetException ite)
            {
                throw new PropertyAccessException(ite, "Exception occurred inside", false, clazz, propertyName);
            }
            catch (IllegalAccessException iae)
            {
                throw new PropertyAccessException(iae, "IllegalAccessException occurred while calling", false, clazz, propertyName);
                //cannot occur
            }
            catch (IllegalArgumentException iae)
            {
                log.error("IllegalArgumentException in class: " + clazz.getName() + ", getter method of property: " + propertyName);
                throw new PropertyAccessException(iae, "IllegalArgumentException occurred calling", false, clazz, propertyName);
            }
        }

		@SuppressWarnings("rawtypes")
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) throws HibernateException
        {
            return get(target);
        }

        public Method getMethod()
        {
            return method;
        }

        public String getMethodName()
        {
            return method.getName();
        }

        public Class<?> getReturnType()
        {
            return method.getReturnType();
        }

		public Member getMember() {
			// TODO Auto-generated method stub
			return null;
		}

    }
}
