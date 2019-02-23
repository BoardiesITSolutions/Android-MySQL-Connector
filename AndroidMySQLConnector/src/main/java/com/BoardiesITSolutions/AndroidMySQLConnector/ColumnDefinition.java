package com.BoardiesITSolutions.AndroidMySQLConnector;

public class ColumnDefinition
{
    public enum ColumnType {
        DECIMAL, TINY, SHORT, LONG, FLOAT, DOUBLE, NULL, TIMESTAMP, LONGLONG, INT24, DATE, TIME, DATETIME, YEAR,
        NEWDATE, VARCHAR, BIT, TIMESTAMP2, DATETIME2, TIME2, NEWDECIMAL, ENUM, SET, TINY_BLOB, MEDIUM_BLOB,
        LONG_BLOG, BLOB, VAR_STRING, STRING, GEOMETRY
    }

    /**
     * Column Flags Bitmask
     */
    public static final int FLAG_NOT_NULL = 0x01;
    public static final int FLAG_PRIMARY_KEY = 0x02;
    public static final int FLAG_UNIQUE_KEY = 0x04;
    public static final int FLAG_MULTIPLE_KEY = 0x08;
    public static final int FLAG_BLOG_SET = 0x10;
    public static final int FLAG_UNSIGNED = 0x20;
    public static final int FLAG_ZERO_FILL = 0x40;
    public static final int FLAG_BINARY = 0x80;
    public static final int FLAG_ENUM = 0x100;
    public static final int FLAG_AUTO_INCREMENT = 0x200;
    public static final int FLAG_TIMESTAMP = 0x400;
    public static final int FLAG_SET = 0x800;

    private String catalog;
    private String database;
    private String table;
    private String column;
    private int characterSet;
    private ColumnType columnType;
    private int flags;
    private int decimals;

    public ColumnDefinition(String catalog, String database, String table, String column, int characterSet,
                            int columnType, int flags, int decimals)
    {
        this.catalog = catalog;
        this.database = database;
        this.table = table;
        this.column = column;
        this.characterSet = characterSet;
        this.setColumnType(columnType);
        this.flags = flags;
        this.decimals = decimals;
    }

    public String getDatabase()
    {
        return this.database;
    }
    public String getTable()
    {
        return this.table;
    }
    public String getColumnName()
    {
        return this.column;
    }
    public ColumnType getColumnType()
    {
        return this.columnType;
    }
    public int getFlags()
    {
        return this.flags;
    }
    public int getDecimals()
    {
        return this.decimals;
    }

    private void setColumnType(int columnType)
    {
        switch (columnType)
        {
            case 0x00:
                this.columnType = ColumnType.DECIMAL;
                break;
            case 0x01:
                this.columnType = ColumnType.TINY;
                break;
            case 0x02:
                this.columnType = ColumnType.SHORT;
                break;
            case 0x03:
                this.columnType = ColumnType.LONG;
                break;
            case 0x04:
                this.columnType = ColumnType.FLOAT;
                break;
            case 0x05:
                this.columnType = ColumnType.DOUBLE;
                break;
            case 0x06:
                this.columnType = ColumnType.NULL;
                break;
            case 0x07:
                this.columnType = ColumnType.TIMESTAMP;
                break;
            case 0x08:
                this.columnType = ColumnType.LONGLONG;
                break;
            case 0x09:
                this.columnType = ColumnType.INT24;
                break;
            case 0x0a:
                this.columnType = ColumnType.DATE;
                break;
            case 0x0b:
                this.columnType = ColumnType.TIME;
                break;
            case 0x0c:
                this.columnType = ColumnType.DATETIME;
                break;
            case 0x0d:
                this.columnType = ColumnType.YEAR;
                break;
            case 0x0e:
                this.columnType = ColumnType.NEWDATE;
                break;
            case 0x0f:
                this.columnType = ColumnType.VARCHAR;
                break;
            case 0x10:
                this.columnType = ColumnType.BIT;
                break;
            case 0x11:
                this.columnType = ColumnType.TIMESTAMP2;
                break;
            case 0x12:
                this.columnType = ColumnType.DATETIME2;
                break;
            case 0x13:
                this.columnType = ColumnType.TIME2;
                break;
            case 0xf6:
                this.columnType = ColumnType.NEWDECIMAL;
                break;
            case 0xf7:
                this.columnType = ColumnType.ENUM;
                break;
            case 0xf8:
                this.columnType = ColumnType.SET;
                break;
            case 0xf9:
                this.columnType = ColumnType.TINY_BLOB;
                break;
            case 0xfa:
                this.columnType = ColumnType.MEDIUM_BLOB;
                break;
            case 0xfb:
                this.columnType = ColumnType.LONG_BLOG;
                break;
            case 0xfc:
                this.columnType = ColumnType.BLOB;
                break;
            case 0xfd:
                this.columnType = ColumnType.VAR_STRING;
                break;
            case 0xfe:
                this.columnType = ColumnType.STRING;
                break;
            case 0xff:
                this.columnType = ColumnType.GEOMETRY;
                break;

        }
    }

    public boolean isPrimaryKey()
    {
        return (this.flags & FLAG_PRIMARY_KEY) == FLAG_PRIMARY_KEY;
    }

}