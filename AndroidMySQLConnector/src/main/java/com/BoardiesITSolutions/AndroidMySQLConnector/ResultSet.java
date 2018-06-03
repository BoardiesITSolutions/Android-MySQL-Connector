package com.BoardiesITSolutions.AndroidMySQLConnector;

import java.util.List;

public class ResultSet
{
    private List<ColumnDefinition> columnDefinitions;
    private List<MySQLRow> rows;
    private int currentRow = 0;

    public ResultSet (List<ColumnDefinition> columnDefinitions, List<MySQLRow> rows)
    {
        this.columnDefinitions = columnDefinitions;
        this.rows = rows;
    }

    /**
     * Return a list of Column Definitions
     * @return
     */
    public List<ColumnDefinition> getFields()
    {
        return this.columnDefinitions;
    }

    public MySQLRow getNextRow()
    {
        if (currentRow > this.rows.size()-1)
        {
            return null;
        }
        MySQLRow row = this.rows.get(this.currentRow);
        this.currentRow++;
        return row;
    }

    public int getNumRows()
    {
        return this.rows.size();
    }
}
