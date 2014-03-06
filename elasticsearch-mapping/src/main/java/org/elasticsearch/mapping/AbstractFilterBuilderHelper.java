package org.elasticsearch.mapping;

/**
 * Abstract class for a filter builder helper (defines hashcode and equals based on the esFieldName as only a single filter is allowed on a field by our
 * framework).
 * 
 * @author luc boutier
 */
public abstract class AbstractFilterBuilderHelper {
    private final String esFieldName;

    public AbstractFilterBuilderHelper(final String esFieldName) {
        this.esFieldName = esFieldName;
    }

    public String getEsFieldName() {
        return esFieldName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((esFieldName == null) ? 0 : esFieldName.hashCode());
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
        if (esFieldName == null) {
            if (other.esFieldName != null)
                return false;
        } else if (!esFieldName.equals(other.esFieldName))
            return false;
        return true;
    }
}