/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.data;

import java.util.Date;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.utils.xUtils;

/**
 *
 * @author Administrator
 */
public class CandlesData {
    public static final int CANDLE_DAILY = 1000;
    public static final int CANDLE_M1 = 1;
    public static final int CANDLE_M5 = 5;
    public static final int CANDLE_M30 = 30;
    
    static int CANDLE_SIZE = 28;
    public String symbol;
    public int shareId;
    public int marketId;
    public float factor;
    public float close[];
    public float open[];
    public float hi[];
    public float lo[];
    public int date[];
    public int volume[];
    public long timeLastUpdate;
    public int firsDate;
    public long timeCreated;
    public boolean isIntraday;
    
    public int candleFrame;

    int cursor;
    public CandlesData(int shareId, String symbol, int market){
        this.shareId = shareId;
        this.symbol = symbol;
        this.marketId = market;
        this.isIntraday = false;
        
        int candleCnt = 2000;
        prepare(candleCnt);
    }
    
    public CandlesData(int shareId, String symbol, int market, int candleCnt){
        this.shareId = shareId;
        this.symbol = symbol;
        this.marketId = market;
        
        prepare(candleCnt+512);
    }
    
    void prepare(int candleCnt){
        if (candleCnt < 128){
            candleCnt = 128;
        }
        
        close = new float[candleCnt];
        open = new float[candleCnt];
        hi = new float[candleCnt];
        lo = new float[candleCnt];
        date = new int[candleCnt];
        volume = new int[candleCnt];
    }

    public void addCandle(float c, float o, float h, float l, int d, int v){
        if (close.length <= cursor){
            int newLen = cursor + 1000;
            float pC[] = new float[newLen];
            float pO[] = new float[newLen];
            float pH[] = new float[newLen];
            float pL[] = new float[newLen];
            
            int pD[] = new int[newLen];
            int pV[] = new int[newLen];
            
            System.arraycopy(open, 0, pO, 0, cursor);
            System.arraycopy(close, 0, pC, 0, cursor);
            System.arraycopy(hi, 0, pH, 0, cursor);
            System.arraycopy(lo, 0, pL, 0, cursor);
            System.arraycopy(date, 0, pD, 0, cursor);
            System.arraycopy(volume, 0, pV, 0, cursor);
         
            open = pO;
            close = pC;
            hi = pH;
            lo = pL;
            date = pD;
            volume = pV;
        }
        
        close[cursor] = c;
        open[cursor] = o;
        hi[cursor] = h;
        lo[cursor] = l;
        date[cursor] = d;
        volume[cursor] = v;
        cursor++;
    }
    
    public void log(){
        for (int i = 0; i < cursor; i++){
            String sd;
            if (isIntraday){
                int d = xUtils.dateFromPackagedDate(date[i]);
                int t = xUtils.timeFromPackagedDate(date[i]);
                sd = String.format("%s %s", 
                        xUtils.dateIntToStringDDMMYY(d),
                        xUtils.timeIntToStringHHMM(t));
            }
            else{
                sd = String.format("%s", xUtils.dateIntToStringDDMMYY(date[i]));
            }
            
            String s = String.format("%s: %.2f/%.2f/%.2f/%.2f; V=%d", 
                        sd,
                        open[i], hi[i], lo[i], close[i], volume[i]);    
            xUtils.trace(s);            
        }
    }
    
    public void reverse(){
        for (int i = 0; i < cursor/2; i++){
            int last = cursor - 1 - i;
            float c = close[i]; close[i] = close[last]; close[last] = c;
            c = open[i]; open[i] = open[last]; open[last] = c;
            c = hi[i]; hi[i] = hi[last]; hi[last] = c;
            c = lo[i]; lo[i] = lo[last]; lo[last] = c;
            int n = date[i]; date[i] = date[last]; date[last] = n;
            n = volume[i]; volume[i] = volume[last]; volume[last] = n;
        }
    }
    
    public void updateLastCandles(CandlesData news)
    {
        if (news.getCandleCnt() > 0){
            removeCandlesIncludeDate(news.date[0]);
        }
        for (int i = 0; i < news.getCandleCnt(); i++){
            addCandle(news.close[i], news.open[i], news.hi[i], news.lo[i], news.date[i], news.volume[i]);
        }
        
        timeLastUpdate = System.currentTimeMillis();
    }
    
    public void removeCandlesIncludeDate(int includeDate)
    {
        for (int i = cursor-1; i >= 0; i--)
        {
            if (date[i] >= includeDate){
                //  cursor will point at includeDate -> will be override
                cursor = i;
            }
            else{
                break;
            }
        }
    }
    
