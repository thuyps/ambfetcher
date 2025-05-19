/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

import com.data.CandlesData;
import com.data.Priceboard;
import com.data.VTDictionary;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;


/**
 *
 * @author thuyps
 */
public class DataFetcher {
    String _folder;
    String _packedFolder;
    
    XMaster _xmasterDaily;
    XMaster _xmasterIntraday;
    Master _masterDaily;
    Master _masterIntraday;

    VTDictionary _priceboardMap;
    
    boolean _packableDB;
    VTDictionary vars = new VTDictionary();
    
    public DataFetcher(String folder){
        _folder = folder;
        _priceboardMap = new VTDictionary();
        
        loadMasters();
    }
    
    public void setPackedFolder(String folder){
        _packedFolder = folder;
    }
   
    public void loadMasters(){
        _xmasterDaily = new XMaster(_folder, false, _priceboardMap);
        _masterDaily = new Master(_folder, false, _priceboardMap);
        
        _xmasterIntraday = new XMaster(_folder, true, _priceboardMap);
        _masterIntraday = new Master(_folder, true, _priceboardMap);
    }    
    
    ArrayList<Priceboard> _arrPriceboard;
    ArrayList<Priceboard> Priceboard(){
        if (_arrPriceboard == null){
            _arrPriceboard = new ArrayList<>();
        }
        if (_arrPriceboard.size() == 0){
            Iterator<String> keys = _priceboardMap.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                if (key.indexOf("d_") == 0 || key.indexOf("i_") == 0){
                    continue;
                }
                Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(key);
                _arrPriceboard.add(ps);
            }
        }
        
