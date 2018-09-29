package com.lungesoft.custom.orm.exeption;

public class OrmMetadataException extends RuntimeException {

    public OrmMetadataException() {
        super();
    }

    public OrmMetadataException(String message) {
        super(message);
    }

    public OrmMetadataException(Throwable cause) {
        super(cause);
    }

    public OrmMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
