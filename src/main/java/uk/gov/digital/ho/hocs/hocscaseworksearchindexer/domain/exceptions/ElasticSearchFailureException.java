package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.exceptions;

public class ElasticSearchFailureException extends RuntimeException {

    public ElasticSearchFailureException(String message, Throwable cause) {
        super(message, cause);
    }

}
