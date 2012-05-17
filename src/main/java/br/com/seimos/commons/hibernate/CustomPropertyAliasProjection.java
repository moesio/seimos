package br.com.seimos.commons.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.AliasedProjection;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Projections;

/**
 * Customized projection to map a property with an alias that is intended to work-around HHH-817. This works in a basic situation that I've tried, but I can't guarantee it works in
 * complex queries.
 * 
 * This causes the alias to be bypassed in the where clause so the original column name is used. Some DB's don't support aliases in the where clause.
 * 
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-817
 * 
 * @author Chris Federowicz
 */
public class CustomPropertyAliasProjection extends AliasedProjection
{
    /** Stores the property name being aliased. */
    private String propertyName;
    /** Stores the column name for the property in the PropertyProjection. */
    private Criteria criteria;
    private CriteriaQuery criteriaQuery;
    /** Default serialization ID. */
    private static final long serialVersionUID = 1L;

    /**
     * @param propertyName
     *            the property name
     * @param alias
     *            alias of the property
     */
    public CustomPropertyAliasProjection(String propertyName, String alias)
    {
        super(Projections.property(propertyName), alias);
        this.propertyName = propertyName;
    }

    /**
     * @see org.hibernate.criterion.AliasedProjection#toSqlString(org.hibernate.Criteria, int, org.hibernate.criterion.CriteriaQuery)
     */
    public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException
    {
        String ret = super.toSqlString(criteria, position, criteriaQuery);

        if (this.criteriaQuery == null) this.criteriaQuery = criteriaQuery;
        if (this.criteria == null) this.criteria = criteria;

        return ret;
    }

    /**
     * Override to use the property's column if we have it (which we should). Hack alert: This assumes that toSqlString gets called beforehand, which seems to be the case.
     * 
     * @see org.hibernate.criterion.AliasedProjection#getColumnAliases(java.lang.String, int)
     */
    public String[] getColumnAliases(String alias, int loc)
    {
        String ret = criteriaQuery.getColumn(criteria, alias);
        return new String[] { ret };
        // return propertyColumn != null ? new String[] { propertyColumn } : super.getColumnAliases(alias, loc);
    }

    public String getPropertyName()
    {
        return propertyName;
    }
}