    public int getCandleCnt(){
        return cursor;
    }
    
    public int measureDataSize(){
        return cursor*(6*4) + 12 + 8 + 32;  //  32: future
    }
    
    public void clear(){
        cursor = 0;
    }
    
    public CandlesData clone(){
        CandlesData c = new CandlesData(shareId, this.symbol, this.marketId);
        c.prepare(this.cursor);
        
        c.symbol = this.symbol;
        c.marketId = this.marketId;
        c.factor = this.factor;
        c.cursor = this.cursor;
        c.timeCreated = this.timeCreated;
        
        System.arraycopy(open, 0, c.open, 0, cursor);
        System.arraycopy(close, 0, c.close, 0, cursor);
        System.arraycopy(hi, 0, c.hi, 0, cursor);
        System.arraycopy(lo, 0, c.lo, 0, cursor);
        System.arraycopy(date, 0, c.date, 0, cursor);
        System.arraycopy(volume, 0, c.volume, 0, cursor);
        
        return c;
    }
    
    public CandlesData cloneFromDate(int from){
        CandlesData c = new CandlesData(shareId, this.symbol, this.marketId);
        c.marketId = this.marketId;
        c.symbol = this.symbol;
        c.factor = this.factor;
        c.timeCreated = this.timeCreated;
        c.timeLastUpdate = this.timeLastUpdate;
        int j = 0;
        for (int i = 0; i < cursor; i++){
            if (date[i] >= from){
                j = i-3;
                if (j < 0){
                    j = 0;
                }
                break;
            }
        }
        int candleCnt = cursor - j;
        if (candleCnt > 0){
            c.prepare(candleCnt);
        }
        for (; j < cursor; j++){
            c.addCandle(close[j], open[j], hi[j], lo[j], date[j], volume[j]);
        }
        c.cursor = candleCnt;
        
        return c;
    }

    public CandlesData cloneWithCandleCnt(int candleCnt){
        CandlesData c = new CandlesData(shareId, this.symbol, this.marketId);
        c.marketId = this.marketId;
        c.symbol = this.symbol;
        c.factor = this.factor;
        c.timeCreated = this.timeCreated;
        c.timeLastUpdate = this.timeLastUpdate;
        int j = cursor - candleCnt;
        if (j < 0){
            j = 0;
        }
        candleCnt = cursor-j;
        if (candleCnt > 0){
            c.prepare(candleCnt);
        }
        for (; j < cursor; j++){
            c.addCandle(close[j], open[j], hi[j], lo[j], date[j], volume[j]);
        }
        c.cursor = candleCnt;
        
        return c;
    }
    
    public int lastDate(){
        if (cursor > 0){
            return date[cursor-1];
        }
        return 0;
    }
    
    public int firstDate(){
        return firsDate;
    }
    
    public int avgVolume(){
        if (cursor > 20){
            double v = 0;
            for (int i = 0; i < 5; i++){
                v += volume[cursor-1-i];
            }
            return (int)(v/5);
        }
        
        return 0;
    }
    
    static public int dateFromSqlDate(java.sql.Date date)
    {
        try{
            return xUtils.getDateAsInt(new Date(date.getTime()));
        }
        catch(Throwable e){
            
        }
        
        return 0;
    }
    
    public boolean isExpired(){
        long now = System.currentTimeMillis();
        long elapsed = now - timeCreated;
        elapsed /= 1000;
        return elapsed > (4*3600);  //  4hours
    }
    
    public void writeTo(xDataOutput aOut){
        aOut.writeUTF(symbol);
        int cnt = cursor;
        aOut.writeInt(cnt);
        for (int i = 0; i < cnt; i++){
            aOut.writeInt(date[i]);
            //  o/c/l/h/v/d
            aOut.writeFloat2(open[i]);
            aOut.writeFloat2(close[i]);
            aOut.writeFloat2(lo[i]);
            aOut.writeFloat2(hi[i]);
            aOut.writeInt(volume[i]);
        }
    }
    
    public void writeTo2(xDataOutput aOut){
        aOut.writeUTF(symbol);
        aOut.writeInt(candleFrame);
        int cnt = cursor;
        aOut.writeInt(cnt);
        for (int i = 0; i < cnt; i++){
            aOut.writeInt(date[i]);
            //  o/c/l/h/v/d
            aOut.writeFloat2(open[i]);
            aOut.writeFloat2(close[i]);
            aOut.writeFloat2(lo[i]);
            aOut.writeFloat2(hi[i]);
            aOut.writeInt(volume[i]);
        }
    }
    
