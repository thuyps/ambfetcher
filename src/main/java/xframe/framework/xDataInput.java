/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xframe.framework;

import xframe.utils.xUtils;


/**
 *
 * @author ThuyPham
 */
public class xDataInput {

    byte[] mData;
    int mNumBytesRead = 0;
    int mSize;
    int mCapacity;

    public xDataInput(int initCap) {
        mCapacity = initCap;
        mNumBytesRead = 0;
        mSize = 0;
        if (mCapacity > 0) {
            mData = new byte[mCapacity];
        }
    }

    public xDataInput(byte[] aBytes, int start, int len) {
        if (len <= 0) {
            return;
        }

        mData = new byte[len];
        mCapacity = len;
        mSize = len;

        System.arraycopy(aBytes, start, mData, 0, len);
    }

    public int size() {
        return mSize;
    }

    public int available() {
        return mSize - mNumBytesRead;
    }

    public boolean readBoolean() {
        int b = readByte();

        return b > 0;
    }

    public boolean readBoolean4() {
        int t = readInt();

        return t != 0;
    }

    public byte readByte() {
        if (available() < 1) {
            return (byte) 0;
        }

        int b = mData[mNumBytesRead++];
        if (b < 0) {
            b = 256 + b;
        }

        return (byte) b;
    }
    
    public int readUByte() {
        if (available() < 1) {
            return (byte) 0;
        }

        int b = mData[mNumBytesRead++];
        if (b < 0) {
            b = 256 + b;
        }

        return b;
    }

    public String readUTF() {
        return readUTFString();
    }

    public String readTerminatedString(int maxStringLen)
    {
        int cnt = 0;

        int j = mNumBytesRead;
        int pos = mNumBytesRead;
        for (int i = 0; i < maxStringLen; i++)
        {
            byte c = mData[j++];
            if (c == 0){
                break;
            }
            else{
                cnt++;
            }
        }
        mNumBytesRead += maxStringLen;

        if (cnt > 0){
            return new String(mData, pos, cnt);
        }

        return null;
    }

    public String readWString() {
        if (available() < 2) {
            return null;
        }

        // Find the length of the string.
        int length = readShort();
        sb.setLength(0);

        byte b0;
        byte b1;
        int t = 0;
        for (int i = 0; i < length; i++) {
//            char c = (char) readShort();
            if (mNumBytesRead + 1 < mData.length) {
                b0 = mData[mNumBytesRead];
                b1 = mData[mNumBytesRead + 1];
               t = (((256 + b0) & 0xff) << 8) | ((256 + b1) & 0xff);
               mNumBytesRead += 2;
           }        	
            
            sb.append((char)t);
        }

        return sb.toString();
    }

    static StringBuffer sb = new StringBuffer();
    static byte[] tmp = new byte[512];
    static final String utf = "UTF-8";

    public String readUTFString() {
        if (available() < 2) {
            return null;
        }

        // Find the length of the string.
        int length = readShort();

        if (available() < length) {
            return null;
        }

        // Get the characters of the string.
        String s = null;
        try {
            byte[] p;
            if (length < tmp.length) {
                p = tmp;
            } else {
                p = new byte[length];
            }
            for (int i = 0; i < length; i++) {
                p[i] = readByte();
            }

            //s = new String(p, 0, length, utf);
            s = xUtils.utf8ToUnicode(p, 0, length);
        } catch (Throwable e) {
        }

        if (s == null) {
            s = "";
        }
        return s;
    }

    public void skipUTF() {
        if (available() < 2) {
            return;
        }

        // Find the length of the string.
        int length = readShort();
        if (length > 0) {
            skip(length);
        }
    }

    public short readShort() {
        if (available() < 2) {
            return 0;
        }

        int t = 0;
        if (mNumBytesRead + 1 < mData.length) {
            byte b0 = mData[mNumBytesRead];
            byte b1 = mData[mNumBytesRead + 1];
            t = (((256 + b0) & 0xff) << 8) | ((256 + b1) & 0xff);
            mNumBytesRead += 2;
        }
        return (short) t;
    }
    

    public int readIntFromC() {
        if (available() < 4) {
            return 0;
        }

        if (mNumBytesRead + 3 < mData.length) {
            byte b3 = mData[mNumBytesRead];
            byte b2 = mData[mNumBytesRead + 1];
            byte b1 = mData[mNumBytesRead + 2];
            byte b0 = mData[mNumBytesRead + 3];
            int t = (((256 + b0) & 0xff) << 24)
                    | (((256 + b1) & 0xff) << 16)
                    | (((256 + b2) & 0xff) << 8)
                    | ((256 + b3) & 0xff);

            mNumBytesRead += 4;
            return t;

        }else
        	return 0;
    }
    public int readInt() {
        if (available() < 4) {
            return 0;
        }

        if (mNumBytesRead + 3 < mData.length) {
            byte b0 = mData[mNumBytesRead];
            byte b1 = mData[mNumBytesRead + 1];
            byte b2 = mData[mNumBytesRead + 2];
            byte b3 = mData[mNumBytesRead + 3];
            int t = (((256 + b0) & 0xff) << 24)
                    | (((256 + b1) & 0xff) << 16)
                    | (((256 + b2) & 0xff) << 8)
                    | ((256 + b3) & 0xff);

            mNumBytesRead += 4;
            return t;

        }
        /*
        int t = (((256+mData[mNumBytesRead])&0xff) << 24)
        | (((256+mData[mNumBytesRead + 1])&0xff) << 16)
        | (((256+mData[mNumBytesRead + 2])&0xff) << 8)
        | ((256+mData[mNumBytesRead + 3])&0xff);
        
        mNumBytesRead += 4;
        return t;
         */ return 0;
    }

