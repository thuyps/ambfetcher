/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.csv;

import com.data.CandlesData;
import com.data.DBHelper;
import com.data.Priceboard;
import com.data.VTDictionary;
import com.soft123.amifetcher.*;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPOutputStream;


/**
 *
 * @author thuyps
 */
public class CSVDataFetcher {
    String _folder;
    String _packedFolder;

    VTDictionary _priceboardMap;
    VTDictionary _symbolsOfMarket;
    VTDictionary _gzipPackedDB;

    VTDictionary _majorSymbolsDict = new VTDictionary();
    VTDictionary _mapSymbol2Stockchart;

    long _timeUpdatePriceboard;

    public CSVDataFetcher(String folder){
        _folder = folder;
        _priceboardMap = new VTDictionary();
        _symbolsOfMarket = new VTDictionary();
        
        _gzipPackedDB = new VTDictionary();
        _majorSymbolsDict = VTDictionary.loadFromFile(null, "majorsymbols2.txt");
        _mapSymbol2Stockchart = _majorSymbolsDict.objectForKeyAsDictionary("SymbolMap");

        updatePriceboard();
        
        xUtils.trace("DB DataFetcher: " + _folder);

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
        long now = System.currentTimeMillis();
        long elapsed = now - _timeUpdatePriceboard;
        if (elapsed > 3*1000){
            updatePriceboard();
            _timeUpdatePriceboard = now;
        }

        ArrayList<Priceboard> priceboars = new ArrayList<>();
        for (String market: markets){
            ArrayList<Priceboard> major;
                ArrayList<String> symbols = symbolsOfMarketMajor(market);
                if (symbols != null && symbols.size() > 0){
                    major = getPriceboard(symbols);
                    priceboars.addAll(major);
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
            if (ps != null){

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
        CandlesData share = getHistoricalData(shareId, symbol, market, date, 0, CandlesData.CANDLE_DAILY);
        return share;
    }
    
    public CandlesData getHistory(int shareId, String symbol, int candles){
        return getHistoricalData(shareId, symbol, 0, 0, 0, CandlesData.CANDLE_DAILY);
    }
    
    //  intraday: candle type: M5, M1, M30
    public CandlesData getIntraday(int shareId, String symbol, int market, int candles, int candleType) {
        return getHistoricalData(shareId, symbol, market, candles, candleType);
    }
    public CandlesData getIntraday(int shareId, String symbol, int market, int date, int time, int candleType) {
        return getHistoricalData(shareId, symbol, market, date, time, candleType);
    }

    CandlesData getHistoricalData(int shareId, String symbol, int market, int date, int time, int candleType){
        CandlesData share = null;

        String filename;
        if (candleType == CandlesData.CANDLE_M1){
            filename = String.format("%s_M1.CSV", symbol);
        }
        else if (candleType == CandlesData.CANDLE_M5){
            filename = String.format("%s_M5.CSV", symbol);
        }
        else if (candleType == CandlesData.CANDLE_M30){
            filename = String.format("%s_M30.CSV", symbol);
        }
        else{
            filename = String.format("%s_D1.CSV", symbol);
        }

        share = new CandlesData(shareId, symbol, market);

        readShareData(share, filename, date, time);

        return share;
    }

    CandlesData getHistoricalData(int shareId, String symbol, int market, int candles, int candleType){
        CandlesData share = null;

        String filename;
        if (candleType == CandlesData.CANDLE_M1){
            filename = String.format("%s_M1.CSV", symbol);
        }
        else if (candleType == CandlesData.CANDLE_M5){
            filename = String.format("%s_M5.CSV", symbol);
        }
        else if (candleType == CandlesData.CANDLE_M30){
            filename = String.format("%s_M30.CSV", symbol);
        }
        else{
            filename = String.format("%s_D1.CSV");
        }

        share = new CandlesData(shareId, symbol, market);

        readShareData(share, filename, 0, 0);
        if (share.getCandleCnt() > candles){
            share = share.cloneWithCandleCnt(candles);
        }

        return share;
    }

    void readShareData(CandlesData share, String csvFilepath, int startDate, int startTime)
    {
        String csv = xFileManager.readFileAsString(_folder, csvFilepath);
        //DateTime,Open,High,Low,Close,Volume,TickVolume
        //2025.09.22 07:30:00,3713.528,3716.353,3712.409,3713.287,5172,0

        // Định dạng (format) thời gian khớp với dữ liệu: "yyyy.MM.dd HH:mm:ss"
        // Sử dụng Locale.ROOT để đảm bảo parsing hoạt động độc lập với cài đặt hệ thống.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

        // Sử dụng Scanner để đọc chuỗi CSV theo từng dòng (line)
        try (Scanner scanner = new Scanner(csv)) {

            // **Bỏ qua dòng tiêu đề (header)** nếu nó tồn tại
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            // Vòng lặp chính để xử lý từng dòng dữ liệu
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Tách các giá trị trong dòng bằng dấu phẩy (,)
                String[] tokens = line.split(",");

                // Phải đảm bảo dòng dữ liệu có đủ 7 trường như định dạng CSV
                if (tokens.length >= 6) {
                    try {
                        String dateTimeString = tokens[0];
                        LocalDateTime date = LocalDateTime.parse(dateTimeString, formatter);
                        int yyyy = date.getYear(); // Kết quả: 2025
                        int mm = date.getMonthValue(); // Kết quả: 9
                        int dd = date.getDayOfMonth(); // Kết quả: 22
                        int h = date.getHour(); // Kết quả: 7
                        int m = date.getMinute(); // Kết quả: 30
                        int s = date.getSecond(); // Kết quả: 0

                        int dateInt = xUtils.CREATE_DATE(yyyy, mm, dd);
                        int timeInt = xUtils.CREATE_TIME(h, m, s);

                        if (dateInt < startDate){
                            continue;
                        }
                        if (dateInt == startDate && timeInt < startTime){
                            continue;
                        }

                        // 2. Phân tích các giá trị nến (Không đổi)
                        // Các trường O, H, L, C, V, T sẽ bắt đầu từ tokens[1]
                        float O = Float.parseFloat(tokens[1]); // Open
                        float H = Float.parseFloat(tokens[2]); // High
                        float L = Float.parseFloat(tokens[3]); // Low
                        float C = Float.parseFloat(tokens[4]); // Close

                        // 3. Phân tích Volume
                        double V = Double.parseDouble(tokens[5]); // Volume (tokens[5])

                        // 4. Thêm nến vào đối tượng Share
                        int dt = dateInt;
                        if (share.candleFrame < CandlesData.CANDLE_DAILY){
                            dt = packDate30m(dateInt, timeInt);
                        }

                        share.addCandle(C, O, H, L, dt, (int)V);

                    } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                    }
                }
            }
        }
        catch (Throwable e) {
            // Xử lý các lỗi khác như lỗi khởi tạo Scanner
            System.err.println("Đã xảy ra lỗi không xác định khi xử lý CSV: " + e.getMessage());
        }
    }

    void updatePriceboard(){
        String csv = null;

        try {
            csv = xFileManager.readFileAsString(_folder, "Priceboard.csv");

            //csv = Files.readString(Paths.get());
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        if (csv == null){
            return;
        }

        //  0,      1,      2,  3,    4,    5,  6,            7
        //Symbol,DateTime,Price,Open,Low,High,PreviousClose,Volume
        //XAUUSD,2025.11.21 00:00:00,4097.842,4079.235,4022.32,4099.9,4079.186,437686.0

        // Định dạng (format) thời gian khớp với dữ liệu: "yyyy.MM.dd HH:mm:ss"
        // Sử dụng Locale.ROOT để đảm bảo parsing hoạt động độc lập với cài đặt hệ thống.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

        // Sử dụng Scanner để đọc chuỗi CSV theo từng dòng (line)
        try (Scanner scanner = new Scanner(csv)) {

            // **Bỏ qua dòng tiêu đề (header)** nếu nó tồn tại
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            // Vòng lặp chính để xử lý từng dòng dữ liệu
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Tách các giá trị trong dòng bằng dấu phẩy (,)
                String[] tokens = line.split(",");

                // Phải đảm bảo dòng dữ liệu có đủ 7 trường như định dạng CSV
                if (tokens.length >= 8) {
                    try {
                        String symbol = tokens[0];

                        String dateTimeString = tokens[1];
                        LocalDateTime date = LocalDateTime.parse(dateTimeString, formatter);
                        int yyyy = date.getYear(); // Kết quả: 2025
                        int mm = date.getMonthValue(); // Kết quả: 9
                        int dd = date.getDayOfMonth(); // Kết quả: 22
                        int h = date.getHour(); // Kết quả: 7
                        int m = date.getMinute(); // Kết quả: 30
                        int s = date.getSecond(); // Kết quả: 0

                        int dateInt = xUtils.CREATE_DATE(yyyy, mm, dd);
                        int timeInt = xUtils.CREATE_TIME(h, m, s);

                        //  0,      1,      2,  3,    4,    5,  6,            7
                        //Symbol,DateTime,Price,Open,Low,High,PreviousClose,Volume

                        // 2. Phân tích các giá trị nến (Không đổi)
                        // Các trường O, H, L, C, V, T sẽ bắt đầu từ tokens[1]
                        float C = Float.parseFloat(tokens[2]); // Close
                        float O = Float.parseFloat(tokens[3]); // Open
                        float L = Float.parseFloat(tokens[4]); // High
                        float H = Float.parseFloat(tokens[5]); // Low
                        float PC = Float.parseFloat(tokens[6]);


                        // 3. Phân tích Volume
                        double V = Double.parseDouble(tokens[7]); // Volume (tokens[5])

                        Priceboard ps = new Priceboard(symbol);
                        ps._date = dateInt;
                        ps._prevClose = PC;
                        ps.setClose(C);
                        ps._open = O;
                        ps._highest = H;
                        ps._lowest = L;
                        ps._volume = (int)V;
                        ps._time = timeInt;

                        ps._symbol = symbol;

                        _priceboardMap.setValue(ps, ps._symbol);

                    } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                    }
                }
            }
            _timeUpdatePriceboard = System.currentTimeMillis();
        }
        catch (Throwable e) {
            // Xử lý các lỗi khác như lỗi khởi tạo Scanner
            System.err.println("Đã xảy ra lỗi không xác định khi xử lý CSV: " + e.getMessage());
        }
    }

    static public int packDate30m(int dateInt, int timeInt)
    {
        //  year: 0-99 => 8 bytes
        //  month: 0-12: 4 bytes
        //  days: 0-31: 6 bytes
        //  hour: 0-24: 6 bytes
        //  minutes: 0-60: 8 bytes

        //  year(8:24) | month(4:20) | day(6:14) | hour(6:8) | minute(8:0)

        int year = xUtils.EXTRACT_YEAR(dateInt);
        int month = xUtils.EXTRACT_MONTH(dateInt);
        int day = xUtils.EXTRACT_DAY(dateInt);
        int hour = xUtils.EXTRACT_HOUR(timeInt);
        int minute = xUtils.EXTRACT_MINUTE(timeInt);
/*
        int packedInt = (year << 24)
                | ((month&0xf)<<20) //  1111
                | ((day&0x3f)<<14)        //  111111
                | ((hour&0x3f)<<8)        //  111111
                | (minute&0xff);

 */
        int M = (year - 2020)*12 + (month-1);
        int packedInt =
                (M<<24) //  1111
                        | (day<<16)        //  111111
                        | (hour<<8)        //  111111
                        | (minute&0xff);

        return packedInt;
    }
}
