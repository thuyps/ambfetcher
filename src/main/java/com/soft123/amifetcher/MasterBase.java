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
public abstract class MasterBase {
    public static final int HEADER_SIZE_MASTER = 53;
    public static final int RECORD_SIZE_MASTER = 53;
    
    
    protected String _folder;
    protected  boolean isIntraday;
    protected ArrayList<stRecord> _records = new ArrayList<>();
    protected VTDictionary _dictRecords = new VTDictionary();
    
    public MasterBase(String folder, boolean intraday){
        isIntraday = intraday;
        _folder = folder;
        
        sharedCandlesData = new CandlesData(0, "", 0, 30*1024);        

        loadMaster();
        
        if (intraday){
            dictPriceboard = new VTDictionary();

            for (stRecord r: _records){
                updatePriceboard(r.symbol);
            }
        }
    }
    
    abstract protected void loadMaster();
    //  ./base/MASTER
    //  ./base/intraday/MASTER
    abstract protected String getFilepath(stRecord r);
    abstract protected int HEADER_RECORD_SIZE(stRecord r);
    abstract protected int RECORD_SIZE(stRecord r);
    
    public boolean contains(String symbol){
        return _dictRecords.hasObject(symbol);
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
        CandlesData share = null;
        
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);

        xDataInput di = xFileManager.readFile(filePath);
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record
            