    public float readFloat() {
        try {
            String sf = readUTF();
            float f = Float.parseFloat(sf);

            return f;
        } catch (Throwable e) {
        }
        return 0;
    }
    
    public float readFloat2() {
        try {
        	int v = readInt();
        	float f = Float.intBitsToFloat(v);

            return f;
        } catch (Throwable e) {
        }
        return 0;
    }    
    
    public double readDouble() {
        try {
            String sf = readUTF();
            double f = Double.parseDouble(sf);
        	
            return f;
        } catch (Throwable e) {
        }
        return 0;
    }
    
    public double readDouble2() {
        try {
        	long l = readLong();
        	double f = Double.longBitsToDouble(l);
        	
            return f;
        } catch (Throwable e) {
        }
        return 0;
    }    

    public long readLong() {
        if (available() < 8) {
            return 0;
        }

        if (mNumBytesRead + 7 < mData.length) {
            byte[] b = new byte[8];
            for (int i = 0; i < 8; i++) {
                b[i] = mData[mNumBytesRead + i];
            }
            long t = 0;
            for (int i = 0; i < 8; i++) {
                t |= (long)((256 + b[i]) & 0xff) << (8 * (8 - i - 1));
            }
            mNumBytesRead += 8;
            return t;
        }
        return 0;
    }

    public int read(byte[] out, int off, int bytesToRead) {
        int len = bytesToRead;
        if (len > available()) {
            len = available();
        }

        System.arraycopy(mData, mNumBytesRead, out, off, len);
        mNumBytesRead += len;

        return len;
    }

    public void stepback(int num){
        mNumBytesRead -= num;
        if (mNumBytesRead < 0){
            mNumBytesRead = 0;
        }
    }

    public void skip(int skipLen) {
        if (skipLen == -1) {
            skipLen = available();
        }
        if (available() < skipLen) {
            skipLen = available();
        }

        mNumBytesRead += skipLen;
    }
    
    public void setCursor(int off){
        if (off >= size()){
            off = 0;
        }
        mNumBytesRead = off;
    }

    public int getCurrentOffset() {
        return mNumBytesRead;
    }

    public byte[] getStream() {
        return mData;
    }
    
    public byte[] getBytes() {
        return mData;
    }    

    public int readIntBig() {
        if (available() < 4) {
            return 0;
        }

        int tmp = (((256 + mData[mNumBytesRead + 3]) & 0xff) << 24)
                | (((256 + mData[mNumBytesRead + 2]) & 0xff) << 16)
                | (((256 + mData[mNumBytesRead + 1]) & 0xff) << 8)
                | ((256 + mData[mNumBytesRead]) & 0xff);

        mNumBytesRead += 4;

        return tmp;
    }

    public short readShortBig() {
        if (available() < 4) {
            return 0;
        }

        int tmp = (((256 + mData[mNumBytesRead + 1]) & 0xff) << 8) | ((256 + mData[mNumBytesRead]) & 0xff);

        mNumBytesRead += 2;

        return (short) tmp;
    }
    
    public int readUShortBig() {
        return readShortBig()&0xFFFF;
    }

    public void resetCursor() {
        mNumBytesRead = 0;
    }

    int mMarkCursor = 0;
    public void markCursor(){
        mMarkCursor = mNumBytesRead;
    }
    public void resetCursorToMarked(){
        mNumBytesRead = mMarkCursor;
    }

    public String readLine() {
        if (available() < 1) {
            return null;
        }

        StringBuffer string = sb;
        string.setLength(0);
        char c;
        do {
            c = (char) readByte();
            switch (c) {
                case '\r':
                case '\n':
                    // Do nothing.  Skip it.
                    break;

                default:
                    // Save the character.
                    string.append(c);
                    break;
            }
        } while (c != '\n' && available() >= 1);

        return string.toString();
    }

    public void bind(byte[] data, int len) {
        mData = data;
        mNumBytesRead = 0;

        if (len > data.length) {
            len = data.length;
        }
        mSize = len;
        mCapacity = mSize;
    }

    static public xDataInput bind(byte[] data, int offset, int len) {
        xDataInput di = new xDataInput(0);
        di.mSize = offset + len;
        if (di.mSize > data.length) {
            di.mSize = data.length;
        }
        di.mNumBytesRead = offset;
        di.mData = data;
        di.mCapacity = di.mSize;
        
        return di;
    }

    public static xDataInput bind(xDataOutput o) {
        xDataInput in = new xDataInput(0);
        in.mCapacity = o.size();
        in.mData = o.getBytes();
        in.mNumBytesRead = 0;
        in.mSize = o.size();

        return in;
    }
}
