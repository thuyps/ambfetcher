/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

import com.data.CandlesData;
import com.data.Priceboard;
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
    boolean _packableDB;
    
    public DataFetcher(String folder){
        _folder = folder;
        
        loadMasters();
    }
    
    public void setPackedFolder(String folder){
        _packedFolder = folder;
    }
   
    public void loadMasters(){
        _xmasterDaily = new XMaster(_folder, false);
        _xmasterIntraday = new XMaster(_folder, true);

        _masterDaily = new Master(_folder, false);
        _masterIntraday = new Master(_folder, true);
    }    
    
    //  sort by average volume
    ArrayList<Priceboard> _arrPriceboard;
    public void setup(boolean packableDB, boolean rtPriceboard){
        _arrPriceboard = new ArrayList<>();
        
        _masterIntraday.getPriceboard(null, _arrPriceboard);
        _xmasterIntraday.getPriceboard(null, _arrPriceboard);
        for (Priceboard ps: _arrPriceboard)
        {
            if (_masterDaily.contains(ps._symbol)){
                stRecord r = _masterDaily.getRecord(ps._symbol);
                ps.rHistory = r;
            }
            else{
                stRecord r = _xmasterDaily.getRecord(ps._symbol);
                ps.rHistory = r;
            }
        }
        //=============================
        _packableDB = packableDB;
        if (packableDB){
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
            guardIntradayFilesForPriceboard();
        }
        
        /*
        if (_arrPriceboard.size() < 10){
            return;
        }
        
        for (int i = 0; i < 10; i++){
            Priceboard ps = _arrPriceboard.get(i);
            xUtils.trace(ps.toString());
        }
*/
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
                                            Priceboard ps = (Priceboard)_xmasterIntraday.dictPriceboard.objectForKeyO("" + fileId);
                                            if (ps != null){
                                                _xmasterIntraday.updatePriceboard(ps._symbol);
                                            }
                                        }
                                    }
                                    else if (filename.endsWith(".dat")){
                                        String ss[] = filename.split("[/]");
                                        filename = ss[ss.length-1];

                                        //  Fxxxx.MDW
                                        filename = filename.replace(".mwd", "");
                                        filename = filename.replace("f", "");

                                        int fileId = xUtils.stringToInt(filename);
                                        if (fileId > 0){
                                            Priceboard ps = (Priceboard)_masterIntraday.dictPriceboard.objectForKeyO("" + fileId);
                                            if (ps != null){
                                                _masterIntraday.updatePriceboard(ps._symbol);
                                            }
                                        }
                                    }
                                }
                                catch(Throwable e1){
                                    e1.printStackTrace();
                                }
                                System.out.println("File is modifed: " + event.context());
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
    
    public void packHistoricalDB(int candleCnt, String type)
    {
        CandlesData share = new CandlesData(0, "", candleCnt);
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        int total = 490 < _arrPriceboard.size()?490:_arrPriceboard.size();

        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.rHistory;
            boolean ok = _masterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            if (!ok){
                _xmasterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            }
            
            shareCnt++;
            share.writeToOutputForPacking(o);
        }
        
        //  shareCnt
        int totalSize = o.size();
        o.setCursor(0);
        o.writeInt(shareCnt);
        o.size();
        
        o.setCursor(totalSize);
        
        String filename = historicalFilename(type);
        xFileManager.saveFile(o, _packedFolder, filename);
    }
    
    String historicalFilename(String type){
        String filename = String.format("packed_%s.his", type);
        return filename;
    }
    
    public xDataInput getHistoricalDB(String type){
        if (!_packableDB){
            return null;
        }

        xDataInput di = xFileManager.readFile(_packedFolder, historicalFilename(type));
        if (di == null){
            int candleCnt = 128;
            int ht = type.charAt(1);
            if (ht == 1){
                candleCnt = 226*4;
            }
            else if (ht == 2){
                candleCnt = 226*2;
            }
            else{
                candleCnt = 226;
            }
            packHistoricalDB(candleCnt, type);
        }
        
        di = xFileManager.readFile(_packedFolder, historicalFilename(type));
        return di;
    }
    
    void packIntradayDB(int candleCnt, int minutesPerCandle, String type)
    {
        if (!_packableDB){
            return;
        }
        CandlesData share = new CandlesData(0, "", candleCnt);
        
        int totalCandles = candleCnt*minutesPerCandle;
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        int total = 490 < _arrPriceboard.size()?490:_arrPriceboard.size();
        
        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.rHistory;
            
            boolean ok = _masterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            if (!ok){
                _xmasterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            }
            
            shareCnt++;
            share.changeCandleType(minutesPerCandle);
            share.writeToOutputForPacking(o);
        }

        //  shareCnt
        int totalSize = o.size();
        o.setCursor(0);
        o.writeInt(shareCnt);
        o.size();
        
        o.setCursor(totalSize);
        
        String filename = intradayFilename(type);
        xFileManager.saveFile(o, _packedFolder, filename);
    }
    
    String intradayFilename(String type){
        String filename = String.format("packed_%s.dat", type);
        return filename;
    }
        
    public xDataInput getIntradayDB(String type){
        if (!_packableDB){
            return null;
        }
        
        String filename = intradayFilename(type);
        
        xDataInput di = xFileManager.readFile(_packedFolder, filename);
        if (di == null){
            if (type.compareTo("M5") == 0){
                packIntradayDB(150, 5, type);
            }
            else{
                packIntradayDB(150, 30, type);
            }
        }
        di = xFileManager.readFile(_packedFolder, filename);
        return di;
    }    
    
    public ArrayList<Priceboard> getPriceboard(ArrayList<String> arrSymbol)
    {
        ArrayList<Priceboard> arrPriceboard = new ArrayList<>();
        
        _masterIntraday.getPriceboard(arrSymbol, arrPriceboard);
        _xmasterIntraday.getPriceboard(arrSymbol, arrPriceboard);
        
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
        return share;
    }
    
    public CandlesData getHistory(int shareId, String symbol, int candles){
        CandlesData share = _xmasterDaily.readData(shareId, symbol, candles);
        if (share == null){
            share = _masterDaily.readData(shareId, symbol, candles);
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
            determineCandleFrame(_xmasterIntraday, symbol, candleType);
            
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
            determineCandleFrame(_masterIntraday, symbol, candleType);
            
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
    
    private void determineCandleFrame(MasterBase master, String symbol, int candleType)
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
            determineCandleFrame(_xmasterIntraday, symbol, candleType);
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
            determineCandleFrame(_masterIntraday, symbol, candleType);
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
