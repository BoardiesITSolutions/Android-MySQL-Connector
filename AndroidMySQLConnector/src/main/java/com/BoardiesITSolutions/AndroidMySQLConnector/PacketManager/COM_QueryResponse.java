package com.BoardiesITSolutions.AndroidMySQLConnector.PacketManager;

import android.util.Log;

import androidx.appcompat.widget.ThemedSpinnerAdapter;

import com.BoardiesITSolutions.AndroidMySQLConnector.ColumnDefinition;
import com.BoardiesITSolutions.AndroidMySQLConnector.Connection;
import com.BoardiesITSolutions.AndroidMySQLConnector.Exceptions.MySQLException;
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
        this.setPacketLength(this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractData(3)));
        this.setPacketSequenceNumber((byte)this.mysqlConn.getMysqlIO().extractData(1));

        Log.d("Query Response", "Packet Length: " + this.getPacketLength() + " Packet Sequence: " + this.getPacketSequenceNumber());

        int numberOfFields = (byte)this.mysqlConn.getMysqlIO().extractData(1);

        if (numberOfFields == 0)
        {
            Log.d("COM_QueryResponse","Number of fields is 0, so treating as OK packet");
            return;
        }

        Log.d("COM_QueryResponse", "Number of Fields: " + numberOfFields);

        for (int currentColumnCount = 0; currentColumnCount < numberOfFields; currentColumnCount++)
        {
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(4);
            int catalogLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String catalog = this.mysqlConn.getMysqlIO().extractData(false, catalogLength);
            int databaseLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String database = this.mysqlConn.getMysqlIO().extractData(false, databaseLength);

            int tableLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String table = this.mysqlConn.getMysqlIO().extractData(false, tableLength);
            int origTableLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String origTable = this.mysqlConn.getMysqlIO().extractData(false, origTableLength);
            int columnNameLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String columnName = this.mysqlConn.getMysqlIO().extractData(false, columnNameLength);
            int origColumnNameLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            String origColumnName = this.mysqlConn.getMysqlIO().extractData(false, origColumnNameLength);
            int nextLength = (byte)this.mysqlConn.getMysqlIO().getLenEncodedInt();
            int characterSet = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractData(2));
            int columnLength = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractData(4));
            int columnType = (byte)this.mysqlConn.getMysqlIO().extractData(1) & 0xff;
            int flags = this.mysqlConn.getMysqlIO().fromByteArray((byte[])this.mysqlConn.getMysqlIO().extractData(2));
            int decimals = (byte)this.mysqlConn.getMysqlIO().extractData(1);

            //2 Byte NULL fillers so shift on another 2
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(2);
            //break;
            this.columnDefinitions.add(new ColumnDefinition(catalog, database, table, columnName, characterSet,
                    columnType, flags, decimals));
        }

        //MySQL 5.1 appears to add an extra packet between the column definitions and the rows so we'll pass
        //it in case we need it - don't think we do though!
        if (this.mysqlConn.isConnectedVersionLessThan(5,5,60) && !this.mysqlConn.isMariaDB())
        {
            Log.d("COMQueryResponse", "Reading extra un-used data");
            int packetLength = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractData(3));
            int packetNumber = (byte) this.mysqlConn.getMysqlIO().extractData(1);
            int eofMarker = (byte) this.mysqlConn.getMysqlIO().extractData(1);
            int warnings = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractData(2));
            int serverStatus = this.mysqlConn.getMysqlIO().fromByteArray((byte[]) this.mysqlConn.getMysqlIO().extractData(2));
        }

        int timesProcessed = 0;
        boolean finishedProcessingColumns = false;
        do
        {
            if (finishedProcessingColumns)
            {
                break;
            }
            int packetType = this.mysqlConn.getMysqlIO().readCurrentByteWithoutShift() & 0xff;
            if (Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET ||
                    Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET)
            {
                if (Helpers.checkIfRealEOFPacket(this.mysqlConn.getMysqlIO()))
                {
                    Log.d("COM_QueryResponse", "Received EOF packet on processing column definition");
                    break;
                }
            }

            if ((this.mysqlConn.getMysqlIO().getCurrentBytesRead() + 4) >= this.mysqlConn.getMysqlIO().getSocketDataLength())
            {
                break;
            }
            this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(4);

            //Check we don't have an error packet
            if (Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_ERROR_PACKET)
            {
                try {
                    MySQLErrorPacket errorPacket = new MySQLErrorPacket(this.mysqlConn);
                    throw new MySQLException(errorPacket.getErrorMsg(), errorPacket.getErrorCode(), errorPacket.getSqlState());
                }
                catch (Exception ex)
                {
                    Log.e("COM_QueryResponse", ex.toString());
                }
                break;
            }




            MySQLRow row = new MySQLRow();

            /*if (Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET
                || Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET)
            {
                Log.d("COM_QueryResponse", "Got EOF or OK Packet. Breaking from loop");
                //We've got an EOF packet so we're at the end or an OK packet so we've got everything we need
                break;
            }*/

            int currentColumn = 0;
            for (; currentColumn < numberOfFields; currentColumn++)
            {
                packetType = this.mysqlConn.getMysqlIO().readCurrentByteWithoutShift() & 0xff;
                if ((numberOfFields == currentColumn-1) &&  Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET ||
                        Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET)
                {

                    if ((this.mysqlConn.getMysqlIO().getSocketDataLength() - this.mysqlConn.getMysqlIO().getCurrentBytesRead()) < 9)
                    {
                        //We've got an EOF packet and the remaining data in the socket data is less than 9 so this is a true 0xFE.
                        //You sometimes get an 0xFE packet when its actually a len encoded integer. As this is a true EOF packet
                        //we can break from the loop as we have everything we need

                        //We've created a row object in case we needed it, but as we've detected as an EOF packet,
                        //set the row to null so it doesn't get added to to the resultset.
                        if (row.getSizeOfHash() == 0)
                        {
                            row = null;
                            finishedProcessingColumns = true;
                        }
                        break;
                    }
                    //We've got an EOF packet but there's actually more data so not a true EOF packet so shift 9 bytes
                    this.mysqlConn.getMysqlIO().shiftCurrentBytePosition(9);

                    //Now that we've shifted past the EOF, see if this packet type is now an EOF as if there are no results you can
                    //get two EOF packets together
                    packetType = this.mysqlConn.getMysqlIO().readCurrentByteWithoutShift() & 0xff;
                    if (Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_OK_PACKET ||
                        Helpers.getMySQLPacketTypeFromIntWithoutShift(packetType) == Helpers.MYSQL_PACKET_TYPE.MYSQL_EOF_PACKET)
                    {
                        if ((this.mysqlConn.getMysqlIO().getSocketDataLength() - this.mysqlConn.getMysqlIO().getCurrentBytesRead()) < 9)
                        {
                            if (row.getSizeOfHash() == 0)
                            {
                                row = null;
                                finishedProcessingColumns = true;
                            }
                            break;
                        }
                    }
                }
                ColumnDefinition columnDefinition = columnDefinitions.get(currentColumn);
                int lengthOfValue = this.mysqlConn.getMysqlIO().getLenEncodedInt();

                //Believe if the length of value is less than 0 (-5) then the value is NULL
                String value = null;
                if (lengthOfValue > 0)
                {
                    try {
                        value = this.mysqlConn.getMysqlIO().extractData(false, lengthOfValue);
                    }
                    catch (Exception ex)
                    {
                        Log.e("COM_QueryResponse", "About to pop");
                        ex.printStackTrace();
                    }
                }
                else
                {
                    value = null; //If we got less than 0, it should be a -1 value which means it was a NULL value
                }

                row.addRowValue(columnDefinition, value);
            }
            //Check we haven't got a blank row as we got an EOF packet

            if (row != null) {
                this.rows.add(row);
            }
            timesProcessed++;
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
