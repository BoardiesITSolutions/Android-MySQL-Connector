package com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions;

public class InvalidSQLPacketException extends Exception
{
    public InvalidSQLPacketException(String message)
    {
        super(message);
        this.printStackTrace();
    }
}
