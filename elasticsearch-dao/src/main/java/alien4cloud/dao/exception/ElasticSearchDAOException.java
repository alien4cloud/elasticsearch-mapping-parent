package alien4cloud.dao.exception;

public class ElasticSearchDAOException extends RuntimeException {

    public ElasticSearchDAOException(String message) {
        super(message);
    }

    public ElasticSearchDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
