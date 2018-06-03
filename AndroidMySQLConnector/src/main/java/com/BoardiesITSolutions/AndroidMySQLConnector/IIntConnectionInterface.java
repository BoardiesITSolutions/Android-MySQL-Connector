package com.BoardiesITSolutions.AndroidMySQLConnector;

import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLConnException;

import java.io.IOException;

public interface IIntConnectionInterface
{
    void socketDataSent();
    void handleException(MySQLConnException ex);
}
