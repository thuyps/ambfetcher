/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.soft123.amifetcher;
import android.content.Context;
import com.data.CandlesData;
import com.data.Priceboard;
import java.io.OutputStream;
import java.util.ArrayList;
import spark.Request;
import spark.Response;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;


/**
 *
 * @author thuyps
 */
public class Amifetcher {

    static String PACKED_FOLDER = "./packed";
    DataFetcher _dataHistorical;
    DataFetcher _dataHistoricalM1;
    
    static Amifetcher amifetcher;
    public static void main(String[] args) {
        System.out.println("=======================");
        
        xFileManager.setFileManager(Context.getInstance());
        xFileManager.createAllDirs(PACKED_FOLDER);
        
        amifetcher = new Amifetcher();
        
        //----------------------------------
        spark.Spark.port(2610);

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
        
        spark.Spark.get("/packed", (req, res) -> {
            amifetcher.doGetPacked(req, res);
            return res.raw();
        });        

        System.out.println("Fetcher running at: http://localhost:4567");        
    }
    
    public Amifetcher(){
        _dataHistorical = new DataFetcher("./datapro");
        _dataHistorical.setPackedFolder(PACKED_FOLDER);
        _dataHistorical.setup(true, false);
        
        //_dataHistoricalM1 = new DataFetcher("./datatick");
        //_dataHistoricalM1.setup(false, true);
    }

    //  daily only
    //  http://localhost:4567/history?symbol=VNM&id=233&mid=0&candles=&startdate=20250303
    public void doGetHistory(Request request, Response response){
        try{
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("startdate");
            int date = xUtils.stringYYYYMMDDToDateInt(startDate, "");
            
            int candles = xUtils.stringToInt(request.queryParams("candles"));

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);

            CandlesData share = null;
            if (candles > 0){
                share = _dataHistorical.getHistory(shareId, symbol, candles);
            }
            else{
                share = _dataHistorical.getHistory(shareId, symbol, market, date);
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
    
    //  candle: valid value: M1/M30
    //  http://localhost:4567/intraday?symbol=VNM&id=233&mid=0&frame=M1&candles=&startdate=20250503&starttime=1050
    public void doGetIntraday(Request request, Response response){
        try{
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("startdate");
            String startTime = request.queryParams("starttime");
            int date = xUtils.stringYYYYMMDDToDateInt(startDate, "");
            int time = xUtils.stringHHMMSSToTimeInt(startTime, "");
            String frame = request.queryParams("frame");            
            int candles = xUtils.stringToInt(request.queryParams("candles"));

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);

            CandlesData share = null;
            if (frame.equalsIgnoreCase("M30")){
                if (candles > 0){
                    share = _dataHistorical.getIntraday(shareId, symbol, candles, CandlesData.CANDLE_M30);
                }
                else{
                    share = _dataHistorical.getIntraday(shareId, symbol, market, date, time, CandlesData.CANDLE_M30);
                }
            }
            else if (frame.equalsIgnoreCase("M1")){
                if (_dataHistoricalM1 != null){
                    if (candles > 0){
                        share = _dataHistoricalM1.getIntraday(shareId, symbol, candles, CandlesData.CANDLE_M1);
                    }
                    else{
                        share = _dataHistoricalM1.getIntraday(shareId, symbol, market, date, time, CandlesData.CANDLE_M1);
                    }
                }
                else{
                    if (candles > 0){
                        share = _dataHistorical.getIntraday(shareId, symbol, candles, CandlesData.CANDLE_M1);
                    }
                    else{
                        share = _dataHistorical.getIntraday(shareId, symbol, market, date, time, CandlesData.CANDLE_M1);
                    }
                }
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
            ArrayList<Priceboard> arr = null;
            if (ss.length == 0 && symbols.compareTo("*") == 0){
                arr = _dataHistoricalM1.getPriceboard(null);
            }
            else{
                ArrayList<String> arrSymb = new ArrayList<>();
                for (String sb: ss){
                    arrSymb.add(sb);
                }
                arr = _dataHistoricalM1.getPriceboard(arrSymb);
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
    
    //  type: D1: historical big
    //  type: D2: historical medium
    //  type: D3: historical small
    //  type: M5: intraday M5
    //  type: M10: intraday M10
    public void doGetPacked(Request request, Response response){
        try{
            String type = request.queryParams("type");
            
            if (type == null || type.length() == 0){
                response.status(404);
                return;
            }
            
            xDataInput di = null;
            String filename = "";
            
            if (type.charAt(0) == 'D'){
                di = _dataHistorical.getHistoricalDB(type);
            }
            else{
                di = _dataHistorical.getIntradayDB(type);
            }
            
            if (di != null){
                // Thiết lập header phản hồi
                response.type("application/octet-stream"); // Dùng cho file nhị phân chung
                response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                response.raw().setContentLength(di.available());
                
                OutputStream out = response.raw().getOutputStream();
                out.write(di.getBytes(), 0, di.available());

            }
            else{
                response.status(404);
            }
        }
        catch(Throwable e){
            response.status(404);
        }
    }    
}
