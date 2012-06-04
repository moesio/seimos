package br.com.seimos.commons.dao;

import java.util.List;

import org.hibernate.Session;

import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filters;

/**
 * Generic class for commons CRUD operation encapsulation
 * 
 * @author Moesio Medeiros
 *
 * @param <Model>
 */
public interface GenericDao<Model>
{
	/**
	 * Retrieves current hibernate session
	 * 
	 * @return
	 */
	Session getCurrentSession();
	
	/**
	 * Persists an entity on database
	 * 
	 * @param entity
	 * @return
	 */
	Model create(Model entity);
	
	/**
	 * Retrieve an entity for ID
	 * 
	 * @param id
	 * @return
	 */
	Model retrieve(Integer id);
	
	/**
	 * Updates an entity
	 * 
	 * @param entity
	 * @return
	 */
	Model update(Model entity);
	
	/**
	 * Deletes an entity based on id
	 * 
	 * @param id
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	void remove(Integer id) throws InstantiationException, IllegalAccessException;
	
	/**
	 * Deletes an entity
	 * 
	 * @param entity
	 */
	void remove(Model entity);
	
	/**
	 * List all available entries for model in database and its associations. 
	 * Caution! It can use huge memory
	 * 
	 * @return
	 */
	List<Model> list();
	
	/**
	 * List using Filter("id") and Filter("descricao", description). Too hardcoded for be maintained
	 * 
	 * @deprecated
	 * @param description
	 * @return
	 */
	List<Model> listByName(String description);
	
	/**
	 * Find by example for entity using a list of string atributes for sorting
	 * 
	 * @deprecated
	 * @param entity
	 * @param order
	 * @return
	 */
    List<Model> sortedFind(Model entity, String... order);

    /**
     * Find by example. All non null attributes will be used as restriction
     * 
     * @param entity
     * @return
     */
    List<Model> find(Model entity);
    
    /**
     * Find using filters as criteria
     * 
     * @see br.com.seimos.commons.hibernate.Filters
     * @param filters
     * @return
     */
	List<Model> find(Filters filters);
	
	/**
	 * Find using filters as criteria, firstResult and maxResult. Usually used for paging
	 * 
     * @see br.com.seimos.commons.hibernate.Filters
     * @see find(Filters)
	 * @param filters
	 * @param firstResult
	 * @param maxResult
	 * @return
	 */
	List<Model> find(Filters filters, Integer firstResult, Integer maxResult);
	
	/**
	 * Find using a list of Filter
	 * 
     * @see br.com.seimos.commons.hibernate.Filter
	 * @param filters
	 * @return
	 */
	List<Model> find(List<Filter> filters);
	
	/**
	 * Find using a list of Filter, firstResult and maxResult. Usually used for paging
	 * 
     * @see br.com.seimos.commons.hibernate.Filter
	 * @param filters
	 * @param firstResult
	 * @param maxResult
	 * @return
	 */
	List<Model> find(List<Filter> filters, Integer firstResult, Integer maxResult);
	
	/**
	 * Find using an array of Filter
	 * 
     * @see br.com.seimos.commons.hibernate.Filter
	 * @param filters
	 * @return
	 */
	List<Model> find(Filter... filters);
	
	/**
     * Find by example. All non null attributes will be used as restriction
	 * 
	 * @param entity
	 * @return
	 */
	Model findUnique(Model entity);

	/**
     * Find using filters as criteria
     * 
     * @see br.com.seimos.commons.hibernate.Filters
	 * @param filters
	 * @return
	 */
	Model findUnique(Filters filters);
	
	/**
	 * Find using an array of Filter
	 * 
     * @see br.com.seimos.commons.hibernate.Filter
	 * @param filters
	 * @return
	 */
	Model findUnique(Filter...filters);
	
	/**
	 * Find using a list of Filter
	 * 
     * @see br.com.seimos.commons.hibernate.Filter
	 * @param filters
	 * @return
	 */
	Model findUnique(List<Filter> filters);
	
	/**
	 * List using Filter("id", id) and Filter("descricao"). Too hardcoded for be maintained
	 * 
	 * @param id
	 * @return
	 */
	Model findById(Object id);
}
