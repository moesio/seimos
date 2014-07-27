package br.com.seimos.commons.dao;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.persistence.Id;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.sql.JoinFragment;
import org.hibernate.transform.RootEntityResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import br.com.seimos.commons.hibernate.CustomPropertyAliasProjection;
import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filter.Condition;
import br.com.seimos.commons.hibernate.Filter.Distinct;
import br.com.seimos.commons.hibernate.Filter.Function;
import br.com.seimos.commons.hibernate.Filter.Order;
import br.com.seimos.commons.hibernate.Filter.Projection;
import br.com.seimos.commons.hibernate.Filter.Wildcard;
import br.com.seimos.commons.hibernate.Filters;
import br.com.seimos.commons.hibernate.ProjectionResultTransformer;
import br.com.seimos.commons.reflection.Reflection;

public class GenericDaoImpl<Model> extends HibernateDaoSupport implements GenericDao<Model> {

	private Class<Model> entityClass;

	@SuppressWarnings("unchecked")
	public GenericDaoImpl() {
		this.entityClass = (Class<Model>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	protected HibernateTemplate hibernateTemplate;

	@Autowired
	public void setHibernateTemplate(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	public Session getCurrentSession() {
		return super.getSession();
	}

	public Class<Model> getEntityClass() {
		return entityClass;
	}

	public Model create(Model entity) {
		Serializable id = getHibernateTemplate().save(entity);
		getSessionFactory().getClassMetadata(getEntityClass()).setIdentifier(entity, id, (SessionImplementor) getCurrentSession());
		return entity;
	}
	
	public void createOrUpdate(Model entity) {
		getHibernateTemplate().saveOrUpdate(entity);
	}

	public Model retrieve(Integer id) throws DataAccessException {
		return (Model) getHibernateTemplate().get(getEntityClass(), id);
	}

	public Model update(Model entity) {
		getHibernateTemplate().update(entity);
		return entity;
	}
	
	public void remove(Integer id) throws InstantiationException, IllegalAccessException{
		Model entity = entityClass.newInstance();
		getSessionFactory().getClassMetadata(getEntityClass()).setIdentifier(entity, id, (SessionImplementor) getCurrentSession());
		getHibernateTemplate().delete(entity);
	}

	public void remove(Model entity) {
		getHibernateTemplate().delete(entity);
	}

	@SuppressWarnings("unchecked")
	public List<Model> list() {
		Criteria criteria = getSession().createCriteria(getEntityClass());
		criteria.setResultTransformer(RootEntityResultTransformer.INSTANCE);

		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	public List<Model> find(Model entity) {
		if (entity == null) {
			return list();
		}

		List<Model> list = createListCriteria(entity).list();
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<Model> sortedFind(Model entity, String... order) {
		if (entity == null) {
			return list();
		}

		Criteria criteria = createListCriteria(entity);

		for (int i = 0; i < order.length; i++) {
			if (order[i].startsWith("-")) {
				criteria.addOrder(org.hibernate.criterion.Order.desc(order[i].substring(1)));
			} else if (order[i].startsWith("+")) {
				criteria.addOrder(org.hibernate.criterion.Order.asc(order[i].substring(1)));
			} else {
				criteria.addOrder(org.hibernate.criterion.Order.asc(order[i]));
			}
		}

		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	public Model findUnique(Model entity) {
		Criteria criteria = getSession().createCriteria(entity.getClass());
		criteria.setResultTransformer(RootEntityResultTransformer.INSTANCE);

		createCriteria(criteria, entity);

		return (Model) criteria.uniqueResult();
	}

	/**
	 * @deprecated Use find(java.util.List<br.com.seimos.commons.hibernate.Filters>
	 *             ) instead
	 */
	public List<Model> find(Filter... filters) {
		return find(Arrays.asList(filters));
		// Filter[] filterArray = new Filter[filters.getEntries().size()];
		// return find(filters.getEntries().toArray(filterArray));
	}

	public List<Model> find(Filters filters) {
		return find(filters.getEntries());
	}
	
	public List<Model> find(Filters filters, Integer firstResult, Integer maxResult) {
		return find(filters.getEntries(), firstResult, maxResult);
	}
	
	public List<Model> find(List<Filter> filters) {
		return find(filters, null, null);
	}

	@SuppressWarnings("unchecked")
	public List<Model> find(List<Filter> filters, Integer firstResult, Integer maxResult) {
		Criteria criteria = getSession().createCriteria(getEntityClass());
		boolean usingFunction = false;
		boolean distinct = false;

		if (filters.size() == 0) {
			return list();
		} else {
			ProjectionList projections = Projections.projectionList();
			
			ArrayList<String> paths = new ArrayList<String>();
			ArrayList<String> grouping = new ArrayList<String>();

			ArrayList<String> wildcardList = new ArrayList<String>();

			for (Iterator<Filter> iterator = filters.iterator(); iterator.hasNext();) {
				Filter filter = (Filter) iterator.next();

				if (filter.getWildcard().equals(Wildcard.YES)) {
					String attribute = filter.getAttribute();
					if (attribute.equals("*")) {
						wildcardList.add("." + attribute);
					} else {
						wildcardList.add(attribute);
					}
					iterator.remove();
				}
			}

			if (wildcardList.size() > 0) {
				for (String attribute : wildcardList) {
					filters.addAll(getMatchAttributes(getEntityClass(), attribute));
				}
			}
			
			ListIterator<Filter> iterator = filters.listIterator();
			
			for (int index = 0; filters.size() > index; index++) {
				
				Filter filter = filters.get(index);
				if (filter.getDistinct() != null && filter.getDistinct() == Distinct.YES) {
					distinct = true;
					continue;
				}

				String attributePath = filter.getAttribute();
				Function function = filter.getFunction();
				Order order = filter.getOrder();
				if (filter.getFunction() != null) {
					usingFunction = true;
				}

				if (attributePath == null && filter.getFilters() != null) {
					// Isso DEVE acontecer quando for usado Condition.AND ou
					// Condition.OR
					Criterion restriction = createFilterRestriction(filter, "DOESN'T MATTER");
					if (restriction != null){
						criteria.add(restriction);
					}
				} else if (attributePath.contains(".")) {
					// atributos com associações

					String associationPath = attributePath.substring(0, attributePath.lastIndexOf("."));
					String[] aliasArray = attributePath.split("\\.");

					for (int i = 0; i < aliasArray.length - 1; i++) {
						StringBuffer proj = new StringBuffer();
						for (int y = 0; y <= i; y++) {
							if (y != i) {
								proj.append(aliasArray[y]);
								proj.append(".");
							} else {
								proj.append(aliasArray[y]);
							}
						}
						if (!paths.contains(proj.toString()) && !Reflection.isEmbedded(getEntityClass(), proj.toString())) {
							// impede a duplicação de aliases
							paths.add(proj.toString());
							String criteriaAlias = proj.toString().replace(".", "_");
							if (filter.getJoinType() == null) {
								criteria.createCriteria(proj.toString(), criteriaAlias);
							} else {
								criteria.createCriteria(proj.toString(), criteriaAlias, filter.getJoinType().getValue());
							}
						}
					}

					String alias = Reflection.isEmbedded(getEntityClass(), associationPath) ? associationPath : associationPath.replace(".", "_");
					String attrib = attributePath.substring(attributePath.lastIndexOf(".") + 1);
					String propertyName = alias + "." + attrib;

					if (function != null) {
						addFunctionToCriteria(projections, function, attributePath);
					} else if (attributePath != null && filter.getProjection() == Projection.YES) {
						projections.add(new CustomPropertyAliasProjection(propertyName, attributePath));
						grouping.add(attributePath);
					}

					if (order != null) {
						addOrderToCriteria(criteria, order, attributePath);
					}

					Criterion restriction = createFilterRestriction(filter, propertyName);
					if (restriction != null) {
						criteria.add(restriction);
					}
				} else {
					// não contém associações, ou seja, atributos com "."
					if (Reflection.isCollection(getEntityClass(), attributePath)) {

						if (!paths.contains(attributePath)) {
							addFiltersFromCollectionFields(iterator, getEntityClass(), attributePath);
							criteria.createCriteria(attributePath, attributePath, JoinFragment.LEFT_OUTER_JOIN);
							paths.add(attributePath);
						}
						
						
					} else {
						if (function != null) {
							addFunctionToCriteria(projections, function, attributePath);
						} else if (filter.getProjection().equals(Projection.YES)) {
							// se o atributo será projetado na consulta
							projections.add(new CustomPropertyAliasProjection(attributePath, attributePath));
							grouping.add(attributePath);
						}
	
						if (order != null) {
							addOrderToCriteria(criteria, order, attributePath);
						}
	
						Criterion restriction = createFilterRestriction(filter, attributePath);
						if (restriction != null) {
							criteria.add(restriction);
						}
					}
				}
			} // for filter

			if (distinct) {
				criteria.setProjection(Projections.distinct(projections));
			} else {
				criteria.setProjection(projections);
			}
			if (usingFunction) {
				for (String group : grouping) {
					projections.add(Projections.groupProperty(group));
				}
			}
		} // filters.length == 0

		criteria.setResultTransformer(new ProjectionResultTransformer(getEntityClass()));

		if (firstResult != null) {
			criteria.setFirstResult(firstResult);
		}
		if (maxResult != null) {
			if (!maxResult.equals(0)) {
				criteria.setMaxResults(maxResult);
			}
		}

		List<Model> list = criteria.list();
		return list;
	}

	private void addFiltersFromCollectionFields(ListIterator<Filter> filtersIterator, Class<Model> clazz, String attributePath) {
		try {
			filtersIterator.next();
			Field field = clazz.getDeclaredField(attributePath);
			Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			Field[] fields = Reflection.getNoTransientFields((Class<?>) type);
			for (Field f : fields) {
				filtersIterator.add(new Filter(attributePath + "." + f.getName()));
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private List<? extends Filter> getMatchAttributes(Class<Model> clazz, String regex) {
		ArrayList<Filter> filters = new ArrayList<Filter>();
		Field[] declaredFields = Reflection.getNoTransientFields(clazz);
		for (Field field : declaredFields) {
			String fieldName = field.getName();
			if (fieldName.matches(regex) && !Reflection.isEntity(field.getClass())) {
				filters.add(new Filter(fieldName));
			}
		}
		return filters;
	}

	public Model findUnique(Filters filters) {
		return findUnique(filters.getEntries());
	}

	public Model findUnique(Filter... filters) {
		return findUnique(Arrays.asList(filters));
	}

	public Model findUnique(List<Filter> filters) {
		List<Model> list = (List<Model>) find(filters);
		if (list.size() == 0)
			return null;
		if (list.size() == 1) {
			return (Model) list.get(0);
		} else {
			throw new HibernateException("Consulta a " + getClass().getSimpleName() + " não traz resultado único");
		}
	}

	private Criterion createFilterRestriction(Filter filter, String propertyName) {

		Object value = filter.getValue();
		Condition condition = filter.getCondition();
		Criterion criterion = null;

		if (condition == null) {
			// Infere a condição baseada no tipo do argumento value
			if (value != null && !value.equals("")) {
				if (value.getClass() == String.class) {
					criterion = Restrictions.ilike(propertyName, (String) value, MatchMode.START);
				} else if (value.getClass() != List.class) {
					criterion = Restrictions.eq(propertyName, value);
				}
			}
		} else {
			// condição explicitamente definida
			switch (condition) {
			case AND:
				if (filter.getFilters() == null || filter.getFilters().getClass() != Filter[].class) {
					throw new IllegalArgumentException("Filters should be set for Condition.AND. Use Filter(Condition condition, Filter... filters) constructor");
				}
				criterion = Restrictions.and(createFilterRestriction(filter.getFilters()[0], filter.getFilters()[0].getAttribute()),
						createFilterRestriction(filter.getFilters()[1], filter.getFilters()[1].getAttribute()));
				break;

			case OR:
				if (filter.getFilters() == null || filter.getFilters().getClass() != Filter[].class) {
					throw new IllegalArgumentException("Filters should be set for Condition.OR. Use Filter(Condition condition, Filter... filters) constructor");
				}
				criterion = Restrictions.or(createFilterRestriction(filter.getFilters()[0], filter.getFilters()[0].getAttribute()),
						createFilterRestriction(filter.getFilters()[1], filter.getFilters()[1].getAttribute()));
				break;

			case BETWEEN:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.between(propertyName, ((Object[]) value)[0], ((Object[]) value)[1]);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type Object[]");
				}
				break;

			case ENDS_WITH:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.ilike(propertyName, (String) value, MatchMode.END);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type String");
				}
				break;

			case ENDS_WITH_CASE_SENSITIVE:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.like(propertyName, (String) value, MatchMode.END);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type String");
				}
				break;

			case EQUALS:
				if (value == null) {
					// TODO Verificar se é possível comparar com string vazia
					// value = "";
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				if (value.getClass() == String.class) {
					criterion = Restrictions.eq(propertyName, (String) value);
				} else {
					criterion = Restrictions.eq(propertyName, value);
				}
				break;

			case EQ_PROPERTY:
				if (value == null){
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try{
					criterion = Restrictions.eqProperty(propertyName, (String) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is not an attribute from " + getEntityClass().toString());
				}
				
			case EQUALS_CASE_INSENSITIVE:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.ilike(propertyName, (String) value, MatchMode.EXACT);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type String");
				}
				break;

			case GREATER:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				criterion = Restrictions.gt(propertyName, value);
				break;

			case GREATER_THAN_PROPERTY:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.gtProperty(propertyName, (String) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is not an attribute from " + getEntityClass().toString());
				}
				break;

			case GREATER_OR_EQUALS:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				criterion = Restrictions.ge(propertyName, value);
				break;

			case GREATER_OR_EQUALS_THAN_PROPERTY:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.geProperty(propertyName, (String) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is not an attribute from " + getEntityClass().toString());
				}
				break;

			case IN:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.in(propertyName, (Object[]) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type Object[]");
				}
				break;

			case LESS:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				criterion = Restrictions.lt(propertyName, value);
				break;
			case LESS_THAN_PROPERTY:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.ltProperty(propertyName, (String) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is not an attribute from " + getEntityClass().toString());
				}
				break;
			case LESS_OR_EQUALS:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				criterion = Restrictions.le(propertyName, value);
				break;
			case LESS_OR_EQUALS_THAN_PROPERTY:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.leProperty(propertyName, (String) value);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is not an attribute from " + getEntityClass().toString());
				}
				break;
			case NOT_EQUALS:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				criterion = Restrictions.ne(propertyName, value);
				break;
			case NOT_IN:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.not(Restrictions.in(propertyName, (Object[]) value));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type Object[]");
				}
				break;
			case NOT_NULL:
				if (value != null) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" should not be set. Use Filter(String attribute, Condition condition) constructor");
				}
				criterion = Restrictions.isNotNull(propertyName);
				break;
			case NULL:
				if (value != null) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" should not be set. Use Filter(String attribute, Condition condition) constructor");
				}
				criterion = Restrictions.isNull(propertyName);
				break;
			case STARTS_WITH:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.ilike(propertyName, (String) value, MatchMode.START);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type String");
				}
				break;

			case STARTS_WITH_CASE_SENSITIVE:
				if (value == null) {
					throw new IllegalArgumentException("A value must be set. Constructor Filter(String attribute, Object value, Condition condition) is expected for Condition."
							+ filter.getCondition());
				}
				try {
					criterion = Restrictions.like(propertyName, (String) value, MatchMode.START);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("\"" + value.toString() + "\" is " + value.getClass() + ". It must be of type String");
				}
				break;
			case NOT_ENDS_WITH:
				// TODO
				break;
			case NOT_ENDS_WITH_CASE_SENSITIVE:
				// TODO
				break;
			case NOT_STARTS_WITH:
				// TODO
				break;
			case NOT_STARTS_WITH_CASE_SENSITIVE:
				// TODO
				break;
			default:
				break;
			}
		}

		return criterion;
	}

	private void addFunctionToCriteria(ProjectionList projections, Function function, String attributePath) {
		switch (function) {
		case MAX:
			projections.add(Projections.alias(Projections.max(attributePath), attributePath));
			break;
		case AVG:
			projections.add(Projections.alias(Projections.avg(attributePath), attributePath));
			break;
		case COUNT:
			projections.add(Projections.alias(Projections.count(attributePath), attributePath));
			break;
		case MIN:
			projections.add(Projections.alias(Projections.min(attributePath), attributePath));
			break;
		case SUM:
			projections.add(Projections.alias(Projections.sum(attributePath), attributePath));
			break;
		case COUNT_DISTINCT:
			projections.add(Projections.alias(Projections.countDistinct(attributePath), attributePath));
			break;
		}
	}

	private void addOrderToCriteria(Criteria criteria, Order order, String attributePath) {
		switch (order) {
		case ASC:
			criteria.addOrder(org.hibernate.criterion.Order.asc(attributePath));
			break;
		case DESC:
			criteria.addOrder(org.hibernate.criterion.Order.desc(attributePath));
			break;
		}
	}

	private Criteria createListCriteria(Model entity) {
		Criteria criteria = getSession().createCriteria(entity.getClass());
		criteria.setResultTransformer(RootEntityResultTransformer.INSTANCE);

		createCriteria(criteria, entity);

		return criteria;
	}

	private void createCriteria(Criteria criteria, Object entidade) {
		createCriteria(criteria, entidade, "");
	}

	private void createCriteria(Criteria criteria, Object entidade, String embeddedField) {
		Class<? extends Object> clazz = entidade.getClass();
		embeddedField = (embeddedField.equals("") ? "" : embeddedField + ".");

		for (Field field : clazz.getDeclaredFields()) {
			String fieldName = field.getName();
			Method method;
			Object result = null;
			try {
				method = clazz.getMethod(Reflection.getGetter(field));
				result = method.invoke(entidade);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			/*
			 * for (int j = 0; j < sortedProperties.length; j++) { if
			 * (sortedProperties[j].equals(field.getName()) ||
			 * sortedProperties[j].equals("+" + field.getName()))
			 * criteria.addOrder(Order.asc(field.getName())); else if
			 * (sortedProperties[j].equals("-" + field.getName()))
			 * criteria.addOrder(Order.desc(field.getName())); }
			 */

			if (result != null && !result.equals("")) {
				if (Reflection.isEntity(result.getClass())) {
					Criteria subCriteria = criteria.createCriteria(embeddedField + fieldName, JoinFragment.LEFT_OUTER_JOIN);
					createCriteria(subCriteria, result);
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
		}

	}

	public Model findById(Object id) {

		Filters filters = new Filters();
		filters.add(new Filter("*", Wildcard.YES));

		Field[] fields = getEntityClass().getDeclaredFields();

		boolean idFound = false;
		for (Field field : fields) {
			if (field.getAnnotation(Id.class) != null) {
				filters.add(new Filter(field.getName(), id));
				idFound = true;
			}
		}

		if (idFound) {
			return findUnique(filters);
		} else {
			throw new QueryException(getEntityClass().getName() + " doesn't have @Id or @EmbeddedId annotated field");
		}
	}

	public List<Model> listByName(String description) {

		try {
			return find(new Filters().add(new Filter("id")).add(new Filter("descricao", description)).add(new Filter("descricao", Filter.Order.ASC)));
		} catch (QueryException e) {
			throw new QueryException("Não existe o atributo \"id\" ou \"descricao\" em " + getClass().getSimpleName() + ". \nMétodo listByName(String) deve ser sobrescrito.");
		}
	}

}
