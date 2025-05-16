/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

import com.data.CandlesData;
import com.data.VTDictionary;
import xframe.framework.xDataInput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;


/**
 *
 * @author thuyps
 */
public class Master extends MasterBase{
    private static final int HEADER_SIZE_MASTER = 53;
    private static final int RECORD_SIZE_MASTER = 53;
    
    /**
     *
     * @param folder
     * @param intraday
     */
    public Master(String folder, boolean intraday, VTDictionary priceboard){
        super(folder, intraday, priceboard);
    }
    
    //  ./base/MASTER
    //  ./base/intraday/MASTER
    @Override
    protected String getFilepath(stRecord r){
        String filePath;
        if (isIntraday){
            filePath = String.format("%s/intraday/F%d.DAT", _folder, r.fileId);
        }
        else{
            filePath = String.format("%s/F%d.DAT", _folder, r.fileId);
        }
        
        return filePath;
    }
    @Override
    protected int HEADER_RECORD_SIZE(stRecord r){
        if (isIntraday){
            return r.recordSize>0?r.recordSize:32;
        }
        else{
            return r.recordSize>0?r.recordSize:28;
        }
    }
    @Override
    protected int RECORD_SIZE(stRecord r){
        if (isIntraday){
            return r.recordSize>0?r.recordSize:32;
        }
        else{
            return r.recordSize>0?r.recordSize:28;
        }
    } 

    @Override
    protected void loadMaster() {
        String filePath = String.format("%s/MASTER", _folder);
        if (isIntraday){
            filePath = String.format("%s/intraday/MASTER", _folder);
        }
        
        _records.clear();

        xDataInput di = xFileManager.readFile(filePath);
        try {
            // Bỏ qua phần header ở đầu file
            di.skip(HEADER_SIZE_MASTER);

            int length = di.available();
            int recordCount = length/RECORD_SIZE_MASTER;

            for (int i = 0; i < recordCount; i++) {    
                int off = HEADER_SIZE_MASTER+i*RECORD_SIZE_MASTER;

                //  fileId
                di.setCursor(off+0);
                int fileFNumber = di.readUByte();
                
                //  record length
                di.setCursor(off+3);
                int recordLength = di.readUByte();
                
                //  total fields
                di.setCursor(off+4);
                int totalFields = di.readByte();
                
                //  symbol
                di.setCursor(off+36);
                String symbol = di.readTerminatedString(14).trim();
                

                //  desc
                di.setCursor(off+7);
                String description = di.readTerminatedString(16);
                
                //  period
                di.setCursor(off+33);
                char period = (char)di.readByte();  //  'D'/'W'/'M'/'I'(1min)
                
                di.setCursor(off+34);
                int intraDayTimeBase = di.readUShortBig();
                                
                stRecord r = new stRecord(symbol, fileFNumber);
                r.recordSize = 0;
                r.recordSize = recordLength;
                r.totalField = totalFields;
                _records.add(r);
                _dictRecords.setValue(r, symbol);

                if (!symbol.isEmpty()) {
                    System.out.printf("Symbol: %s | Desc: %s | Group: %s | Market: %s | File ID: F%d.DAT%n",
                            symbol, description, 0, 0, fileFNumber);
                }
            }
            
            logSymbols("/", "FOREX base");

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
