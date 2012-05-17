package br.com.seimos.commons.dao;

import java.util.List;

import org.hibernate.Session;

import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filters;

public interface GenericDao<Model>
{
	Session getCurrentSession();
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
	Model findById(Integer id);
}
