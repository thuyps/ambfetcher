/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.data;

import xframe.utils.xUtils;

/**
 *
 * @author thuyps
 */
public class Priceboard {
    public String _symbol;
    public float _price;
    public float _prevClose;
    public float _highest;
    public float _lowest;
    public float _open;
    
    public int _volume; //  total volume of the day
    public long _timeUpdate;
    public long _timeUpdateOpen;
    public int _fileId;
    public int _date;
    public int _time;
    
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
 
    public String toString(){
        String sd = xUtils.dateIntToStringYYYYMMDD(_date);
        String st = xUtils.timeIntToStringHHMMSS(_time);
        String s = String.format("%s %s %s %.3f %.3f %.3f :%.3f %.3f %d", sd, st,
                _symbol,
                _open, _highest, _lowest, _price, _prevClose, _volume
                );
        return s;
    }
}
