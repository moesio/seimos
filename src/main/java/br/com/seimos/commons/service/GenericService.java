package br.com.seimos.commons.service;

import java.util.List;

import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filters;

public interface GenericService<Model>
{
	Model create(Model entity);
	Model retrieve(Integer id);
	Model update(Model entity);
	void remove(Integer id) throws InstantiationException, IllegalAccessException;
	void remove(Model entity);
	List<Model> list();
	List<Model> listByName(String description);
    List<Model> sortedFind(Model entity, String... order);
    List<Model> find(Model entity);
	List<Model> find(Filters filters);
	List<Model> find(Filters filters, Integer firstResult, Integer maxResult);
	List<Model> find(List<Filter> filters);
	List<Model> find(List<Filter> filters, Integer firstResult, Integer maxResult);
	List<Model> find(Filter... filters);
	Model findUnique(Model entity);
	Model findUnique(Filters filters);
	Model findUnique(Filter...filters);
	Model findUnique(List<Filter> filters);
	Model findById(Object id);
}
