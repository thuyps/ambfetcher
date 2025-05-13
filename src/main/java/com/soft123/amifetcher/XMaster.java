/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

import com.data.CandlesData;
import com.data.Priceboard;
import com.data.VTDictionary;
import java.util.ArrayList;
import xframe.framework.xDataInput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;

/**
 *
 * @author thuyps
 */
public class XMaster extends MasterBase{
    private static final int HEADER_SIZE_XMASTER = 150;
    private static final int RECORD_SIZE_XMASTER = 150;
    
    public XMaster(String folder, boolean intraday){
        super(folder, intraday);
    }
    
        //  ./base/MASTER
    //  ./base/intraday/MASTER
    @Override
    protected String getFilepath(stRecord r){
        String filePath;
        if (isIntraday){
            filePath = String.format("%s/intraday/F%d.MWD", _folder, r.fileId);
        }
        else{
            filePath = String.format("%s/F%d.MWD", _folder, r.fileId);
        }
        
        return filePath;
    }
    @Override
    protected int HEADER_RECORD_SIZE(stRecord r){
        return isIntraday?32:28;
    }
    @Override
    protected int RECORD_SIZE(stRecord r){
        return isIntraday?32:28;
    } 
    

    @Override
    protected void loadMaster() {
        String filePath = String.format("%s/XMASTER", _folder);
        if (isIntraday){
            filePath = String.format("%s/intraday/XMASTER", _folder);
        }
        
        _records.clear();

        xDataInput di = xFileManager.readFile(filePath);
        try {
            // Bỏ qua phần header ở đầu file
            di.skip(HEADER_SIZE_XMASTER);

            int length = di.available();
            int recordCount = length/RECORD_SIZE_XMASTER;

            for (int i = 0; i < recordCount; i++) {    
                di.resetCursor();
                di.skip(HEADER_SIZE_XMASTER+i*RECORD_SIZE_XMASTER);
                di.markCursor();
                
                byte onePad = di.readByte();
                String symbol = di.readTerminatedString(15);
                
                di.resetCursorToMarked();
                di.skip(16);
                
                String description = di.readTerminatedString(32);
                di.resetCursorToMarked();
                
                //  period
                di.skip(62);
                char period = (char)di.readByte();  //  'D'/'W'/'M'
                
                di.resetCursorToMarked();
                di.skip(65);
                int fileFNumber = di.readShortBig();
                
                di.resetCursorToMarked();
                di.skip(80);
                int startDate = di.readIntBig();
                di.resetCursorToMarked();
                di.skip(104);
                int lastDate = di.readIntBig();
                int firstTradeDate = di.readIntBig();
                int lastTradeDate = di.readIntBig();

                //String marketType = getMarketType(marketTypeCode);
                //  pad: 34
                
                stRecord r = new stRecord(symbol, fileFNumber);
                _records.add(r);

                if (!symbol.isEmpty()) {
                    System.out.printf("Symbol: %s | Desc: %s | Group: %s | Market: %s | File ID: F%d.MWD%n",
                            symbol, description, 0, 0, fileFNumber);
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
