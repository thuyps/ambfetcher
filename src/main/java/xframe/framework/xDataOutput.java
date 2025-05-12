/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xframe.framework;


import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author ThuyPham
 */
public class xDataOutput {

    byte[] mData;
    int mCursor;
    int mIncStep = 512;

    public xDataOutput(int initSize) {
        mData = new byte[initSize];
        mCursor = 0;
    }

    public xDataOutput(byte[] p) {
        if (p != null) {
            mData = p;
            mCursor = 0;
        }
    }
    public xDataOutput(byte[] p,int pos) {
        if (p != null) {
            mData = p;
            mCursor = pos;
        }
    }
    
    public xDataOutput(byte[] p,int off, int len) {
        if (p != null && len > 0) {
            mData = new byte[len];
            mCursor = len;
            System.arraycopy(p, off, mData, 0, len);
        }
    }    
    
    public void setSize(int size)
    {
    	if (size > size()){	
            increaseMemory(size);
        }
    }

    public void setIncreaseCapacity(int incCap) {
        mIncStep = incCap;
    }

    int availableRoom() {
        return mData.length - mCursor;
    }

    public void write(byte[] data) {
        if (data == null) {
            return;
        }

        write(data, 0, data.length);
    }

    void increaseMemory(int newLen) {
        byte[] p = new byte[newLen + mIncStep];
        if (mData != null && mCursor > 0) {
            System.arraycopy(mData, 0, p, 0, mCursor);
        }
        mData = p;
    }

    public void write(byte[] aByteData, int aOffset, int aLength) {
        if (availableRoom() < aLength) {
            int newSize = mData.length + aLength + mIncStep;
            byte[] p = new byte[newSize];

            System.arraycopy(mData, 0, p, 0, mCursor);

            mData = p;
        }

        //System.arraycopy(aByteData, aOffset, mData, mCursor, aLength);

        for (int i = 0; i < aLength; i++) {
            mData[mCursor + i] = aByteData[aOffset + i];
        }

        mCursor += aLength;
    }

    public void writeBoolean(boolean aBool) {
        if (aBool) {
            writeByte(1);
        } else {
            writeByte(0);
        }
    }

    public void writeByte(int aByte) {
        if (mCursor >= mData.length) {
            increaseMemory(mCursor + mIncStep);
        }
        mData[mCursor++] = (byte) aByte;
    }

    public void writeChars(String s) {
        byte[] p = s.getBytes();
        for (int i = 0; i < p.length; i++) {
            writeByte(p[i]);
        }
    }

    public void writeShort(int aShort) {
        // Write each of the two bytes, one at a time.
        writeByte((byte) ((aShort & 0xFF00) >> 8));
        writeByte((byte) (aShort & 0xFF));
    }

    public void writeInt(int aInt) {
        writeByte((byte) (aInt >> 24));
        writeByte((byte) (aInt >> 16));
        writeByte((byte) (aInt >> 8));
        writeByte((byte) (aInt & 0xFF));
    }
    
    public void writeIntAt(int aInt, int at){
        mData[at+0] = (byte)(aInt >> 24);
        mData[at+1] = (byte)(aInt >> 16);
        mData[at+2] = (byte)(aInt >> 8);
        mData[at+3] = (byte)(aInt & 0xFF);
    }

    public void writeFloat(float f){
        try{
            writeUTF(Float.toString(f));
        }catch(Throwable e){

        }
    }
    
    public void writeFloat2(float f){
        try{
        	int val = Float.floatToIntBits(f);
        	writeInt(val);
        }catch(Throwable e){

        }
    }
    
    public void writeDouble(double f){
        try{
            writeUTF(Double.toString(f));
        }catch(Throwable e){

        }
    }
    
    public void writeDouble2(double f){
        try{
            long l = Double.doubleToLongBits(f);
            writeLong(l);
        }catch(Throwable e){

        }
    }    

    public void writeLong(long aLong) {
        for (int i = 7; i >= 0; i--) {
            writeByte((byte) (aLong >> (8 * i)));
        }
    }

    public int writeWString(String str){
        if (str == null || str.equals("")) {
            writeShort(0);
            return 0;
        }
        int len = str.length();

        writeShort(len);
        increaseMemory(mCursor + 2*len);
        char c;
        for (int i = 0; i < len; i++){
            c = str.charAt(i);
            writeShort(c);
        }
        return 2*len + 2;
    }

    public int writeUTF(String str) {
        if (str == null || str.equals("")) {
            writeShort(0);
            return 0;
        }
        int strlen = str.length();
        int utflen = 0;
        int c;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if ((mData.length - mCursor) < (utflen + 2)) {
            increaseMemory(mCursor + utflen + 100);
        }


        mData[mCursor++] = (byte) ((utflen >>> 8) & 0xFF);
        mData[mCursor++] = (byte) ((utflen >>> 0) & 0xFF);

        int i = 0;
        for (i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) {
                break;
            }
            mData[mCursor++] = (byte) c;
        }

        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                mData[mCursor++] = (byte) c;

            } else if (c > 0x07FF) {
                mData[mCursor++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                mData[mCursor++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                mData[mCursor++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            } else {
                mData[mCursor++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                mData[mCursor++] = (byte) (0x80 | ((c >> 0) & 0x3F));
            }
        }
        return utflen + 2;
    }

    /*
    public void writeUTF_old(String aString) {
    if (aString == null) {
    writeShort(0);
    } else {
    int length = (int) aString.length();

    writeShort(length);
    byte[] p = aString.getBytes();
    for (int i = 0; i < p.length; i++) {
    writeByte(p[i]);
    }
    }
    }
     */
    public void write(xDataOutput data) {
        if (data == null) {
            return;
        }

        write(data.getBytes(), 0, data.size());
    }

    public byte[] getBytes() {
        return mData;
    }

    public int getCursor() {
        return mCursor;
    }

    public int size() {
        return mCursor;//getCursor();
    }

    public void reset() {
        mCursor = 0;
    }
    public void setCursor(int cursor){
        if (cursor >= 0 && cursor < mData.length){
            mCursor = cursor;
        }
    }
    
    public xDataInput createDataInput()
    {
    	byte[] p = getBytes();
    	return new xDataInput(p, 0, size());
    }

    public xDataInput decompress(){
        try{
            ByteArrayInputStream bi = new ByteArrayInputStream(getBytes(), 0, size());

            GZIPInputStream gis = new GZIPInputStream(bi);

            // copy GZIPInputStream to FileOutputStream
            byte[] buffer = new byte[1024];
            int len;
            xDataOutput o = new xDataOutput(size()*3);
            while ((len = gis.read(buffer)) > 0) {
                o.write(buffer, 0, len);
            }
            bi.close();
            gis.close();

            xDataInput di = new xDataInput(o.getBytes(), 0, o.size());
            return di;
        }
        catch(Throwable e){

        }
        return null;
    }

    @Override
    public String toString() {
        try {
            if (size() > 0) {
                return new String(getBytes(), 0, size(), "utf-8");
            }
        }catch (Throwable e){}

        return super.toString();
    }
}
