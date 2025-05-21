/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.data;

import com.soft123.amifetcher.stRecord;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.utils.xUtils;

/**
 *
 * @author thuyps
 */
public class Priceboard {
    public String _symbol;
    private float _price;
    public float _prevClose;
    public float _highest;
    public float _lowest;
    public float _open;
    
    public int _volume; //  total volume of the day (/100)
    public long _timeUpdate;
    public long _timeUpdateOpen;
    
    public int _date;
    public int _time;
    
    public int _avgVolume;  //  *10
    public int _preDate;
    public int _todayDate;
    
    public stRecord recordD;    //  daily
    public stRecord recordI;    //  intraday
    
    public void setClose(float c){
        _price = c;
        _timeUpdate = System.currentTimeMillis();
    }
    public float getClose(){
        return _price;
    }
    public boolean isExpired(){
        return System.currentTimeMillis() - _timeUpdate > 30*1000;
    }
    
    public Priceboard(String symbol){
        _symbol = symbol;
    }
    
    public void reset(){
        _volume = 0;
        _open = 0;
        _highest = 0;
        _lowest = 0;
        _price = 0;
        _prevClose = 0;
    }
    
    public void writeTo(xDataOutput o){
        o.writeUTF(_symbol);
        
        o.writeInt(_date);
        o.writeInt(_time);
        
        o.writeFloat2(_open);
        o.writeFloat2(_highest);
        o.writeFloat2(_lowest);
        o.writeFloat2(_price);
        
        o.writeFloat2(_prevClose);
        
        o.writeInt(_volume);
    }
    
    static public void read(xDataInput di, Priceboard ps){
        ps._symbol = di.readUTF();
        ps._date = di.readInt();
        ps._time = di.readInt();
        
        ps._open = di.readFloat2();
        ps._highest = di.readFloat2();
        ps._lowest = di.readFloat2();
        ps._price = di.readFloat2();
        
        ps._prevClose = di.readFloat2();
        
        ps._volume = di.readInt();
    }
 
    public String toString(){
        String sd = xUtils.dateIntToStringYYYYMMDD(_date);
        String st = xUtils.timeIntToStringHHMMSS(_time);
        String s = String.format("%s %s %s %.3f %.3f %.3f :%.3f %.3f %d", sd, st,
                _symbol,
                _open, _highest, _lowest, _price, _prevClose, _volume
                );
        return s;
    }
    
    public String toStringWithDateEncoded(){
        String sd = "" + _date;
        String st = "" + _time;
        String s = String.format("%s %s %s %.3f %.3f %.3f %.3f %.3f %d", sd, st,
                _symbol,
                _open, _highest, _lowest, _price, _prevClose, _volume
                );
        return s;
    }
}
