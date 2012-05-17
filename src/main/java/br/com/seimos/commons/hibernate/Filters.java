package br.com.seimos.commons.hibernate;

import java.util.ArrayList;
import java.util.List;

public class Filters {

	private List<Filter> entries = new ArrayList<Filter>();
	
	public Filters(){
	}

	public Filters(Filter filter) {
		add(filter);
	}
	
	public Filters add(String filter){
		return add(new Filter(filter));
	}
	
	public Filters add(Filter filter) {
		entries.add(filter);
		return this;
	}
	
	public List<Filter> getEntries() {
		return entries;
	}

	public void setEntries(List<Filter> entries) {
		this.entries = entries;
	}

}