    public xDataOutput writeTo(){
        xDataOutput o = new xDataOutput(64 + cursor * CANDLE_SIZE);
        
        /*
        o.writeShort(12);   //  FILE_SHARE_DATA_VERSION
        o.writeUTF(symbol);
        o.writeByte(marketId);
        o.writeInt(cursor);
        for (int i = 0; i < cursor; i++)
        {
            o.writeFloat2(open[i]);
            o.writeFloat2(close[i]);
            o.writeFloat2(hi[i]);
            o.writeFloat2(lo[i]);
            o.writeFloat2(0);

            //o.writeInt(0);  //  ce value, not used
            o.writeInt(volume[i]);
            o.writeInt(date[i]);
        }        
        */
        writeTo(o);
        
        return o;
    }
    
    public void writeToOutputForPacking(xDataOutput o, int candles){        
        o.writeUTF(symbol);
        int candleCnt = candles < cursor?candles:cursor;
        int begin = cursor - candles;
        if (begin < 0){
            begin = 0;
        }
        o.writeInt(candleCnt);
        for (int i = 0; i < candleCnt; i++)
        {
            int idx = begin + i;
            o.writeInt(date[idx]);
            
            o.writeFloat2(open[idx]);
            o.writeFloat2(close[idx]);
            o.writeFloat2(hi[idx]);
            o.writeFloat2(lo[idx]);

            o.writeInt(volume[idx]);
        }
    }
    
    static public void readFromPacked(xDataInput di, CandlesData share)
    {
        try{
            share.symbol = di.readUTF();
            int candleCnt = di.readInt();
            share.prepare(candleCnt);
            for (int i = 0; i < candleCnt; i++)
            {
                share.date[i] = di.readInt();
                
                share.open[i] = di.readFloat2();
                share.close[i] = di.readFloat2();
                share.hi[i] = di.readFloat2();
                share.lo[i] = di.readFloat2();
                
                share.volume[i] = di.readInt();
            }
        }
        catch(Throwable e){
            
        }
    }
    
    public int getAvgVol(int days){
        int begin = cursor - days;
        if (cursor == 0){
            return 0;
        }
        if (begin < 0 || days == 0){
            return volume[cursor-1];
        }
        int cnt = 0;
        int vol = 0;
        for (int i = begin; i < cursor; i++){
            vol += volume[i];
            cnt++;
        }
        
        return vol/cnt;
    }
    
    public void changeCandleType(int minutesPerCandle)
    {
        if (minutesPerCandle <= 1){
            return;
        }
        if (!isIntraday){
            return;
        }
        
        //====================================
        
        int cnt = getCandleCnt();
        int FIRST_MARK      = 0xabcdabcd;
        int last = FIRST_MARK;

        if (cnt < 4){
            return;
        }

        int D = 0;
        int V = 0;
        float C = 0;
        float H = 0;
        float L = 0;
        float O = 0;
        int j = 0;

        for (int i = 0; i < cnt; i++)
        {
            int ndate = this.date[i];
            int dateInt = xUtils.dateFromPackagedDate(ndate);
            int timeInt = xUtils.timeFromPackagedDate(ndate);
            int hh = xUtils.EXTRACT_HOUR(timeInt);
            int mm = xUtils.EXTRACT_MINUTE(timeInt);


            int MM = hh*60+mm;
            int idx = (MM+5)/60;    //  1h

            //  2h
            if (minutesPerCandle == 5){
                idx = (MM-1)/5;    //  5
            }
            else if (minutesPerCandle == 10){
                idx = (MM-1)/10;    //  10
            }
            else if (minutesPerCandle == 15){
                idx = (MM-1)/15;    //  15
            }
            else if (minutesPerCandle == 30){
                idx = (MM-1)/30;    //  1h
            }
            else if (minutesPerCandle == 60){
                idx = (MM-1)/60;    //  1h
            }

            if (idx != last) //  new hour
            {
                if (last != FIRST_MARK)    //  new candle
                {
                    close[j] = C;
                    date[j] = D;
                    hi[j] = H;
                    lo[j] = L;
                    open[j] = O;
                    volume[j] = V;

                    j++;
                }

                O = open[i];
                H = hi[i];
                L = lo[i];
                C = close[i];
                D = date[i];
                V = volume[i];

                last = idx;
            }
            else{
                V += volume[i];
                D = date[i];
                C = close[i];
                if (lo[i] < L) L = lo[i];
                if (hi[i] > H) H = hi[i];

                last = idx;
            }

        }// end of for
        //--------------
        if (j < cnt)
        {
            close[j] = C;
            date[j] = D;
            hi[j] = H;
            lo[j] = L;
            open[j] = O;
            volume[j] = V;

            j++;
        }
        //--------------
        cursor = j;
    }
}
