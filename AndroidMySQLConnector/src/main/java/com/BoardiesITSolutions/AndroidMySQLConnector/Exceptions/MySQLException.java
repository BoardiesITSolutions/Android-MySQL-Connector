package com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions;

public class MySQLException extends Exception
{
    private String message;
    private int errorCode;
    private int sqlState;

    public MySQLException(String message, int sqlErrorCode, int sqlState)
    {
        super(message);
        this.message = message;
        this.errorCode = sqlErrorCode;
        this.sqlState = sqlState;
    }

    public String getMessage()
    {
        return this.message;
    }

    public int getSQLErrorCode()
    {
        return this.errorCode;
    }

    public int getSQLState()
    {
        return this.sqlState;
    }
}
