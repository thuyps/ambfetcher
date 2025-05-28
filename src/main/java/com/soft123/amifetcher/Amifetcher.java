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
       
        // Đăng ký hook shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Đang dừng Spark...");
            try{
                spark.Spark.stop();
            }catch(Throwable e){}
            System.out.println("Spark đã dừng.");
        }));
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

        System.out.println("Fetcher running at: http://localhost:2610");        
    }
    
    public Amifetcher(){
        _dataHistorical = new DataFetcher("./datafetpro");  //  datapro
        _dataHistorical.setPackedFolder(PACKED_FOLDER);
        _dataHistorical.setup(true, true);
        
        //_dataHistoricalM1 = new DataFetcher("./datatick");
        //_dataHistoricalM1.setup(false, false);
    }

    //  daily only
    //  http://localhost:4567/history?symbol=VNM&id=233&mid=0&candles=&date=20250303&frame=D/M1/M5/M30&ver=
    public void doGetHistory(Request request, Response response){
        try{
            String sver = request.queryParams("ver");
            int version = xUtils.stringToInt(sver);
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("date");
            int date = xUtils.stringToInt(startDate);
            
            int candles = xUtils.stringToInt(request.queryParams("candles"));
            String frame = request.queryParams("frame");

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);
            
            //===============================
            if (frame != null && frame.contains("M")){
                doGetIntraday(request, response);
                return;
            }
            //===============================

            CandlesData share = null;
            if (candles > 0){
                share = _dataHistorical.getHistory(shareId, symbol, candles);
            }
            else{
                share = _dataHistorical.getHistory(shareId, symbol, market, date);
            }
            
            if(share != null){
                xDataOutput o = new xDataOutput(share.measureDataSize());
                if (version == 2){
                    share.writeTo2(o);
                }
                else{
                    share.writeTo(o);
                }
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
    //  http://localhost:4567/intraday?symbol=VNM&id=233&mid=0&frame=M1&candles=&date=20250503&time=1050
    public void doGetIntraday(Request request, Response response){
        try{
            String sver = request.queryParams("ver");
            int version = xUtils.stringToInt(sver);
            String symbol = request.queryParams("symbol");
            String startDate = request.queryParams("date");
            String startTime = request.queryParams("time");
            int date = xUtils.stringToInt(startDate);
            int time = xUtils.stringToInt(startTime);
            String frame = request.queryParams("frame");            
            int candles = xUtils.stringToInt(request.queryParams("candles"));

            String sid = request.queryParams("id");
            int shareId = xUtils.stringToInt(sid);

            sid = request.queryParams("mid");
            int market = xUtils.stringToInt(sid);

            CandlesData share = null;
            if (frame.equalsIgnoreCase("M30") || frame.equalsIgnoreCase("M5")){
                int candleFrame = frame.equalsIgnoreCase("M30")?CandlesData.CANDLE_M30:CandlesData.CANDLE_M5;
                if (candles > 0){
                    share = _dataHistorical.getIntraday(shareId, symbol, candles, candleFrame);
                }
                else{
                    share = _dataHistorical.getIntraday(shareId, symbol, market, date, time, candleFrame);
                }
                if (share != null){
                    share.candleFrame = candleFrame;
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
                if (share != null){
                    share.candleFrame = CandlesData.CANDLE_M1;
                }
            }
            
            if(share != null){
                xDataOutput o = new xDataOutput(share.measureDataSize());
                if (version == 2){
                    share.writeTo2(o);
                }
                else{
                    share.writeTo(o);
                }
                
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
    
    xDataOutput _priceboardOfAll;
    long _timeUpdatePriceboardOfAll;
    //  return_type: 0: as string; 1: binary
    //  http://localhost:4567/priceboard?symbols=VNM,HAG,XAU/USD&return_type=1
    public void doGetPriceboard(Request request, Response response){
        try{
            String symbols = request.queryParams("symbols");
            int returnType = xUtils.stringToInt(request.queryParams("return_type"));
            
            String ss[] = symbols.split("[,]");
            ArrayList<Priceboard> arr = null;
            if (symbols.compareTo("*") == 0){
                if (returnType == 1 && _priceboardOfAll != null && _priceboardOfAll.size() > 0){
                    if (System.currentTimeMillis() - _timeUpdatePriceboardOfAll < 5000){
                        writeDataOutputToResponse(_priceboardOfAll, response);
                        return;
                    }
                }
                //---------------------
                
                arr = _dataHistorical.getPriceboard(null);
            }
            else{
                ArrayList<String> arrSymb = new ArrayList<>();
                for (String sb: ss){
                    arrSymb.add(sb);
                }
                arr = _dataHistorical.getPriceboard(arrSymb);
            }
            
            if(arr != null && arr.size() > 0){
                if (returnType == 0){
                    StringBuffer sb = new StringBuffer();

                    for (Priceboard ps: arr)
                    {
                        sb.append(ps.toStringWithDateEncoded());
                        sb.append('\n');
                    }

                    //  write to the response
                    response.type("text/plain");

                    String s = sb.toString();
                    response.raw().getOutputStream().write(s.getBytes());
                }
                else{
                    //  symbol(16) + date(4) + time(4) + o/h/l/c + chg + v
                    int size = arr.size()*(16+4+4+4*4+4+4);
                    xDataOutput o = new xDataOutput(4+size);
                    o.writeInt(arr.size());
                    for (Priceboard ps: arr)
                    {
                        ps.writeTo(o);
                    }
                    
                    _priceboardOfAll = o;
                    _timeUpdatePriceboardOfAll = System.currentTimeMillis();
                    
                    writeDataOutputToResponse(o, response);
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
    
    void writeDataOutputToResponse(xDataOutput o, Response response)
    {
        if (o != null && o.size() > 0){
            response.type("application/octet-stream");

            String filename = "priceboard";
            response.header("Content-Disposition", "inline; filename=\"" + filename + "\"");
            response.raw().setContentLength((int) o.size());

            try (OutputStream os = response.raw().getOutputStream()) {
                os.write(o.getBytes(), 0, o.size());
            }
            catch(Throwable throwable){}
        }
    }
    
    //  frame: 5/30/1000
    //  candles
    public void doGetPacked(Request request, Response response){
        try{
            int frame = xUtils.stringToInt(request.queryParams("frame"));
            int candles = xUtils.stringToInt(request.queryParams("candles"));
            
            if (frame != CandlesData.CANDLE_DAILY 
                    && frame != CandlesData.CANDLE_M5 
                    && frame != CandlesData.CANDLE_M30){
                response.status(404);
                return;
            }
            
            xDataInput di = null;
            String filename = "";
            
            di = _dataHistorical.getHistoricalDB(frame, candles);
            
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
