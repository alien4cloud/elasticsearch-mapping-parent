package org.elasticsearch.mapping;

/**
 * Abstract class for a filter builder helper (defines hashcode and equals based on the esFieldName as only a single filter is allowed on a field by our
 * framework).
 * 
 * @author luc boutier
 */
public abstract class AbstractFilterBuilderHelper implements IFilterBuilderHelper {
    private final String nestedPath;
    private final String filterPath;

    /**
     * Create a filter for the given field.
     * 
     * @param nestedPath The path to the nested object if any.
     * @param filterPath The path to the field to filter.
     */
    public AbstractFilterBuilderHelper(final String nestedPath, final String filterPath) {
        this.nestedPath = nestedPath;
        this.filterPath = filterPath;
    }

    public String getFilterPath() {
        return filterPath;
    }

    @Override
    public boolean isNested() {
        return nestedPath != null;
    }

    public String getNestedPath() {
        return nestedPath;
    }

    public String getEsFieldName() {
        return nestedPath + "." + filterPath;
        // return nestedPath == null ? filterPath : nestedPath + "." + filterPath;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEsFieldName() == null) ? 0 : getEsFieldName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractFilterBuilderHelper other = (AbstractFilterBuilderHelper) obj;
        if (getEsFieldName() == null) {
            if (other.getEsFieldName() != null)
                return false;
        } else if (!getEsFieldName().equals(other.getEsFieldName()))
            return false;
        return true;
    }
}