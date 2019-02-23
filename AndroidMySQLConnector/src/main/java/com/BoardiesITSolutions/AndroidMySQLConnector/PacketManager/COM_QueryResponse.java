package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import android.util.Log;

import com.BoardiesITSolutions.AndroidMySQLConnector.ColumnDefinition;
import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import com.BoardiesITSolutions.AndroidMySQLConnector.Helpers;
import com.BoardiesITSolutions.AndroidMySQLConnector.MySQLRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class COM_QueryResponse extends BasePacket
{
    private List<ColumnDefinition> columnDefinitions;
    private List<MySQLRow> rows;

    public COM_QueryResponse(Connection mysqlConn)
    {
        super(mysqlConn);
        this.columnDefinitions = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.processPacketData();
    }

    private void processPacketData()
    {
        this.setPacketLength(this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractDataAsString(3)));
        this.setPacketSequenceNumber((byte)this.mysqlConn.getMysqlIO().extractDataAsString(1));

        int numberOfFields = (byte)this.mysqlConn.getMysqlIO().extractDataAsString(1);

        for (int currentColumnCount = 0; currentColumnCount < numberOfFields; currentColumnCount++)
        {
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(4);

            int catalogLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String catalog = this.mysqlConn.getMysqlIO().extractDataAsString(false, catalogLength);

            int databaseLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String database = this.mysqlConn.getMysqlIO().extractDataAsString(false, databaseLength);

            int tableLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String table = this.mysqlConn.getMysqlIO().extractDataAsString(false, tableLength);

            int origTableLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String origTable = this.mysqlConn.getMysqlIO().extractDataAsString(false, origTableLength);

            int columnNameLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String columnName = this.mysqlConn.getMysqlIO().extractDataAsString(false, columnNameLength);

            int origColumnNameLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String origColumnName = this.mysqlConn.getMysqlIO().extractDataAsString(false, origColumnNameLength);

            int nextLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();

            int characterSet = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractDataAsString(2));


            //int columnLength = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractDataAsString(4));

            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(4);

            //int columnLength = this.mysqlConn.getMysqlIO().getLenEncodedInt();

            //int columnType = (byte)this.mysqlConn.getMysqlIO().extractDataAsString(1);
            int columnType = this.mysqlConn.getMysqlIO().extractData(1)[0] & 0xff;

            Log.d("QueryResponse", "Column Name: " + columnName + " Type: " + columnType);
            /*if (columnType == -4)
            {
                columnType = 0xfc;
            }*/

            int flags = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractDataAsString(2));

            int decimals = (byte)this.mysqlConn.getMysqlIO().extractDataAsString(1);


            //2 Byte NULL fillers so shift on another 2
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(2);
            //break;

            this.columnDefinitions.add(new ColumnDefinition(catalog, database, table, columnName, characterSet,
                    columnType, flags, decimals));
        }

        //MySQL 5.1 appears to add an extra packet between the column definitions and the rows so we'll pass
        //it in case we need it - don't think we do though!
        if (this.mysqlConn.isConnectedVersionLessThan(5,5,60))
        {
            int packetLength = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractDataAsString(3));
            int packetNumber = (byte) this.mysqlConn.getMysqlIO().extractDataAsString(1);
            int eofMarker = (byte) this.mysqlConn.getMysqlIO().extractDataAsString(1);
            int warnings = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractDataAsString(2));
            int serverStatus = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractDataAsString(2));
        }
        do
        {
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(4);
            MySQLRow row = new MySQLRow();
            int packetType = this.mysqlConn.getMysqlIO().readCurrentByteWithoutShift();
            if (Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET)
            {
                //We've got an EOF packet so we're at the end or an OK packet so we've got everything we need
                break;
            }
            int currentColumn = 0;
            for (; currentColumn < numberOfFields; currentColumn++)
            {
                ColumnDefinition columnDefinition = columnDefinitions.get(currentColumn);
                //Log.d("COMQueryResp", "Getting data for " + columnDefinition.getColumnName() + " with type: " + columnDefinition.getColumnType().toString());
                int lengthOfValue = this.mysqlConn.getMysqlIO().getLenEncodedInt();

                //Believe if the length of value is less than 0 (-5) then the value is NULL
                Object value = null;
                if (lengthOfValue > 0)
                {
                    if (columnDefinition.getColumnType() == ColumnDefinition.ColumnType.BLOB)
                    {
                        value = this.mysqlConn.getMysqlIO().extractData(lengthOfValue);
                    }
                    else
                    {
                        value = (String)this.mysqlConn.getMysqlIO().extractDataAsString(false, lengthOfValue);
                    }

                }
                else
                {
                    value = null; //If we got less than 0, it should be a -1 value which means it was a NULL value
                }

                row.addRowValue(columnDefinition, value);
            }
            this.rows.add(row);

        }while(true);
    }

    public List<ColumnDefinition> getColumnDefinitions()
    {
        return this.columnDefinitions;
    }

    public List<MySQLRow> getRows()
    {
        return this.rows;
    }

    /**
     * This isn't neede for COM_Query Response, so don't call it
     * @return
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    @Override
    public ByteArrayOutputStream getPacketData() throws IOException, UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }
}
