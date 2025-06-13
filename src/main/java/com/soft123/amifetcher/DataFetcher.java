/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

import com.data.CandlesData;
import com.data.DBHelper;
import com.data.Priceboard;
import com.data.VTDictionary;
import java.io.ByteArrayOutputStream;
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
import java.util.zip.GZIPOutputStream;
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
    VTDictionary _symbolsOfMarket;
    VTDictionary _gzipPackedDB;
    
    boolean _packableDB;
    VTDictionary vars = new VTDictionary();
    VTDictionary _majorSymbolsDict = new VTDictionary();  
    VTDictionary _majorPriceboardDict = new VTDictionary();  
    
    public DataFetcher(String folder){
        _folder = folder;
        _priceboardMap = new VTDictionary();
        _symbolsOfMarket = new VTDictionary();
        
        _gzipPackedDB = new VTDictionary();
        _majorSymbolsDict = VTDictionary.loadFromFile(null, "majorsymbols.txt");
        
        xUtils.trace("DB DataFetcher: " + _folder);
        
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
        
        debugPrintPriceboard();
    }    
    
    ArrayList<Priceboard> _arrPriceboard;
    void debugPrintPriceboard(){
        xUtils.trace("========================");
        int idx = 0;
        Iterator<String> keys = _priceboardMap.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            if (key.indexOf("d_") == 0 || key.indexOf("i_") == 0){
                continue;
            }
            Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(key);
            xUtils.trace(String.format("%d: %s", ++idx, ps.toString()));
        }
        xUtils.trace("========================");
    }
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
                                String msg = String.format("%s Modifed %s/%s", xUtils.timeAsStringDDHHMMSS(), _folder, event.context());
                                System.out.println(msg);
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                        
                        Thread.sleep(3500);
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
                        
                        Thread.sleep(3000);
                    }    
                }
                catch(Throwable e){
                    e.printStackTrace();
                }                
            }
        };
        
        new Thread(r).start();

    }    
    
   
    String packedFilename(String market, int candleFrame){
        String filename = String.format("packed_m%s_%d.his", market, candleFrame);
        return filename;
    }
    
    //  market: .FX; .VC; .COM
    public stPackData getHistoricalDB(String market, int frame, int candles){
        if (!_packableDB){
            return null;
        }
        
        long now = System.currentTimeMillis();
        
        //  check if need re-pack full DB
        boolean isExpired = false;
        stPackData z = getGZipPackedData(market, frame, true);
        if (z == null){
            isExpired = true;
        }
        else{
            long createdTime = z.createdTime;
            double elapsed = (now - createdTime)/1000;
            float expiredSeconds = frame/2;
            if (elapsed > expiredSeconds){
                isExpired = true;
            }
        }
        
        //  repack full DB
        if (isExpired){
            int packingCandles = frame == CandlesData.CANDLE_DAILY?250:250;
            
            synchronized (this){
                doPackDB(market, frame, packingCandles, true);
            }
        }
        
        //-------------------------
        if (candles < 30){
            //  snapshot
            isExpired = false;
            z = getGZipPackedData(market, frame, false);
            if (z == null){
                isExpired = true;
            }
            else{
                double elapsed = (now - z.createdTime)/1000;
                float expiredSeconds = 60;
                if (frame == 1000){
                    expiredSeconds = 3600;
                }
                else if (frame == 30){
                    expiredSeconds = 5*60;
                }
                if (elapsed > expiredSeconds){
                    isExpired = true;
                }
            }
            if (isExpired){
                doPackDB(market, frame, candles, false);
            }
            z = getGZipPackedData(market, frame, false);
        }
        else{
            z = getGZipPackedData(market, frame, true);
        }
        return z;
    }
    
    xDataInput extractSnapshotFromDB(int candles, xDataInput di)
    {
        try{           
            int CANDLE_SIZE = 6*4;
            
            int frame = di.readInt();
            int candles0 = di.readInt();
            int shareCnt = di.readInt();
            
            int size = shareCnt*(candles*CANDLE_SIZE + 20);
            xDataOutput o = new xDataOutput(size);

            o.writeInt(frame);
            o.writeInt(candles);
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
    //  m: .FX; .COM; .VC
    boolean isRecordBelongsToMarket(stRecord r, String m){
        if (m.isEmpty()){
            return !r.symbol.contains(".");
        }
        return r.symbol.contains(m);
    }
    
    //  market or fxcrypto
    void doPackDB(String market, int frame, int candles, boolean fullDB)
    {
        if (!_packableDB){
            return;
        }
        if (frame == CandlesData.CANDLE_DAILY){
            packDailyDB(market, candles, fullDB);
        }
        else{
            packIntradayDB(market, frame, candles, fullDB);
        }
    }
    
    //  daily - D
    public void packDailyDB(String market, int candles, boolean fullDB)
    {
        CandlesData share = new CandlesData(0, "", candles);
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4+4+4); //  candleFrame | candles | shareCnt
        int shareCnt = 0;
        
        int total = _arrPriceboard.size();

        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.recordD;
            if (!isRecordBelongsToMarket(r, market)){
                continue;
            }
            boolean ok = _masterDaily.readData(r.shareId, r.symbol, r.marketId, candles, share);
            if (!ok){
                _xmasterDaily.readData(r.shareId, r.symbol, r.marketId, candles, share);
            }
            
            shareCnt++;
            share.writeToOutputForPacking(o, candles);
        }
        
        //  shareCnt
        int totalSize = o.size();
        o.setCursor(0);
        o.writeInt(CandlesData.CANDLE_DAILY);
        o.writeInt(candles);
        o.writeInt(shareCnt);
        
        o.setCursor(totalSize); //  size to the end
        
        byte[] gzipData = gzipData(o.getBytes(), o.size());
        setGZipPackedData(gzipData, market, CandlesData.CANDLE_DAILY, fullDB);
        
        //  create snapshot for optimization
        if (fullDB){
            xDataInput di = extractSnapshotFromDB(30, xDataInput.bind(o));            
            gzipData = gzipData(di.getBytes(), di.size());
            setGZipPackedData(gzipData, market, CandlesData.CANDLE_DAILY, false);
        }
    }
    
    void packIntradayDB(String market, int frame, int candles, boolean fullDB){
        //=================================
        CandlesData share = new CandlesData(0, "", 0);
        
        int totalCandles = 0;//candleCnt*minutesPerCandle;
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4+4+4); //  candleFrame | candles | shareCnt
        int shareCnt = 0;
        
        int total = _arrPriceboard.size();
        
        //-------------------------
        
        for (int i = 0; i < total; i++){
            Priceboard ps = _arrPriceboard.get(i);
            stRecord r = ps.recordI;    //  intraday's record
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
            if (!isRecordBelongsToMarket(r, market)){
                continue;
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
        o.writeInt(frame);
        o.writeInt(candles);
        o.writeInt(shareCnt);
        
        o.setCursor(totalSize);     //  size to the end
        
        byte[] gzipData = gzipData(o.getBytes(), o.size());
        setGZipPackedData(gzipData, market, frame, fullDB);
        
        //  create snapshot for optimization
        if (fullDB){
            xDataInput di = extractSnapshotFromDB(30, xDataInput.bind(o));
            gzipData = gzipData(di.getBytes(), di.size());
            setGZipPackedData(gzipData, market, frame, false);
        }
    }
    
    private void setGZipPackedData(byte[] gzipData, String market, int candleFrame, boolean fullDB){
        String key = String.format("gzip_%s_%d_%s", market, candleFrame, fullDB?"full":"snap");
        
        stPackData z = (stPackData)_gzipPackedDB.objectForKeyO(key);
        if (z == null){
            z = new stPackData(market);
            _gzipPackedDB.setValue(z, key);
        }
        z.setData(gzipData, true);
    }
    private stPackData getGZipPackedData(String market, int candleFrame, boolean fullDB){
        String key = String.format("gzip_%s_%d_%s", market, candleFrame, fullDB?"full":"snap");
        
        stPackData z = (stPackData)_gzipPackedDB.objectForKeyO(key);
        if (z != null){
            return z;
        }
        return null;
    }
    
    private byte[] gzipData(byte[] data, int size){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
            try(GZIPOutputStream gzipOut = new GZIPOutputStream(baos)){
                gzipOut.write(data, 0, size);
            }
            
            return baos.toByteArray();
        }
        catch(Throwable e){
            
        }
        return null;
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
    
    ArrayList<String> symbolsOfMarketMajor(String market)
    {
        ArrayList<String> symbols = null;
        try{
            if (market == null || market.length() == 0){
                market = ".US";
            }
            symbols = _majorSymbolsDict.getArrayString(market);
        }
        catch(Throwable e){
        }
        return symbols;
    }
    
    ArrayList<String> symbolsOfMarket(String market)
    {
        ArrayList<String> symbols = (ArrayList<String>)_symbolsOfMarket.objectForKeyO(market);
        if (symbols == null || symbols.size() == 0)
        {
            ArrayList<String> symbolsAll = _priceboardMap.getKeysAsArray();
            String exchange = DBHelper.convertVNChartMarketToDB(market);
            
            symbols = new ArrayList<>();
            _symbolsOfMarket.setValue(symbols, market);
                    
            ArrayList<Priceboard> arrPriceboard = new ArrayList<>();
            for (String sb: symbolsAll){
                if (!sb.contains("_")){ //  is symbol
                    if (exchange.length() > 0 && sb.contains(exchange)){
                        symbols.add(sb);
                    }else if (exchange.length() == 0 && !sb.contains(".")){
                        symbols.add(sb);
                    }
                }
            }
        }
        symbols = (ArrayList<String>)_symbolsOfMarket.objectForKeyO(market);
        return symbols;
    }
    
    public ArrayList<Priceboard> getPriceboardOfMarketMajorSymbols(ArrayList<String> markets){
        ArrayList<Priceboard> priceboars = new ArrayList<>();
        for (String market: markets){
            ArrayList<Priceboard> major = (ArrayList<Priceboard>)_majorPriceboardDict.objectForKeyO(market);
            if (major != null){
                priceboars.addAll(major);
            }
            else{
                ArrayList<String> symbols = symbolsOfMarketMajor(market);
                if (symbols != null && symbols.size() > 0){
                    major = getPriceboard(symbols);
                    priceboars.addAll(major);
                }
            }
        }
        
        return priceboars;
    }
    
    public ArrayList<Priceboard> getPriceboardOfMarket(String market)
    {
        ArrayList<String> symbols = symbolsOfMarket(market);
        if (symbols == null){
            return new ArrayList<Priceboard>();
        }
       
        ArrayList<Priceboard> arrPriceboard = new ArrayList<>();
        for (String sb: symbols){
            Priceboard ps = (Priceboard)_priceboardMap.objectForKeyO(sb);
            if (ps != null && !ps._symbol.contains("DATAFET")){
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
    
    public ArrayList<Priceboard> getPriceboard(ArrayList<String> arrSymbol)
    {
        if (arrSymbol == null || arrSymbol.size() == 0){
            return Priceboard();
        }
        //====================================
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
            symbol = _xmasterIntraday.convertShortSymbolToFullSymbol(symbol);
            
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
            symbol = _masterIntraday.convertShortSymbolToFullSymbol(symbol);
            
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
            symbol = _xmasterIntraday.convertShortSymbolToFullSymbol(symbol);
            
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
            symbol = _xmasterIntraday.convertShortSymbolToFullSymbol(symbol);
            
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