        ArrayList<Priceboard> arr = new ArrayList<>();
        arr.addAll(_arrPriceboard);
        return arr;
    }

    public void setup(boolean packableDB, boolean rtPriceboard){
        ArrayList<Priceboard> arr = Priceboard();
        
        _packableDB = packableDB;
        if (packableDB || rtPriceboard){
            _masterIntraday.filterVNSymbols(_arrPriceboard);
            _xmasterIntraday.filterVNSymbols(_arrPriceboard);

            for (Priceboard ps: _arrPriceboard)
            {
                if (_masterDaily.contains(ps._symbol)){
                    _masterDaily.doStatisticOnSymbol(ps);
                }
                else{
                    _xmasterDaily.doStatisticOnSymbol(ps);
                }
            }

            Collections.sort(_arrPriceboard, new Comparator<Priceboard>(){
                @Override
                public int compare(Priceboard o1, Priceboard o2) {
                    if (o1._avgVolume > o2._avgVolume){
                        return -1;
                    }
                    else if (o1._avgVolume < o2._avgVolume){
                        return 1;
                    }
                    return 0;
                }
            });
        }
        
        if (rtPriceboard){
            guardDailyFilesForPriceboard();
            guardIntradayFilesForPriceboard();
        }
    }
    
    private void guardDailyFilesForPriceboard(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String folderToGuard = String.format("%s", _folder);

                try{
                    Path path = Paths.get(folderToGuard);
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

                    System.out.println("Đang theo dõi thư mục...");

                    while (true) {
                        WatchKey key = watchService.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            //System.out.println("Sự kiện: " + event.kind() + " - File: " + event.context());

                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                try{
                                    Path modifiedFile = (Path) event.context();

                                    String filename = modifiedFile.toString();
                                    filename = filename.toLowerCase();
                                    
                                    if (filename.endsWith(".mwd")) {
                                        String ss[] = filename.split("[/]");
                                        filename = ss[ss.length-1];

                                        //  Fxxxx.MDW
                                        filename = filename.replace(".mwd", "");
                                        filename = filename.replace("f", "");

                                        int fileId = xUtils.stringToInt(filename);
                                        if (fileId > 0){
                                            Priceboard ps = (Priceboard)_xmasterDaily._priceboardMap.objectForKeyO("d_" + fileId);
                                            if (ps != null){
                                                _xmasterDaily.updatePriceboardPriceD(ps._symbol);
                                            }
                                        }
                                    }
                                    else if (filename.endsWith(".dat")){
                                        String ss[] = filename.split("[/]");
                                        filename = ss[ss.length-1];

                                        //  Fxxxx.MDW
                                        filename = filename.replace(".dat", "");
                                        filename = filename.replace("f", "");

                                        int fileId = xUtils.stringToInt(filename);
                                        if (fileId > 0){
                                            Priceboard ps = (Priceboard)_xmasterDaily._priceboardMap.objectForKeyO("d_" + fileId);
                                            if (ps != null){
                                                _xmasterDaily.updatePriceboardPriceD(ps._symbol);
                                            }
                                        }
                                    }
                                }
                                catch(Throwable e1){
                                    e1.printStackTrace();
                                }
                                System.out.println(String.format("Modifed %s/%s", _folder, event.context()));
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }    
                }
                catch(Throwable e){
                    e.printStackTrace();
                }                
            }
        };
        
        new Thread(r).start();

    } 
    
    private void guardIntradayFilesForPriceboard(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String folderToGuard = String.format("%s/intraday", _folder);

                try{
                    Path path = Paths.get(folderToGuard);
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

                    System.out.println("Đang theo dõi thư mục...");

                    while (true) {
                        WatchKey key = watchService.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            //System.out.println("Sự kiện: " + event.kind() + " - File: " + event.context());

                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                try{
                                    Path modifiedFile = (Path) event.context();

                                    String filename = modifiedFile.toString();
                                    filename = filename.toLowerCase();

                                    if (filename.endsWith(".mwd")) {
                                        String ss[] = filename.split("[/]");
                                        filename = ss[ss.length-1];

                                        //  Fxxxx.MDW
                                        filename = filename.replace(".mwd", "");
                                        filename = filename.replace("f", "");

                                        int fileId = xUtils.stringToInt(filename);
                                        if (fileId > 0){
                                            Priceboard ps = (Priceboard)_xmasterIntraday._priceboardMap.objectForKeyO("i_" + fileId);
                                            if (ps != null){
                                                _xmasterIntraday.updatePriceboardPriceIntraday(ps._symbol);
                                            }
                                        }
                                    }
                                    else if (filename.endsWith(".dat")){
                                        String ss[] = filename.split("[/]");
                                        filename = ss[ss.length-1];

                                        //  Fxxxx.MDW
                                        filename = filename.replace(".dat", "");
                                        filename = filename.replace("f", "");

                                        int fileId = xUtils.stringToInt(filename);
                                        if (fileId > 0){
                                            Priceboard ps = (Priceboard)_masterIntraday._priceboardMap.objectForKeyO("i_" + fileId);
                                            if (ps != null){
                                                _masterIntraday.updatePriceboardPriceIntraday(ps._symbol);
                                            }
                                        }
                                    }
                                }
                                catch(Throwable e1){
                                    e1.printStackTrace();
                                }
                                System.out.println(String.format("Modifed %s/%s", _folder, event.context()));
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }    
                }
                catch(Throwable e){
                    e.printStackTrace();
                }                
            }
        };
        
        new Thread(r).start();

    }    
    
    //  daily - D
    public void packDailyDB(int candleCnt)
    {
        CandlesData share = new CandlesData(0, "", candleCnt);
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        int total = 490 < _arrPriceboard.size()?490:_arrPriceboard.size();

        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.recordD;
            boolean ok = _masterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            if (!ok){
                _xmasterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            }
            
            shareCnt++;
            share.writeToOutputForPacking(o, candleCnt);
        }
        
        //  shareCnt
        int totalSize = o.size();
        o.setCursor(0);
        o.writeInt(shareCnt);
        
        o.setCursor(totalSize); //  size to the end
        
        String filename = packedFilename(CandlesData.CANDLE_DAILY);
        xFileManager.saveFile(o, _packedFolder, filename);
    }
    
    String packedFilename(int candleFrame){
        String filename = String.format("packed_m%d.his", candleFrame);
        return filename;
    }
    
    public xDataInput getHistoricalDB(int frame, int candles){
        if (!_packableDB){
            return null;
        }
        
        String filename = packedFilename(frame);

        String createdKey = String.format("time_%s", filename);
        long createdTime = vars.objectForKeyAsLong(createdKey);
        long now = System.currentTimeMillis();
        double elapsed = (now - createdTime)/1000;
        
        float expiredSeconds = frame/2;
        if (elapsed > expiredSeconds){
            xFileManager.removeFile(_packedFolder, filename);
        }

        xDataInput di = xFileManager.readFile(_packedFolder, filename);
        if (di == null){
            int packingCandles = frame == CandlesData.CANDLE_DAILY?3*250:250;
            doPackDB(frame, packingCandles);
            vars.setValue(now, createdKey);
        }
        
        di = xFileManager.readFile(_packedFolder, filename);
        if (candles < 30){
            //  snapshot, extract frame
            di = extractSnapshotFromDB(candles, di);
        }
        return di;
    }
    
    xDataInput extractSnapshotFromDB(int candles, xDataInput di)
    {
        try{           
            int CANDLE_SIZE = 6*4;
            
            int shareCnt = di.readInt();
            int size = shareCnt*(candles*CANDLE_SIZE + 20);
            xDataOutput o = new xDataOutput(size);

            o.writeInt(shareCnt);
            
            for (int i = 0; i < shareCnt; i++){
                String symbol = di.readUTF();
                int candlesOfShare = di.readInt();
                di.markCursor();
                
                int candlesToRead = candles < candlesOfShare?candles:candlesOfShare;
                int begin = candlesOfShare - candlesToRead;
                if (begin < 0){
                    begin = 0;
                }
                //  seek to 
                int offset = begin*CANDLE_SIZE;
                di.skip(offset);
                
                o.writeUTF(symbol);
                o.writeInt(candlesToRead);
                o.write(di.getBytes(), di.getCurrentOffset(), candlesToRead*CANDLE_SIZE);
                
                di.resetCursorToMarked();
                di.skip(candlesOfShare*CANDLE_SIZE);
            }
            
            di = new xDataInput(o.getBytes(), 0, o.size());
            return di;
        }
        catch(Throwable e){
            e.printStackTrace();
        }
        
        return null;
    }
    
    void doPackDB(int frame, int candles)
    {
        if (!_packableDB){
            return;
        }
        if (frame == CandlesData.CANDLE_DAILY){
            packDailyDB(candles);
            return;
        }
        //=================================
        CandlesData share = new CandlesData(0, "", 0);
        
        int totalCandles = 0;//candleCnt*minutesPerCandle;
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        int total = 490 < _arrPriceboard.size()?490:_arrPriceboard.size();
        
        //-------------------------
        
        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.recordI;
            if (r == null){
                xUtils.trace("no way!!!");
                continue;
            }
            
            //------total candles need to read:
            if (r.period == 0){
                if (_masterIntraday.contains(r.symbol)){
                    determineCandleFrame(_masterIntraday, r.symbol);
                }
                else{
                    determineCandleFrame(_xmasterDaily, r.symbol);
                }
            }
            if (r.period == 0){
                //  daily
                totalCandles = candles;
            }
            else if (frame > r.period){
                totalCandles = candles*frame/r.period;
            }
            else{
                totalCandles = candles;
            }
            //--------------------------------
            
            boolean ok = _masterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            if (!ok){
                _xmasterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            }
            
            shareCnt++;
            share.changeCandleType(frame);
            
            //  write to outputData
            share.writeToOutputForPacking(o, candles);
        }

        //  shareCnt
        int totalSize = o.size();
        o.setCursor(0);
        o.writeInt(shareCnt);
        
        o.setCursor(totalSize);     //  size to the end
        
        String filename = packedFilename(frame);
        xFileManager.saveFile(o, _packedFolder, filename);
    }
    
    /*
    public xDataInput getIntradayDB(int candleFrame){
        if (!_packableDB){
            return null;
        }
        
        String type = String.format("m%02d", candleFrame);
        String filename = intradayFilename(type);
        
        xDataInput di = xFileManager.readFile(_packedFolder, filename);
        if (di == null){
            packIntradayDB(150, candleFrame, type);
        }
        di = xFileManager.readFile(_packedFolder, filename);
        return di;
    }    
    */
    public ArrayList<Priceboard> getPriceboard(ArrayList<String> arrSymbol)
    {
        ArrayList<Priceboard> arrPriceboard = new ArrayList<>();
        
        for (String sb: arrSymbol){
            Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(sb);
            if (ps != null){
                if (ps.isExpired()){
                    if (_masterDaily.contains(ps._symbol)){
                        _masterDaily.updatePriceboardPriceD(ps._symbol);
                    }
                    else if (_xmasterDaily.contains(ps._symbol)){
                        _xmasterDaily.updatePriceboardPriceD(ps._symbol);
                    }
                }
                arrPriceboard.add(ps);
            }
        }
        
        return arrPriceboard;
    }
    
    //  history: candle type: D
    public CandlesData getHistory(int shareId, String symbol, int market, int date){
        if (date == 0){
            return getHistory(shareId, symbol, 3000);
        }
        CandlesData share = _xmasterDaily.readData(shareId, symbol, market, date, 0);
        if (share == null){
            share = _masterDaily.readData(shareId, symbol, market, date, 0);
        }        
        if (share != null){
            share.candleFrame = CandlesData.CANDLE_DAILY;
        }
        return share;
    }
    
    public CandlesData getHistory(int shareId, String symbol, int candles){
        CandlesData share = _xmasterDaily.readData(shareId, symbol, candles);
        if (share == null){
            share = _masterDaily.readData(shareId, symbol, candles);
        }        
        if (share != null){
            share.candleFrame = CandlesData.CANDLE_DAILY;
        }
        return share;
    }
    
    //  intraday: candle type: M5, M1 (if using datatick)
    public CandlesData getIntraday(int shareId, String symbol, int market, int date, int time, int candleType){
        if (date == 0){
            return getIntraday(shareId, symbol, 3000, candleType);
        }
        CandlesData share = null;
        if (_xmasterIntraday.contains(symbol)){
            determineCandleFrame(_xmasterIntraday, symbol);
            
            share = _xmasterIntraday.readData(shareId, symbol, market, date, time);
            stRecord r = _xmasterIntraday.getRecord(symbol);
            boolean needChangeCandleType = false;
            if (r.period > 0 && r.period < candleType){
                needChangeCandleType = true;
            }
            if (needChangeCandleType){
                share.changeCandleType(candleType);
            }
        }
        else if (_masterIntraday.contains(symbol)){
            determineCandleFrame(_masterIntraday, symbol);
            
            share = _masterIntraday.readData(shareId, symbol, market, date, time);
            stRecord r = _masterIntraday.getRecord(symbol);
            boolean needChangeCandleType = false;
            if (r.period > 0 && r.period < candleType){
                needChangeCandleType = true;
            }
            if (needChangeCandleType){
                share.changeCandleType(candleType);
            }
        }
        
        return share;
    }
    
    private void determineCandleFrame(MasterBase master, String symbol)
    {
        stRecord r = master.getRecord(symbol);
        if (r != null && r.period == 0){
            //  determine period
            CandlesData share = master.readData(0, symbol, 20);
            if (share.getCandleCnt() > 2){
                int cd = share.date[0];
                int date0 = xUtils.dateFromPackagedDate(cd);
                int time0 = xUtils.timeFromPackagedDate(cd);
                int m0 = xUtils.EXTRACT_HOUR(time0)*60 + xUtils.EXTRACT_MINUTE(time0);
                int minPeriod = 1000;
                for (int i = 1; i < share.getCandleCnt(); i++){
                    cd = share.date[i];
                    int date = xUtils.dateFromPackagedDate(cd);
                    int time = xUtils.timeFromPackagedDate(cd);
                    if (date == date0){
                        int m1 = xUtils.EXTRACT_HOUR(time)*60 + xUtils.EXTRACT_MINUTE(time);
                        if (m1 > m0 && m1 - m0 < minPeriod){
                            minPeriod = m1 - m0;
                            m0 = m1;
                        }
                    }
                }
                if (minPeriod < 1000 && minPeriod > 0){
                    r.period = minPeriod;
                }
            }
        }
    }
    
    public CandlesData getIntraday(int shareId, String symbol, int candles, int candleType){
        CandlesData share = null;
        if (_xmasterIntraday.contains(symbol)){
            determineCandleFrame(_xmasterIntraday, symbol);
            stRecord r = _xmasterIntraday.getRecord(symbol);
            boolean needChangeCandleType = false;
            if (r.period > 0 && r.period < candleType){
                candles = (int)(candles*(float)candleType/r.period);
                needChangeCandleType = true;
            }
            share = _xmasterIntraday.readData(shareId, symbol, candles);
            if (needChangeCandleType){
                share.changeCandleType(candleType);
            }
        }
        else if (_masterIntraday.contains(symbol)){
            determineCandleFrame(_masterIntraday, symbol);
            stRecord r = _masterIntraday.getRecord(symbol);
            boolean needChangeCandleType = false;
            if (r.period > 0 && r.period < candleType){
                candles = (int)(candles*(float)candleType/r.period);
                needChangeCandleType = true;
            }
            share = _masterIntraday.readData(shareId, symbol, candles);
            if (needChangeCandleType){
                share.changeCandleType(candleType);
            }
        }
        return share;
    }    
}
