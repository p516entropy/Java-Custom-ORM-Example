package com.lungesoft.custom.orm.exeption;

public class OrmManagerException extends RuntimeException {

    public OrmManagerException() {
        super();
    }

    public OrmManagerException(String message) {
        super(message);
    }

    public OrmManagerException(Throwable cause) {
        super(cause);
    }

    public OrmManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
