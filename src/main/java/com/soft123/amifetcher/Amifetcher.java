/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.soft123.amifetcher;
import spark.Spark.*;
import com.data.CandlesData;
import com.data.Priceboard;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import spark.Request;
import spark.Response;
import xframe.framework.xDataOutput;
import xframe.utils.xUtils;


/**
 *
 * @author thuyps
 */
public class Amifetcher {
    String DATABASE_PATH = "./datapro_am";
    XMaster _xmasterDaily;
    XMaster _xmasterIntraday;
    Master _masterDaily;
    Master _masterIntraday;
    
    static Amifetcher amifetcher;
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        amifetcher = new Amifetcher();
        
        amifetcher.loadMasters();
        amifetcher.guardIntradayFiles();
        
        spark.Spark.port(4567);

        spark.Spark.get("/history", (req, res) -> {
            amifetcher.doGetHistory(req, res);
            return res.raw();
        });
        
        spark.Spark.get("/intraday", (req, res) -> {
            amifetcher.doGetIntraday(req, res);
            return res.raw();
        });
        
        spark.Spark.get("/priceboard", (req, res) -> {
            amifetcher.doGetPriceboard(req, res);
            return res.raw();
        });

        System.out.println("Fetcher running at: http://localhost:4567");        
    }
    
    public Amifetcher(){
        
    }

    public void loadMasters(){
        _xmasterDaily = new XMaster(DATABASE_PATH, false);
        _xmasterIntraday = new XMaster(DATABASE_PATH, true);

        _masterDaily = new Master(DATABASE_PATH, false);
        _masterIntraday = new Master(DATABASE_PATH, true);
    }
    
    //  http://localhost:4567/history?symbol=VNM&id=233&mid=0&startdate=20250303
    public void doGetHistory(Request request, Response response){
        try{
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("startdate");
            int date = xUtils.stringYYYYMMDDToDateInt(startDate, "");

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);

            CandlesData share = _xmasterDaily.readData(shareId, symbol, market, date, 0);
            if (share == null){
                share = _masterDaily.readData(shareId, symbol, market, date, 0);
            }
            if(share != null){
                xDataOutput o = share.writeTo();
                if (o != null && o.size() > 0){
                    response.type("application/octet-stream");
                    
                    String filename = String.format("%s_%s", symbol, startDate);
                    response.header("Content-Disposition", "inline; filename=\"" + filename + "\"");
                    response.raw().setContentLength((int) o.size());
                    
                    try (OutputStream os = response.raw().getOutputStream()) {
                        os.write(o.getBytes(), 0, o.size());
                    }
                }
            }
            else{
                response.status(404);
            }
        }
        catch(Throwable e){
            response.status(404);
        }
    }
    
    //  http://localhost:4567/intraday?symbol=VNM&id=233&mid=0&startdate=20250503&starttime=1050
    public void doGetIntraday(Request request, Response response){
        try{
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("startdate");
            String startTime = request.queryParams("starttime");
            int date = xUtils.stringYYYYMMDDToDateInt(startDate, "");
            int time = xUtils.stringHHMMSSToTimeInt(startTime, "");

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);

            CandlesData share = _xmasterIntraday.readData(shareId, symbol, market, date, time);
            if (share == null){
                share = _masterIntraday.readData(shareId, symbol, market, date, time);
            }
            if(share != null){
                xDataOutput o = share.writeTo();
                if (o != null && o.size() > 0){
                    response.type("application/octet-stream");
                    
                    String filename = String.format("%s_%s", symbol, startDate);
                    response.header("Content-Disposition", "inline; filename=\"" + filename + "\"");
                    response.raw().setContentLength((int) o.size());
                    
                    try (OutputStream os = response.raw().getOutputStream()) {
                        os.write(o.getBytes(), 0, o.size());
                    }
                }
            }
            else{
                response.status(404);
            }
        }
        catch(Throwable e){
            response.status(404);
        }
    }    
    
    //  http://localhost:4567/priceboard?symbols=VNM,HAG,XAU/USD
    public void doGetPriceboard(Request request, Response response){
        try{
            String symbols = request.queryParams("symbols");
            
            String ss[] = symbols.split("[,]");
            ArrayList<Priceboard> arr = new ArrayList<>();
            if (ss.length == 0 && symbols.compareTo("*") == 0){
                _xmasterIntraday.getPriceboard(null, arr);
                _masterIntraday.getPriceboard(null, arr);
            }
            else{
                ArrayList<String> arrSymb = new ArrayList<>();
                for (String sb: ss){
                    arrSymb.add(sb);
                }
                _xmasterIntraday.getPriceboard(arrSymb, arr);
                _masterIntraday.getPriceboard(arrSymb, arr);
            }
            
            if(arr != null && arr.size() > 0){
                StringBuffer sb = new StringBuffer();
                
                for (Priceboard ps: arr)
                {
                    sb.append(ps.toString());
                    sb.append('\n');
                }
                
                {
                    response.type("text/plain");
                    
                    String s = sb.toString();
                    response.raw().getOutputStream().write(s.getBytes());
                }
            }
            else{
                response.status(404);
            }
        }
        catch(Throwable e){
            response.status(404);
        }
    }    
    
    void guardIntradayFiles(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String folderToGuard = String.format("%s/intraday", DATABASE_PATH);

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
                                System.out.println("File đã bị sửa đổi: " + event.context());
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
    
    public void packHistoricalDB(int candleCnt)
    {
        int avgVol10 = 100;     //  100 = 10k
        int minCandles = 50;
        CandlesData share = new CandlesData(0, "", candleCnt);
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        //  master
        ArrayList<stRecord> records = _masterDaily.getRecords();
        for (stRecord r: records){
            _masterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            if (share.getCandleCnt() > minCandles){
                int vol10 = share.getAvgVol(6);
                if (vol10 < avgVol10){
                    continue;
                }
            }
            
            shareCnt++;
            share.writeToOutputForPacking(o);
        }
        //  xmaster
        records = _xmasterDaily.getRecords();
        for (stRecord r: records){
            _xmasterDaily.readData(r.shareId, r.symbol, r.marketId, candleCnt, share);
            if (share.getCandleCnt() > minCandles){
                int vol10 = share.getAvgVol(6);
                if (vol10 < avgVol10){
                    continue;
                }
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
    }
    
    public void packIntradayDB(int candleCnt, int minutesPerCandle)
    {
        int avgVol30 = 1500;     //  100 = 10k
        int minCandles = 50;
        CandlesData share = new CandlesData(0, "", candleCnt);
        
        int totalCandles = candleCnt*minutesPerCandle;
        
        xDataOutput o = new xDataOutput(20*1024*1024);
        o.setCursor(4);
        int shareCnt = 0;
        
        //  master
        ArrayList<stRecord> records = _masterIntraday.getRecords();
        for (stRecord r: records){
            _masterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            if (share.getCandleCnt() > minCandles){
                int vol30 = share.getAvgVol(30);
                if (vol30 < avgVol30){
                    continue;
                }
            }
            
            shareCnt++;
            share.changeCandleType(minutesPerCandle);
            share.writeToOutputForPacking(o);
        }
        //  xmaster
        records = _xmasterIntraday.getRecords();
        for (stRecord r: records){
            _xmasterIntraday.readData(r.shareId, r.symbol, r.marketId, totalCandles, share);
            if (share.getCandleCnt() > minCandles){
                int vol30 = share.getAvgVol(30);
                if (vol30 < avgVol30){
                    continue;
                }
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
    }
    
    //  sort by average volume
    void loadVNSymbols(){
        
    }
}
