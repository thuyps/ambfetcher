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
public class XMaster {
    private static final int HEADER_SIZE_XMASTER = 150;
    private static final int RECORD_SIZE_XMASTER = 150;
    
    private static final int HEADER_SIZE_EMASTER = 192;
    private static final int RECORD_SIZE_EMASTER = 192;
    
    String _folder;
    public boolean isIntraday;
    ArrayList<stRecord> _records = new ArrayList<>();
    public XMaster(String folder, boolean intraday){
        isIntraday = intraday;
        _folder = folder;

        loadXMaster();
        
        if (intraday){
            dictPriceboard = new VTDictionary();            
            
            for (stRecord r: _records){
                updatePriceboard(r.symbol);
            }
        }
    }
    

    void loadXMaster() {
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
    
    public static float convertMBFToFloat(byte[] p, int off) {
        int valtemp = (p[off] & 0xFF) | 
                     ((p[off+1] & 0xFF) << 8) | 
                     ((p[off+2] & 0xFF) << 16) | 
                     ((p[off+3] & 0xFF) << 24);

        long unsignedValue = valtemp & 0xFFFFFFFFL;

        // Thực hiện chuyển đổi MBF sang IEEE giống như code gốc
        valtemp = (int)(((unsignedValue - 0x02000000) & 0xFF000000) >> 1) |
                  ((valtemp & 0x00800000) << 8) |
                  (valtemp & 0x007FFFFF);

        // Chuyển đổi bit pattern sang float
        return Float.intBitsToFloat((int)valtemp);
    }
    
    public static int convertSerialDateToLocalDate(float serialDate) {
        // Chuyển float thành int (bỏ phần thập phân)
        int dateInt = (int) serialDate;
        
        // Chuyển số thành chuỗi và đảm bảo có đủ 7 chữ số (thêm số 0 ở đầu nếu cần)
        String dateStr = String.format("%07d", dateInt);
        
        // Tách các thành phần ngày tháng năm
        int year = Integer.parseInt(dateStr.substring(0, 3)) + 1900; // 110 -> 2010
        int month = Integer.parseInt(dateStr.substring(3, 5));
        int day = Integer.parseInt(dateStr.substring(5, 7));
        
        return (year << 16)|(month << 8)|day;
    }
    
    private CandlesData readDataD(stRecord r, int startDate) {
        final int MWD_HEADER_SIZE = 28;
        final int MWD_RECORD_SIZE = 28;

        CandlesData share = null;
        String filePath = String.format("%s/F%d.MWD", _folder, r.fileId);

        xDataInput di = xFileManager.readFile(filePath);
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record
            
            di.resetCursor();
            di.skip(MWD_HEADER_SIZE);
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);

            int length = di.available();
            int recordCount = length/MWD_RECORD_SIZE;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            

            byte[] p = new byte[MWD_RECORD_SIZE];
            for (int i = 0; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(MWD_HEADER_SIZE + i*MWD_RECORD_SIZE);
                
                di.read(p, 0, MWD_RECORD_SIZE);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                
                if (date < startDate){
                    continue;
                }
                
                float open = convertMBFToFloat(p, 4);
                float hi = convertMBFToFloat(p, 8);
                float low = convertMBFToFloat(p, 12);
                float close = convertMBFToFloat(p, 16);
                int vol = (int)(convertMBFToFloat(p, 20)/100);
                
                //String s = String.format("%s: %.2f/%.2f/%.2f/%.2f; V=%d", xUtils.dateIntToStringYYYYMMDD(date), open, hi, low, close, vol);
                //xUtils.trace(s);
                share.addCandle(close, open, hi, low, date, vol);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        return share;
    }
    
    private void readDataD(stRecord r, int candleCnt, CandlesData share) {
        final int MWD_HEADER_SIZE = 28;
        final int MWD_RECORD_SIZE = 28;

        share.clear();
        String filePath = String.format("%s/F%d.MWD", _folder, r.fileId);

        xDataInput di = xFileManager.readFile(filePath);
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record
            
            di.resetCursor();
            di.skip(MWD_HEADER_SIZE);
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);

            int length = di.available();
            int recordCount = length/MWD_RECORD_SIZE;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            

            byte[] p = new byte[MWD_RECORD_SIZE];
            
            int begin = totalRecord - candleCnt;
            if (begin < 0){
                begin = 0;
            }
            
            for (int i = begin; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(MWD_HEADER_SIZE + i*MWD_RECORD_SIZE);
                
                di.read(p, 0, MWD_RECORD_SIZE);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                
                float open = convertMBFToFloat(p, 4);
                float hi = convertMBFToFloat(p, 8);
                float low = convertMBFToFloat(p, 12);
                float close = convertMBFToFloat(p, 16);
                int vol = (int)(convertMBFToFloat(p, 20)/100);
                
                share.addCandle(close, open, hi, low, date, vol);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    private CandlesData readDataIntraday(stRecord r, int startDate, int startTime) {
        final int MWD_HEADER_SIZE_INTRADAY = 32;
        final int MWD_RECORD_SIZE_INTRADAY = 32;

        String filePath = String.format("%s/intraday/F%d.MWD", _folder, r.fileId);

        xDataInput di = xFileManager.readFile(filePath);
        CandlesData share = null;
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(MWD_HEADER_SIZE_INTRADAY);

            int length = di.available();
            int recordCount = length/MWD_RECORD_SIZE_INTRADAY;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[MWD_RECORD_SIZE_INTRADAY];
            for (int i = 0; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(MWD_HEADER_SIZE_INTRADAY + i*MWD_RECORD_SIZE_INTRADAY);
                
                di.read(p, 0, MWD_RECORD_SIZE_INTRADAY);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                
                int t = (int)convertMBFToFloat(p, 4);
                int ss = t%100;
                int mm = (int)(t/100);
                mm = mm % 100;
                int hh = t/10000;
                int time = (hh << 16) | (mm << 8) | ss;
                
                float open = convertMBFToFloat(p, 4+4);
                float hi = convertMBFToFloat(p, 8+4);
                float low = convertMBFToFloat(p, 12+4);
                float close = convertMBFToFloat(p, 16+4);
                int vol = (int)convertMBFToFloat(p, 20+4);
                
                if (date < startDate){
                    continue;
                }
                if (date == startDate){
                    if (time < startTime){
                        continue;
                    }
                }
                
                int packedDate = xUtils.dateTimeToPackaged(date, time);
                
                share.addCandle(close, open, hi, low, packedDate, vol);

            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        return share;
    }
    
    private void readDataIntraday(stRecord r, int candleCnt, CandlesData share) {
        final int MWD_HEADER_SIZE_INTRADAY = 32;
        final int MWD_RECORD_SIZE_INTRADAY = 32;

        String filePath = String.format("%s/intraday/F%d.MWD", _folder, r.fileId);

        xDataInput di = xFileManager.readFile(filePath);
        share.clear();
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(MWD_HEADER_SIZE_INTRADAY);

            int length = di.available();
            int recordCount = length/MWD_RECORD_SIZE_INTRADAY;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[MWD_RECORD_SIZE_INTRADAY];
            
            int begin = totalRecord - candleCnt;
            if (begin < 0){
                begin = 0;
            }
            
            for (int i = begin; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(MWD_HEADER_SIZE_INTRADAY + i*MWD_RECORD_SIZE_INTRADAY);
                
                di.read(p, 0, MWD_RECORD_SIZE_INTRADAY);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                
                int t = (int)convertMBFToFloat(p, 4);
                int ss = t%100;
                int mm = (int)(t/100);
                mm = mm % 100;
                int hh = t/10000;
                int time = (hh << 16) | (mm << 8) | ss;
                
                float open = convertMBFToFloat(p, 4+4);
                float hi = convertMBFToFloat(p, 8+4);
                float low = convertMBFToFloat(p, 12+4);
                float close = convertMBFToFloat(p, 16+4);
                int vol = (int)convertMBFToFloat(p, 20+4);
                
                
                int packedDate = xUtils.dateTimeToPackaged(date, time);
                
                share.addCandle(close, open, hi, low, packedDate, vol);

            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    private CandlesData readDataIntraday(Priceboard ps, boolean lastPrice) {
        final int MWD_HEADER_SIZE_INTRADAY = 32;
        final int MWD_RECORD_SIZE_INTRADAY = 32;

        String filePath = String.format("%s/intraday/F%d.MWD", _folder, ps._fileId);

        xDataInput di = xFileManager.readFile(filePath);
        CandlesData share = sharePriceboard;
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(MWD_HEADER_SIZE_INTRADAY);

            int length = di.available();
            int recordCount = length/MWD_RECORD_SIZE_INTRADAY;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            //share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);
            share.isIntraday = this.isIntraday;
            share.clear();
            
            int lastDate = 0;

            byte[] p = new byte[MWD_RECORD_SIZE_INTRADAY];
            for (int i = totalRecord-1; i >= 0; i--) {    
                di.resetCursor();
                di.skip(MWD_HEADER_SIZE_INTRADAY + i*MWD_RECORD_SIZE_INTRADAY);
                
                di.read(p, 0, MWD_RECORD_SIZE_INTRADAY);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                if (lastDate == 0){
                    lastDate = date;
                }

                int t = (int)convertMBFToFloat(p, 4);
                int ss = t%100;
                int mm = (int)(t/100);
                mm = mm % 100;
                int hh = t/10000;
                int time = (hh << 16) | (mm << 8) | ss;
                
                float open = convertMBFToFloat(p, 4+4);
                float hi = convertMBFToFloat(p, 8+4);
                float low = convertMBFToFloat(p, 12+4);
                float close = convertMBFToFloat(p, 16+4);
                int vol = (int)convertMBFToFloat(p, 20+4);
                
                int packedDate = xUtils.dateTimeToPackaged(date, time);
                
                share.addCandle(close, open, hi, low, packedDate, vol);
                
                if (date < lastDate){
                    break;
                }         
                
                if (lastPrice){
                    break;
                }
            }
            
            share.reverse();

        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        return share;
    }
    
    stRecord getRecord(String symbol){
        for (stRecord r: _records){
            if (r.symbol.compareToIgnoreCase(symbol) == 0){
                return r;
            }
        }
        
        return null;
    }
    
    public ArrayList<stRecord> getRecords(){
        return _records;
    }    
    
    public CandlesData readData(int shareId, String symbol, int marketId, 
            int startDate, int startTime)
    {
        stRecord r = getRecord(symbol);
        if (r == null){
            return null;
        }
        
        r.shareId = shareId;
        r.marketId = marketId;
        
        if (isIntraday){
            return readDataIntraday(r, startDate, startTime);
        }
        else{
            return readDataD(r, startDate);
        }
    }
    
    public void readData(int shareId, String symbol, int marketId, 
            int candleCnt, CandlesData share)
    {
        share.clear();
        stRecord r = getRecord(symbol);
        if (r == null){
            return;
        }
        
        r.shareId = shareId;
        r.marketId = marketId;
        
        if (isIntraday){
            readDataIntraday(r, candleCnt, share);
        }
        else{
            readDataD(r, candleCnt, share);
        }
    }    
    
    CandlesData sharePriceboard;
    VTDictionary dictPriceboard;
    void updatePriceboard(String symbol){
        if (!isIntraday){
            return;
        }
        
        if (sharePriceboard == null){
            sharePriceboard = new CandlesData(0, symbol, 0, 24*3600);
        }

        Priceboard ps = (Priceboard)dictPriceboard.objectForKeyO(symbol);
        if (ps == null){
            ps = new Priceboard(symbol);
            
            stRecord r = getRecord(symbol);
            ps._fileId = r.fileId;
            
            dictPriceboard.setValue(ps, symbol);
            dictPriceboard.setValue(ps, ""+r.fileId);
        }
        
        long now = System.currentTimeMillis();
        long elapsed = (now - ps._timeUpdate)/1000;
        long elapsedOpen = (now - ps._timeUpdateOpen)/1000;
        sharePriceboard.clear();
        if (elapsedOpen > 3600){
            //  reupdate 
            readDataIntraday(ps, false);
            
            int candles = sharePriceboard.getCandleCnt();
            ps.reset();
            ps._timeUpdate = now;
            ps._timeUpdateOpen = now;
            for (int i = 0; i < candles; i++){
                if (i == 0){
                    ps._prevClose = sharePriceboard.close[0];
                    continue;
                }
                //=====================
                
                int v = sharePriceboard.volume[i];
                ps._volume += v;
                if (ps._open == 0 && sharePriceboard.open[i] > 0){
                    ps._timeUpdateOpen = now;
                    ps._open = sharePriceboard.open[i];
                }
                ps._price = sharePriceboard.close[i];
                if (ps._highest == 0){
                    ps._highest = ps._price;
                }
                if (ps._price > ps._highest){
                    ps._highest = ps._price;
                }
                if (ps._lowest == 0){
                    ps._lowest = ps._price;
                }
                if (ps._price < ps._lowest){
                    ps._lowest = ps._price;
                }
                
                int date = sharePriceboard.date[i];
                ps._date = xUtils.dateFromPackagedDate(date);
                ps._time = xUtils.timeFromPackagedDate(date);
            }
        }
        else{
            readDataIntraday(ps, true);

            int candles = sharePriceboard.getCandleCnt();

            ps._timeUpdate = now;

            if (candles == 0){
                int v = sharePriceboard.volume[0];
                ps._volume += v;
                if (ps._open == 0 && sharePriceboard.open[0] > 0){
                    ps._timeUpdateOpen = now;
                    ps._open = sharePriceboard.open[0];
                }
                ps._price = sharePriceboard.close[0];
                if (ps._highest == 0){
                    ps._highest = ps._price;
                }
                if (ps._price > ps._highest){
                    ps._highest = ps._price;
                }
                if (ps._lowest == 0){
                    ps._lowest = ps._price;
                }
                if (ps._price < ps._lowest){
                    ps._lowest = ps._price;
                }
                
                int date = sharePriceboard.date[0];
                int today = xUtils.dateFromPackagedDate(date);
                if (today > ps._date){
                    //  new day
                    ps.reset();
                    ps._timeUpdate = 0;
                    ps._timeUpdateOpen = 0;
                    updatePriceboard(symbol);
                }
                else{
                    ps._date = xUtils.dateFromPackagedDate(date);                    
                    ps._time = xUtils.timeFromPackagedDate(date);
                }
            }
        }
        
        
    }
    public ArrayList<Priceboard> getPriceboard(ArrayList<String> arrSymbol, ArrayList<Priceboard> arrPriceboard)
    {
        if (arrSymbol == null){
            for (stRecord r: _records){
                Priceboard ps = (Priceboard)dictPriceboard.objectForKeyO(r.symbol);
                if (ps != null){
                    arrPriceboard.add(ps);
                }
            }
        }
        else{
            for (String sb: arrSymbol){
                Priceboard ps = (Priceboard)dictPriceboard.objectForKeyO(sb);
                if (ps != null){
                    arrPriceboard.add(ps);
                }
            }
        }

        return arrPriceboard;
    }
}
