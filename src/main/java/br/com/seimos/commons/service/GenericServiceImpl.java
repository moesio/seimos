package br.com.seimos.commons.service;

import java.util.List;

import br.com.seimos.commons.dao.GenericDao;
import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filters;

public abstract class GenericServiceImpl<Model, Dao extends GenericDao<Model>> implements GenericService<Model> {
	public abstract Dao getDao();

	public Model create(Model entity) {
		return getDao().create(entity);
	}

	public Model retrieve(Integer id) {
		return getDao().retrieve(id);
	}

	public Model update(Model entity) {
		return getDao().update(entity);
	}

	public void remove(Integer id) throws InstantiationException, IllegalAccessException{
		getDao().remove(id);
	}
	
	public void remove(Model entity) {
		getDao().remove(entity);
	}

	public List<Model> list() {
		return getDao().list();
	}

	public List<Model> find(Filters filters) {
		return getDao().find(filters, null, null);
	}
	
	public List<Model> find(Filters filters, Integer firstResult, Integer maxResult) {
		return getDao().find(filters, firstResult, maxResult);
	}
	
	public List<Model> find(List<Filter> filters) {
		return getDao().find(filters, null, null);
	}

	public List<Model> find(List<Filter> filters, Integer firstResult, Integer maxResult) {
		return getDao().find(filters, firstResult, maxResult);
	}
	
	public List<Model> find(Filter... filters) {
		return getDao().find(filters);
	}

	public List<Model> find(Model entity) {
		return getDao().find(entity);
	}

	public List<Model> sortedFind(Model entity, String... order) {
		return getDao().sortedFind(entity, order);
	}
	
	public Model findUnique(Filters filters){
		return getDao().findUnique(filters);
	}
	
	public Model findUnique(Filter... filters)
	{
		return getDao().findUnique(filters);
	}

	public Model findUnique(Model entity) {
		return getDao().findUnique(entity);
	}
	
	public Model findUnique(List<Filter> filters){
		return getDao().findUnique(filters);
	}

	public Model findById(Object id) {
		return getDao().findById(id);
	}

	public List<Model> listByName(String description) {
		return getDao().listByName(description);
	}
}