            di.resetCursor();
            di.skip(headerRecordSize);
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);

            int length = di.available();
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            

            byte[] p = new byte[recordSize];
            for (int i = 0; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(headerRecordSize + i*recordSize);
                
                di.read(p, 0, recordSize);
                
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
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        xDataInput di = xFileManager.readFile(filePath);
        if (di == null){
            xUtils.trace("");
        }
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record
            
            di.resetCursor();
            di.skip(headerRecordSize);
            
            share.symbol = r.symbol;
            share.shareId = r.shareId;
            share.marketId = r.marketId;

            int length = di.available();
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            

            byte[] p = new byte[recordSize];
            int begin = totalRecord - candleCnt;
            if (begin < 0){
                begin = 0;
            }
            for (int i = begin; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(headerRecordSize + i*recordSize);
                
                di.read(p, 0, recordSize);
                
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
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        xDataInput di = xFileManager.readFile(filePath);
        CandlesData share = null;
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(headerRecordSize);

            int length = di.available();
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[recordSize];
            for (int i = 0; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(headerRecordSize + i*recordSize);
                
                di.read(p, 0, recordSize);
                
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
    
    private void readDataIntraday(stRecord r, int candleCnt, CandlesData share
            ) {
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        xDataInput di = xFileManager.readFile(filePath);

        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(headerRecordSize);

            int length = di.available();
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            share.symbol = r.symbol;
            share.shareId = r.shareId;
            share.marketId = r.marketId;
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[recordSize];
            int begin = totalRecord - candleCnt;
            if (begin < 0){
                begin = 0;
            }
            for (int i = begin; i < totalRecord; i++) {    
                di.resetCursor();
                di.skip(headerRecordSize + i*recordSize);
                
                di.read(p, 0, recordSize);
                
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
    
    private void readDataIntraday(stRecord r, CandlesData share, boolean lastPrice           
            ) {
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        share.clear();
        xDataInput di = xFileManager.readFile(filePath);
        try {
            int totalRecord;
            di.skip(2);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(headerRecordSize);

            int length = di.available();
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            //share = new CandlesData(r.shareId, r.symbol, r.marketId, totalRecord);
            share.isIntraday = this.isIntraday;
            share.clear();
            
            int lastDate = 0;

            byte[] p = new byte[recordSize];
            for (int i = totalRecord-1; i >= 0; i--) {    
                di.resetCursor();
                di.skip(headerRecordSize + i*recordSize);
                
                di.read(p, 0, recordSize);
                
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
    }
    
    public stRecord getRecord(String symbol){
        return (stRecord)_dictRecords.objectForKeyO(symbol);
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
    
    public boolean readData(int shareId, String symbol, int marketId, 
            int candleCnt, CandlesData share)
    {
        share.clear();
        stRecord r = getRecord(symbol);
        if (r == null){
            return false;
        }
        
        r.shareId = shareId;
        r.marketId = marketId;
        
        if (isIntraday){
            readDataIntraday(r, candleCnt, share);
        }
        else{
            readDataD(r, candleCnt, share);
        }
        
        return true;
    }
    
    public CandlesData readData(int shareId, String symbol, 
            int candleCnt)
    {
        CandlesData share = new CandlesData(shareId, symbol, 0, candleCnt);
        
        readData(shareId, symbol, 0, candleCnt, share);
        
        return share;
    }
    
    protected CandlesData sharedCandlesData;
    protected VTDictionary dictPriceboard;
    void updatePriceboard(String symbol){
        if (!isIntraday){
            return;
        }
        
        if (sharedCandlesData == null){
            sharedCandlesData = new CandlesData(0, symbol, 0, 24*3600);
        }

        Priceboard ps = (Priceboard)dictPriceboard.objectForKeyO(symbol);
        if (ps == null){
            ps = new Priceboard(symbol);
            
            stRecord r = getRecord(symbol);
            ps._fileId = r.fileId;
            ps.rIntrday = r;
            
            dictPriceboard.setValue(ps, symbol);
            dictPriceboard.setValue(ps, ""+r.fileId);
        }
        
        long now = System.currentTimeMillis();
        long elapsed = (now - ps._timeUpdate)/1000;
        long elapsedOpen = (now - ps._timeUpdateOpen)/1000;
        sharedCandlesData.clear();
        if (elapsedOpen > 3600){
            //  reupdate 
            readDataIntraday(ps.rIntrday, sharedCandlesData, false);
            
            int candles = sharedCandlesData.getCandleCnt();
            ps.reset();
            ps._timeUpdate = now;
            ps._timeUpdateOpen = now;
            for (int i = 0; i < candles; i++){
                if (i == 0){
                    ps._prevClose = sharedCandlesData.close[0];
                    continue;
                }
                //=====================
                
                int v = sharedCandlesData.volume[i];
                ps._volume += v;
                if (ps._open == 0 && sharedCandlesData.open[i] > 0){
                    ps._timeUpdateOpen = now;
                    ps._open = sharedCandlesData.open[i];
                }
                ps._price = sharedCandlesData.close[i];
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
                
                int date = sharedCandlesData.date[i];
                ps._date = xUtils.dateFromPackagedDate(date);
                ps._time = xUtils.timeFromPackagedDate(date);
            }
        }
        else{
            readDataIntraday(ps.rIntrday, sharedCandlesData, true);

            int candles = sharedCandlesData.getCandleCnt();

            ps._timeUpdate = now;

            if (candles == 0){
                int v = sharedCandlesData.volume[0];
                ps._volume += v;
                if (ps._open == 0 && sharedCandlesData.open[0] > 0){
                    ps._timeUpdateOpen = now;
                    ps._open = sharedCandlesData.open[0];
                }
                ps._price = sharedCandlesData.close[0];
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
                
                int date = sharedCandlesData.date[0];
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
    
    public void filterVNSymbols(ArrayList<Priceboard> arrPriceboard){
        ArrayList<Priceboard> nonVN = new ArrayList<>();
        for (Priceboard ps: arrPriceboard){
            if (ps._symbol.length() != 3){
                nonVN.add(ps);
            }
        }
        
        for (Priceboard ps: nonVN){
            arrPriceboard.remove(ps);
        }
    }
    
    public void doStatisticOnSymbol(Priceboard ps)
    {
        if (isIntraday){
            return;
        }
        try{
            sharedCandlesData.clear();
            stRecord historicalRecord = ps.rHistory;
            if (historicalRecord != null){
                readDataD(historicalRecord, 15, sharedCandlesData);
                ps._avgVolume = sharedCandlesData.getAvgVol(10);
            }
        }
        catch(Throwable e){
            
        }
    }
}
