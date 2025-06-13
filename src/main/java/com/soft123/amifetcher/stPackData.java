/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.soft123.amifetcher;

/**
 *
 * @author Administrator
 */
public class stPackData {
    public stPackData(String market){
            this.market = market;
            isZipped = false;
        }
    public void setData(byte[] data, boolean isZipped){
        this.isZipped = isZipped;
        createdTime = System.currentTimeMillis();
        gzipData = data;
    }
    public byte[] gzipData;
    public String market;
    public long createdTime;
    public boolean isZipped;
}
