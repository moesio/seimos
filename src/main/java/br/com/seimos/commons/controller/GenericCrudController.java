package br.com.seimos.commons.controller;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import br.com.seimos.commons.hibernate.Filter;
import br.com.seimos.commons.hibernate.Filter.Wildcard;
import br.com.seimos.commons.hibernate.Filters;
import br.com.seimos.commons.service.GenericService;

/**
 * Generic Controller for CRUD encapsulation. Subclasses must be annotated with both
 * @org.springframework.stereotype.Controller
 * @org.springframework.web.bind.annotation.RequestMapping(value = <"models">)
 * 
 * "models" stands for Model type
 * 
 * Must override constructor for attribute service, with its related service, usually interface <Model>Service. 
 * This constructor should be @Autowired annotated
 * 
 * This controller evaluates URL in RESTFul standard for CRUD as follows
 * 
 * /models/         - method GET, retrieves a complete list
 * /models/{id}     - method GET, retrieves a Model which id is {id}
 * /models/filter   - method GET, retrieves a Model using Model received on body as filter
 * /models          - method PUT, creates a new record of type Model. A Model in json format must be sent in body request
 * /models/batch    - method PUT, creates a new record of type Model for each of the list. A Model in json format must be sent in body request
 * /models          - method POST, update a record of type Model. A Model or a list of Model in json format must be sent in body request
 * /models/{id}     - method DELETE, delete a record of type Model which id is {id}
 * 
 * 
 * @author Jackson Coelho
 * @author Moesio Medeiros 
 * @date Thu Apr 20 17:45:29 BRT 2012
 * 
 */
public class GenericCrudController<Model> {

	protected GenericService<Model> service;

	private Logger logger = Logger.getLogger(GenericCrudController.class);

	public GenericCrudController(GenericService<Model> service) {
		this.service = service;
	}

	/**
	 * Retrieves a complete list of Model without any filter
	 * 
	 * @return Collection<Model>
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Collection<Model> list() {
		return service.list();
	}

	/**
	 * Retrieves a Model which id is {id}
	 * 
	 * @param id
	 * @return Model
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Model findByID(@PathVariable Integer id) {
		Filters filters = new Filters().add(new Filter("id", id)).add(new Filter("*", Wildcard.YES));

		return service.findUnique(filters);
	}


	/**
	 * Retrieves a Model using model as example 
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/filter", method = RequestMethod.POST)
	@ResponseBody
	public List<Model> findByExample(@RequestBody Model model){
		return service.find(model);
	}

	/**
	 * Creates a new record of type Model. A Model in json format must be sent in body request
	 * 
	 * @param model
	 * @return TRUE - Success / FALSE - Failed
	 */
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Boolean create(@RequestBody Model model) {

		try {
			service.create(model);
			return true;
		} catch (Exception e) {
			logger.error("Create exception for " +  model, e);
			return false;
		}

	}

	/**
	 * Creates a batch of new records of type Model. A list of Model in json format must be sent in body request
	 * 
	 * @param models - a list of Model
	 * @return
	 */
	@RequestMapping(value = "/batch", method = RequestMethod.POST)
	@ResponseBody
	public Boolean create(@RequestBody List<Model> models) {
		for (Model model : models) {
			if (create(model)) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Update a record of type Model. A Model or a list of Model in json format must be sent in body request
	 * 
	 * {id} is prompted at URL for matching to client layer, although it's not used
	 * 
	 * @param model
	 * @return TRUE - Success / FALSE - Failed
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	@ResponseBody
	public Boolean update(@RequestBody Model model) {

		try {
			service.update(model);
		} catch (Exception e) {
			logger.error("Update exception for " + model, e);
			return false;
		}
		return true;
	}

	/**
	 * Deletes a record of type Model which id is {id}
	 * 
	 * @param typeJson
	 * @return TRUE - Success / FALSE - Failed
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public Boolean remove(@PathVariable Integer id) {

		try {
			service.remove(id);
			return true;
		} catch (Exception e) {
			logger.error("Delete exception for " + id, e);
			return false;
		}
	}

}
