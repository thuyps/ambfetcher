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
import xframe.framework.xDataInputRandomAccessFile;
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
    protected int _candleFrame;
    protected ArrayList<stRecord> _records = new ArrayList<>();
    protected VTDictionary _dictRecords;
    protected VTDictionary _priceboardMap;
    
    public MasterBase(String folder, boolean intraday, VTDictionary priceboardMap){
        isIntraday = intraday;
        _folder = folder;
        _dictRecords = new VTDictionary();
        _priceboardMap = priceboardMap;
        
        sharedCandlesData = new CandlesData(0, "", 0, 30*1024);        

        loadMaster();

        for (stRecord r: _records){
            if (intraday){
                updatePriceboardPriceIntraday(r.symbol);
            }
            else{
                updatePriceboardPriceD(r.symbol);
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
    
    public void setCandleFrame(int cf){
        _candleFrame = cf;
    }
    public int getCandleFrame(){
        return _candleFrame;
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

        xDataInputRandomAccessFile rdi = new xDataInputRandomAccessFile(filePath);
        if (rdi.fileSize() == 0){
            rdi.close();
            return null;
        }
        try {
            int totalRecord;
            xDataInput di = rdi.seekTo(2, 10);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record
            
            int dataSize = rdi.fileSize() - headerRecordSize;

            int length = dataSize;
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            
            
            int candleCnt = 256;
            if (candleCnt > totalRecord){
                candleCnt = totalRecord;
            }
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, candleCnt);            

            byte[] p = new byte[recordSize];
            int begin = totalRecord - candleCnt;
            int neededDataSize = candleCnt*recordSize;
            int offset = headerRecordSize + begin*recordSize;
            di = rdi.seekTo(offset, neededDataSize);
            
            for (int i = 0; i < candleCnt; i++) {    
                di.resetCursor();
                di.skip(i*recordSize);
                
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
                //int vol = (int)(convertMBFToFloat(p, 20)/100);
                int vol;
                float volf = convertMBFToFloat(p, 20);
                vol = (int)((volf > 1000000000)?volf/100:volf);
                
                
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
        
        xDataInputRandomAccessFile rdi = new xDataInputRandomAccessFile(filePath);
        if (rdi.fileSize() == 0){
            xUtils.trace("");
        }
        try {
            rdi.seekTo(2, 10);
            int totalRecord;
            totalRecord = rdi.DI().readUShortBig();
            totalRecord -= 1;   //  header record
            
            int dataSize = rdi.fileSize() - headerRecordSize;
            
            share.symbol = r.symbol;
            share.shareId = r.shareId;
            share.marketId = r.marketId;

            int length = dataSize;
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }            

            byte[] p = new byte[recordSize];
            int begin = totalRecord - candleCnt;
            if (begin < 0){
                begin = 0;
            }
            candleCnt = totalRecord - begin;
            int neededDataSize = candleCnt*recordSize;
            int offset = headerRecordSize + begin*recordSize;
            xDataInput di = rdi.seekTo(offset, neededDataSize);
            for (int i = 0; i < candleCnt; i++) {    
                di.resetCursor();
                di.skip(i*recordSize);
                
                di.read(p, 0, recordSize);
                
                float d = convertMBFToFloat(p, 0);
                int date = convertSerialDateToLocalDate(d);
                
                float open = convertMBFToFloat(p, 4);
                float hi = convertMBFToFloat(p, 8);
                float low = convertMBFToFloat(p, 12);
                float close = convertMBFToFloat(p, 16);
                //int vol = (int)(convertMBFToFloat(p, 20)/100);
                float volf = convertMBFToFloat(p, 20);
                int vol = (int)((volf > 1000000000)?volf/100:volf);
                
                share.addCandle(close, open, hi, low, date, vol);
            }
            /*
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
*/
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        rdi.close();
    }
    
    private CandlesData readDataIntraday(stRecord r, int startDate, int startTime) {
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        xDataInputRandomAccessFile rdi = new xDataInputRandomAccessFile(filePath);
        
        CandlesData share = null;
        try {
            int totalRecord;
            xDataInput di = rdi.seekTo(2, 10);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            int dataSize = rdi.fileSize() - headerRecordSize;

            int length = dataSize;
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            int candleCnt = 512;
            if (candleCnt > totalRecord){
                candleCnt = totalRecord;
            }
            
            share = new CandlesData(r.shareId, r.symbol, r.marketId, candleCnt);
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[recordSize];
            int neededDataSize = candleCnt*recordSize;
            int begin = totalRecord - candleCnt;
            int offset = headerRecordSize + begin*recordSize;
            di = rdi.seekTo(offset, neededDataSize);
            
            for (int i = 0; i < candleCnt; i++) {    
                di.resetCursor();
                di.skip(i*recordSize);
                
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
        
        rdi.close();
        
        return share;
    }
    
    private void readDataIntraday(stRecord r, int candleCnt, CandlesData share
            ) {
        String filePath = getFilepath(r);
        int headerRecordSize = HEADER_RECORD_SIZE(r);
        int recordSize = RECORD_SIZE(r);
        
        xDataInputRandomAccessFile rdi = new xDataInputRandomAccessFile(filePath);

        try {
            int totalRecord;
            xDataInput di = rdi.seekTo(2, 10);
            totalRecord = di.readUShortBig();
            totalRecord -= 1;   //  header record

            di.resetCursor();
            di.skip(headerRecordSize);

            int dataSize = rdi.fileSize() - headerRecordSize;
            int length = dataSize;
            
            int recordCount = length/recordSize;
            if (totalRecord > recordCount){
                totalRecord = recordCount;
            }
            
            share.symbol = r.symbol;
            share.shareId = r.shareId;
            share.marketId = r.marketId;
            share.isIntraday = this.isIntraday;

            byte[] p = new byte[recordSize];
            if (candleCnt > totalRecord){
                candleCnt = totalRecord;
            }
            
            int neededDataSize = candleCnt*recordSize;
            int begin = totalRecord - candleCnt;
            int offset = headerRecordSize + begin*recordSize;
            di = rdi.seekTo(offset, neededDataSize);
            
            for (int i = 0; i < candleCnt; i++) {    
                di.resetCursor();
                di.skip(i*recordSize);
                
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
        
        rdi.close();
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
        if (contains(symbol)){
            CandlesData share = new CandlesData(shareId, symbol, 0, candleCnt);

            readData(shareId, symbol, 0, candleCnt, share);

            return share;
        }
        else{
            return null;
        }
    }
    
    protected CandlesData sharedCandlesData;
    
    void updatePriceboardPriceD(String symbol){        
        if (sharedCandlesData == null){
            sharedCandlesData = new CandlesData(0, symbol, 0, 24*3600);
        }

        //--------------
        Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(symbol);
        if (ps == null){
            ps = new Priceboard(symbol);
            
            _priceboardMap.setValue(ps, symbol);
           
        }

        stRecord r = getRecord(symbol);
        if (r == null){
            return;
        }
        ps.recordD = r;    
        String k = "d_" + r.fileId;
        if (_priceboardMap.hasObject(k) == false){
            _priceboardMap.setValue(ps, k);
        }
        //--------------
        
        sharedCandlesData.clear();
        if (symbol.compareTo("AUD/USD") == 0){
            xUtils.trace("");
        }
        
        readData(r.shareId, symbol, 0, 2, sharedCandlesData);

        int candles = sharedCandlesData.getCandleCnt();

        ps._timeUpdate = System.currentTimeMillis();

        if (candles > 1){
            int today = candles-1;
            int previous = candles - 2;
            ps._preDate = sharedCandlesData.date[previous];
            ps._prevClose = sharedCandlesData.close[previous];
            
            ps._date = sharedCandlesData.date[today];
            ps._open = sharedCandlesData.open[today];
            ps.setClose(sharedCandlesData.close[today]);
            ps._highest = sharedCandlesData.hi[today];
            ps._lowest = sharedCandlesData.lo[today];
            ps._volume = sharedCandlesData.volume[today];
        }   
    }
    
    void updatePriceboardPriceIntraday(String symbol){
        if (!isIntraday){
            return;
        }
        
        if (sharedCandlesData == null){
            sharedCandlesData = new CandlesData(0, symbol, 0, 24*3600);
        }

        Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(symbol);
        if (ps == null){
            ps = new Priceboard(symbol);
            
            _priceboardMap.setValue(ps, symbol);
        }
        stRecord r = getRecord(symbol);
        ps.recordI = r;    
        String k = "i_" + r.fileId;
        if (_priceboardMap.hasObject(k) == false){
            _priceboardMap.setValue(ps, k);
        }
        //--------------
        
        long now = System.currentTimeMillis();
        readData(r.shareId, symbol, 0, 2, sharedCandlesData);
        int candles = sharedCandlesData.getCandleCnt();

        ps._timeUpdate = now;

        if (candles > 1){
            int last = candles - 1;
            ps.setClose(sharedCandlesData.close[last]);
            int dt = sharedCandlesData.date[last];
            ps._date = xUtils.dateFromPackagedDate(dt);
            ps._time = xUtils.timeFromPackagedDate(dt);
        }   
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
            stRecord historicalRecord = ps.recordD;
            if (historicalRecord != null){
                readDataD(historicalRecord, 15, sharedCandlesData);
                ps._avgVolume = sharedCandlesData.getAvgVol(10);
            }
        }
        catch(Throwable e){
            
        }
    }
    
    public void logSymbols(String contain, String name){
        xUtils.trace(String.format("================%s================", name));
        int ID = 0;
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO [dbo].[crypto] ([Symbol], [crypto_id]) VALUES\n");
        for (stRecord r: _records){
            
            if (r.symbol.contains("/")){
                //xUtils.trace(r.symbol);
                String s = String.format("('%s', %d),", r.symbol, 500010+ID++);
                sb.append(s);
                sb.append("\n");
            }
        }
        
        xUtils.trace(sb.toString());
        xUtils.trace("==============================");
    }    
}
