/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.soft123.amifetcher;

/**
 *
 * @author thuyps
 */
public class stRecord {
    public stRecord(String sb, int fId){
            symbol = sb;
            fileId = fId;
        }
    public String symbol;
    public int fileId;
    public int marketId;
    public int shareId;
    public int recordSize;    
    public int totalField;
}
