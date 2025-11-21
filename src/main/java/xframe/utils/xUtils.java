/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xframe.utils;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;


import xframe.framework.xDataOutput;
import xframe.framework.xVector;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author ThuyPham
 */
public class xUtils {

    public static final int LINE_GREY_COLOR = 0xff151515;
    public static final int LINE_GREY_COLOR2 = 0xff000000;//0xff050505;
    public static final int LINE_YELLOW_COLOR = 0xffaaaa5f;
    public static final int BG_WHITE_COLOR = 0xffffffff;
    
    public static StringBuffer sb = new StringBuffer();
    public static String emptyString = "";
    
    public static StringBuffer getSB(){
    	sb.setLength(0);
    	return sb;
    }

    public static int ARGB(int a, int r, int g, int b)
    {
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
    public static int RGB(int r, int g, int b)
    {
        return (0xff<<24)|(r<<16)|(g<<8)|b;
    }

    public static int CREATE_DATE(int yyyy, int mm, int dd)
    {
        return (yyyy << 16) | (mm << 8) | dd;
    }
    public static int CREATE_TIME(int h, int m, int s)
    {
        return (h << 16) | (m << 8) | s;
    }

    public static int EXTRACT_YEAR(int d) {
        return (d >> 16);
    }

    public static int EXTRACT_MONTH(int d) {
        return ((d >> 8) & 0xff);
    }

    public static int EXTRACT_DAY(int d) {
        return (d & 0xff);
    }
//	date:0x00hhmmss

    public static int EXTRACT_HOUR(int t) {
        return ((t >> 16) & 0xff);
    }

    public static int EXTRACT_MINUTE(int t) {
        return ((t >> 8) & 0xff);
    }

    public static int EXTRACT_SECOND(int t) {
        return (t & 0xff);
    }

    public static int INT_MAKE(int b3, int b2, int b1, int b0){
        return ((b3&0xff)<<24) | ((b2&0xff)<<16) | ((b1&0xff)<<8) | b0&0xff;
    }
    public static int INT_EXTRACT(int v, int idx){
        int shift = idx*8;
        return (v >> shift)&0xff;
    }

    public static int ABS_INT(int a) {
        if (a > 0) {
            return a;
        }
        return -a;
    }

    public static float ABS_FLOAT(float a) {
        if (a > 0) {
            return a;
        }
        return -a;
    }

    public static void memcpy(byte[] dest, byte[] src, int src_off, int len) {
        if (len > dest.length) {
            len = dest.length;
        }
        for (int i = 0; i < len; i++) {
            dest[i] = src[src_off + i];
        }
    }

    public static void memcpy(byte[] dest, int dest_off, byte[] src, int src_off, int len) {
        if (len > dest.length - dest_off) {
            len = dest.length - dest_off;
        }
        for (int i = 0; i < len; i++) {
            dest[dest_off + i] = src[src_off + i];
        }
    }

    public static int readByte(byte[] p, int off) {
        int t = ((256 + p[off]) & 0xff);
        return t;
    }

    public static short readShort(byte[] p, int off) {
        int t = (((256 + p[off]) & 0xff) << 8) | ((256 + p[off + 1]) & 0xff);

        return (short) t;
    }

    public static float readFloat(byte[] p, int off) {
        int intValue = readInt(p, off);
        float floatValue = 0;
        floatValue = Float.intBitsToFloat(intValue);

        return floatValue;
    }

    public static int readInt(byte[] p, int off) {
        int t = (((256 + p[off]) & 0xff) << 24) | (((256 + p[off + 1]) & 0xff) << 16) | (((256 + p[off + 2]) & 0xff) << 8) | ((256 + p[off + 3]) & 0xff);

        off += 4;
        return t;
    }

    public static String readTerminatedString(byte[] p, int off) {
        sb.setLength(0);
        int end = p.length;

        for (int i = off; i < end; i++) {
            if (p[i] == 0) {
                break;
            }
            sb.append((char) p[i]);
        }
        return sb.toString();
    }

    public static void writeInt(byte[] p, int off, int v) {
        p[off] = (byte) ((v >> 24) & 0xff);
        p[off + 1] = (byte) ((v >> 16) & 0xff);
        p[off + 2] = (byte) ((v >> 8) & 0xff);
        p[off + 3] = (byte) (v & 0xff);
    }

    public static void writeshort(byte[] p, int off, int v) {
        p[off] = (byte) ((v >> 8) & 0xff);
        p[off + 1] = (byte) (v & 0xff);
    }

    public static void writeFloat(byte[] p, int off, float vf) {
        int v = Float.floatToIntBits(vf);

        p[off] = (byte) ((v >> 24) & 0xff);
        p[off + 1] = (byte) ((v >> 16) & 0xff);
        p[off + 2] = (byte) ((v >> 8) & 0xff);
        p[off + 3] = (byte) (v & 0xff);
    }

    public static int getDaysCountFrom(int date){
        if (date == 0)
            return 10000;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, EXTRACT_YEAR(date));
        c.set(Calendar.MONTH, EXTRACT_MONTH(date)-1);
        c.set(Calendar.DAY_OF_MONTH, EXTRACT_DAY(date));

        long dateMs = c.getTime().getTime();
        long now = Calendar.getInstance().getTime().getTime();

        long distance = now - dateMs;

        int msOfADay = 24*60*60*1000;
        long dayCount = distance/msOfADay;

        return (int)dayCount;
    }

    public static long dateToNumber(int date)
    {
        long y = EXTRACT_YEAR(date);
        long m = EXTRACT_MONTH(date);
        long d = EXTRACT_DAY(date);
        m = (m + 9) % 12;   /* mar=0, feb=11 */
        y = y - m/10;       /* if Jan/Feb, year-- */
        return 365*y + y/4 - y/100 + y/400 + (m*306 + 5)/10 + ( d - 1 );
    }

    public static int dateFromNumber(long d){
        long y, ddd, mm, dd, mi;

        y = (10000*d + 14780)/3652425;
        ddd = d - (y*365 + y/4 - y/100 + y/400);
        if (ddd < 0) {
            y--;
            ddd = d - (y*365 + y/4 - y/100 + y/400);
        }
        mi = (52 + 100*ddd)/3060;
        int Y = (int)(y + (mi + 2)/12);
        int M = (int)((mi + 2)%12 + 1);
        int D = (int)(ddd - (mi*306 + 5)/10 + 1);

        return (Y << 16) | (M << 8) | D;
    }

    public static int stepBackToWorkingDay(int date)
    {
        while (true)
        {
            int day = dayOfWeek(date);
            if (day == 0 || day == 6){
                long number = dateToNumber(date);
                number--;
                date = dateFromNumber(number);
            }
            else{
                break;
            }
        }
        return date;
    }

    static public boolean isWorkingTime(){
        Calendar calendar = Calendar.getInstance();
        int date = calendar.get(Calendar.DAY_OF_WEEK);

        if (date >= Calendar.MONDAY && date <= Calendar.FRIDAY){
            int hh = calendar.get(Calendar.HOUR_OF_DAY);
            int mm = calendar.get(Calendar.MINUTE);

            int t = (hh << 8)|mm;
            /*
            int min1 = (9 << 8);
            int max1 = (11<<8)|30;
            int min2 = (12 << 8)|50;
            int max2 = (15 << 8)|30;

            return (t >= min1 && t <= max1) | (t >= min2 && t <= max2);

             */
            int min1 = (9 << 8);        //  9:00
            int max1 = (11<<8)|30;
            int min2 = (12 << 8)|50;
            int max2 = (15 << 8)|0;     //  15:00

            //  9:00 <= now <= 15:00
            return (t >= min1 && t <= max2);
        }

        return false;
    }

    static public boolean isWorkingTime(String timezoneId){
        Calendar calendar = Calendar.getInstance();

        if (timezoneId != null)
        {
            TimeZone fromTZ = TimeZone.getTimeZone(timezoneId);
            calendar.setTimeZone(fromTZ);
        }
        int date = calendar.get(Calendar.DAY_OF_WEEK);

        if (date >= Calendar.MONDAY && date <= Calendar.FRIDAY){
            int hh = calendar.get(Calendar.HOUR_OF_DAY);
            int mm = calendar.get(Calendar.MINUTE);

            int t = (hh << 8)|mm;
            int min = (9 << 8);
            int max = (16 << 8);

            return (t >= min && t <= max);
        }

        return false;
    }

    public static int distanceDate(int d1, int d2)
    {
        long ld1 = dateToNumber(d1);
        long ld2 = dateToNumber(d2);
        return (int)(ld1 - ld2);
    }

    public static int getDateAsInt(String timezoneId, int dayBack) {
        Calendar calendar = Calendar.getInstance();
        if (timezoneId != null)
        {
            TimeZone fromTZ = TimeZone.getTimeZone(timezoneId);
            calendar.setTimeZone(fromTZ);
        }

        long milliseconds = ((long)dayBack * 24 * 60 * 60 * 1000); //  convert to millisecond
        Date d = new Date(System.currentTimeMillis() - milliseconds);

        calendar.setTime(d);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return (year << 16) | (month << 8) | (date);
    }

    public static int getDateAsInt() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return (year << 16) | (month << 8) | (date);
    }
    public static int getTimeAsInt() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        int mm = calendar.get(Calendar.MINUTE);
        int ss = calendar.get(Calendar.SECOND);

        return (hh << 16) | (mm << 8) | ss;
    }
    //	mm/dd/yyyy
    public static int getDateFromStringMMDDYYYY(String s)
    {
    	int date = 0;
    	try
    	{
    		byte[] p = s.getBytes();
    		String[] ss = {"", "", ""};
    		int j = 0;
    		for (int i = 0; i < p.length; i++)
    		{
    			if (p[i] != '/')
    			{
    				ss[j] += (char)p[i];
    			}else{
    				j++;
    			}
    		}
    		
    		int mm = Integer.parseInt(ss[0]);
    		int dd = Integer.parseInt(ss[1]);
    		int yyyy = Integer.parseInt(ss[2]);
    		
    		if (yyyy < 100)
    			yyyy += 2000;
    		
    		date = (yyyy << 16) | (mm << 8) | dd;
    	}catch(Throwable e){
    		e.printStackTrace();
    	}
    	
    	return date;
    }
    
    //	mm/dd/yyyy
    public static int getDateFromStringDDMMYYYY(String s)
    {
    	int date = 0;
    	try
    	{
    		byte[] p = s.getBytes();
    		String[] ss = {"", "", ""};
    		int j = 0;
    		for (int i = 0; i < p.length; i++)
    		{
    			if (p[i] != '/')
    			{
    				ss[j] += (char)p[i];
    			}else{
    				j++;
    			}
    		}
    		
    		int dd = Integer.parseInt(ss[0]);
    		int mm = Integer.parseInt(ss[1]);
    		int yyyy = Integer.parseInt(ss[2]);
    		
    		if (yyyy < 100)
    			yyyy += 2000;
    		
    		date = (yyyy << 16) | (mm << 8) | dd;
    	}catch(Throwable e){
    		e.printStackTrace();
    	}
    	
    	return date;
    }

    public static String dateIntToStringDDMMYY(int date){
        //return "" + EXTRACT_DAY(date) + "/" + EXTRACT_MONTH(date) + "/" + (EXTRACT_YEAR(date)-2000);
        return String.format("%02d/%02d/%02d",
                EXTRACT_DAY(date),
                EXTRACT_MONTH(date),
                (EXTRACT_YEAR(date)-2000)
                );
    }
    
    public static String dateIntToStringDDMM(int date){
        //return "" + EXTRACT_DAY(date) + "/" + EXTRACT_MONTH(date) + "/" + (EXTRACT_YEAR(date)-2000);
        return String.format("%02d/%02d",
                EXTRACT_DAY(date),
                EXTRACT_MONTH(date)
                );
    }
    
    public static String dateIntToStringMMYY(int date){
        //return "" + EXTRACT_DAY(date) + "/" + EXTRACT_MONTH(date) + "/" + (EXTRACT_YEAR(date)-2000);
        return String.format("%02d/%02d",
                EXTRACT_MONTH(date),
                (EXTRACT_YEAR(date)-2000)
                );
    }    
    
    public static String timeIntToStringHHMMSS(int time){
        return String.format("%02d:%02d:%02d",
                EXTRACT_HOUR(time),
                EXTRACT_MINUTE(time),
                EXTRACT_SECOND(time)
                );
    }
    
    public static String timeIntToStringHHMM(int time){
        return String.format("%02d:%02d",
                EXTRACT_HOUR(time),
                EXTRACT_MINUTE(time)
                );
    }
    
    public static String dateIntToStringDDMMYYYY(int date){
        //return "" + EXTRACT_DAY(date) + "/" + EXTRACT_MONTH(date) + "/" + (EXTRACT_YEAR(date)-2000);
        return String.format("%02d/%02d/%04d",
                EXTRACT_DAY(date),
                EXTRACT_MONTH(date),
                EXTRACT_YEAR(date)
        );
    }
    public static String dateIntToStringYYYYMMDD(int date){
        //return "" + EXTRACT_DAY(date) + "/" + EXTRACT_MONTH(date) + "/" + (EXTRACT_YEAR(date)-2000);
        return String.format("%04d/%02d/%02d",
                EXTRACT_YEAR(date),
                EXTRACT_MONTH(date),
                EXTRACT_DAY(date)
        );
    }

    public static String getDateAsString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return "" + date + "/" + month + "/" + year;
    }
    
    public static int getDateAsInt(java.util.Date jdate) {
        if (jdate == null){
            return 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(jdate);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return (year << 16) | (month << 8) | (date);
    }

    public static int getDateAsInt(int dayBack) {
        Calendar calendar = Calendar.getInstance();

        long milliseconds = ((long)dayBack * 24 * 60 * 60 * 1000); //  convert to millisecond
        Date d = new Date(System.currentTimeMillis() - milliseconds);

        calendar.setTime(d);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return (year << 16) | (month << 8) | (date);
    }

    public static Vector getTokens(String s, char delim) {
        Vector v = new Vector();

        StringBuffer tk = xUtils.sb;
        tk.setLength(0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == delim) {
                v.addElement(tk.toString());
                tk.setLength(0);
            } else {
                tk.append(c);
            }
        }
        if (tk.length() > 0) {
            v.addElement(tk.toString());
        }

        return v;
    }

    public static int intToString(int v, byte[] sz, int off) {
        //  v = 123;
        int i = 0;
        do {
            tempSz[i++] = (byte) ((v % 10) + '0');
            v = v / 10;
        } while (v > 0);
        for (int j = i - 1; j >= 0; j--) {
            sz[off++] = tempSz[j];
        }
        return i;
    }

    public static String szToString(byte[] sz) {
        xUtils.sb.setLength(0);

        int j = 0;
        while (sz[j] != 0) {
            xUtils.sb.append((char) sz[j]);
            j++;
        }
        return xUtils.sb.toString();
    }
    static byte[] tempSz = new byte[50];
    static byte[] tempSz1 = new byte[50];

    public static String formatDecimalNumberWidthSignal(int val, int majorDev, int postSurfixCnt) {
        String s = formatDecimalNumber(val, majorDev, postSurfixCnt);
        if (val == 0)
            return s;
        
        if (val < 0) return "- "+s;

        return "+" + s;
    }

//	val = 12345, decimalRound = 100, postSurfixCnt = 1 => sz = 123.4
//	val = 12345, decimalRound = 1000, postSurfixCnt = 2 => sz = 12.34
    public static String formatDecimalNumber(int val, int majorDev, int postSurfixCnt) {
        byte[] sz = tempSz1;
        if (val == 0) {
            sz[0] = '-';
            sz[1] = 0;
            return szToString(sz);
        }

        int sub = (val % majorDev);
        if (sub < 0) {
            sub = -sub;
        }
        int t = 0;
        if (val < 0) {
            t += intToString(xUtils.ABS_INT(val / majorDev), sz, t);
            sz[t++] = '.';
            t += intToString(sub, sz, t);
        } else {
            t += intToString(val / majorDev, sz, t);
            sz[t++] = '.';
            t += intToString(sub, sz, t);
        }
        sz[t] = 0;

        //	17.500
        int j = 0;
        while (sz[j] != '.') {
            j++;
        }
        if (postSurfixCnt == 0) {
            sz[j] = 0;
            return szToString(sz);
        }
        j++;
        int k = 0;
        while (sz[j + k] != 0 && k < postSurfixCnt) {
            k++;
        }
        sz[j + k] = 0;

        return szToString(sz);
    }

//	val = 12345, decimalRound = 100, postSurfixCnt = 1 => sz = 123.4
//	val = 12345, decimalRound = 1000, postSurfixCnt = 2 => sz = 12.34
    public static void formatDecimalNumberAsNullTerminatedString(int val, int majorDev, int postSurfixCnt, byte[] out) {
        byte[] sz = out;
        if (val == 0) {
            sz[0] = '-';
            sz[1] = 0;
            return;
        }

        int sub = (val % majorDev);
        if (sub < 0) {
            sub = -sub;
        }
        int t = 0;
        if (val < 0) {
            t += intToString(xUtils.ABS_INT(val / majorDev), sz, t);
            sz[t++] = '.';
            t += intToString(sub, sz, t);
        } else {
            t += intToString(val / majorDev, sz, t);
            sz[t++] = '.';
            t += intToString(sub, sz, t);
        }
        sz[t] = 0;

        //	17.500
        int j = 0;
        while (sz[j] != '.') {
            j++;
        }
        if (postSurfixCnt == 0) {
            sz[j] = 0;
            return;
        }
        j++;
        int k = 0;
        while (sz[j + k] != 0 && k < postSurfixCnt) {
            k++;
        }
        sz[j + k] = 0;
    }

//	val = 12345, decimalRound = 100, postSurfixCnt = 1 => sz = 123.4
//	val = 12345, decimalRound = 1000, postSurfixCnt = 2 => sz = 12.34
    //	val = 1112.345, decimalRound = 10000, postSurfixCnt = 4 => sz = 12.34
    public static String formatDecimalNumber2(int val, int decimalRound, int postSurfixCnt) {
        xUtils.sb.setLength(0);
        if (val == 0) {
            sb.append('-');
            return sb.toString();
        }
        return formatDecimalNumber(val, decimalRound, postSurfixCnt);
    }

    public static String[] filterStringList(String[] strings, String pattern)
    {
        if(strings == null || strings.length <= 0)
            return null;
        
        Vector temp = new Vector();
        int len = strings.length;

        for(int i = 0; i < len; i++)
        {
            if(strings[i].startsWith(pattern.toUpperCase()))
            {
                temp.addElement(strings[i]);               
            }
        }

        len = temp.size();
        String[] rtn = new String[len];
        temp.copyInto(rtn);

        return rtn;
    }

    public static String formatNumber(double number) {
    	DecimalFormat myFormatter = new DecimalFormat("###,###,###,###");
        String output = myFormatter.format(number);
        
        return output;
    	/*
        if (number == 0){
            return "0";
        }
        //  2345678
        byte[] tmp = tempSz;//new byte[15];
        int j = 0;
        int signal = 1;
        if (number < 0) {
            number = -number;
            signal = -1;
        }

        while (number > 0) {
        	xUtils.trace("HHHHHHHHHH" + number);
            tmp[j++] = (byte) (number % 10);
            number = number / 10;
        }
        // 00013

        sb.setLength(0);
        if (signal == -1)
            sb.append('-');
        for (int i = j - 1; i >= 0; i--) {
            sb.append((char)('0'+tmp[i]));
            if ((i % 3) == 0 && i > 0) {
                sb.append(',');
            }
        }

        return sb.toString();
        */
    }

    public static final long B = 1000000000;
    public static final long M = 1000000;
    public static final long K = 1000;
    public static String formatNumberUsingLetters(double number) {
        return formatNumberUsingLetters(number, "b", "m", "k");
    }

    public static String formatNumberUsingLettersVN_Money(double number) {
        return formatNumberUsingLetters1Decimal(number, "tỉ", "tr", "k");
    }

    public static String formatNumberUsingLetters(double number, String b, String m, String k) {
        String s;
        
        //number = 120006100100f;
        
        double originNumber = number;
        double a_number = Math.abs(number);
        int even;
        double odd;
        number = a_number;

        //number = 49222333444L;

        if (a_number > B){
            //  number = 49222333444
            even = (int)(number / B);
            odd = number - (B*even);    //  222333444
            odd /= (10*M);   //  odd = 22.2
            int nOdd = (int)Math.round(odd);
            if (nOdd == 100){
                even++;
                nOdd = 0;
            }
            if (nOdd > 10) {
                s = String.format("%d.%d%s", even, nOdd, b);
            }
            else{
                s = String.format("%d.0%d%s", even, nOdd, b);
            }
        }
        else if (a_number > M){
            //  number = 12333444;
            even = (int)(number / M);   //  12
            odd = number - (M*even);    //  333444
            odd /= 100000;  //   3.34
            int nOdd = (int)Math.round(odd);
            if (nOdd == 10){
                even++;
                nOdd = 0;
            }

            s = String.format("%d.%d%s", even, nOdd, m);
        }
        else if (a_number > K){
            //  number = -699999.237
            even = (int)(number / K);   //  699
            odd = number - (K*even);    //  99
            odd /= 100;  //  4.44
            int nOdd = (int)Math.round(odd);
            if (nOdd == 10){
                even++;
                nOdd = 0;
            }
            if (nOdd > 0) {
                s = String.format("%d.%d%s", even, nOdd, k);
            }
            else{
                s = String.format("%d%s", even, k);
            }
        }
        else{
            s = String.format("%d", (int)number);
        }

        if (originNumber < 0){
            s = "-" +s;
        }

        return s;
    }
    
    public static String formatNumberUsingLetters1Decimal(double number, String b, String m, String k) {
        String s;
        
        //number = 120006100100f;
        
        double originNumber = number;
        double a_number = Math.abs(number);
        int even;
        double odd;
        number = a_number;

        //number = 49222333444L;

        if (a_number > B){
            //  number = 49222333444
            even = (int)(number / B);
            odd = number - (B*even);    //  222333444
            odd /= (100*M);   //  odd = 222
            int nOdd = (int)Math.round(odd);
            if (nOdd == 10){
                even++;
                nOdd = 0;
            }
            if (nOdd > 1) {
                s = String.format("%d.%d%s", even, nOdd, b);
            }
            else{
                s = String.format("%d.0%s", even, b);
            }
        }
        else if (a_number > M){
            //  number = 12333444;
            even = (int)(number / M);   //  12
            odd = number - (M*even);    //  333444
            odd /= 100000;  //   3.34
            int nOdd = (int)Math.round(odd);
            if (nOdd == 10){
                even++;
                nOdd = 0;
            }

            s = String.format("%d.%d%s", even, nOdd, m);
        }
        else if (a_number > K){
            //  number = -699999.237
            even = (int)(number / K);   //  699
            odd = number - (K*even);    //  99
            odd /= 100;  //  4.44
            int nOdd = (int)Math.round(odd);
            if (nOdd == 10){
                even++;
                nOdd = 0;
            }
            if (nOdd > 0) {
                s = String.format("%d.%d%s", even, nOdd, k);
            }
            else{
                s = String.format("%d%s", even, k);
            }
        }
        else{
            s = String.format("%d", (int)number);
        }

        if (originNumber < 0){
            s = "-" +s;
        }

        return s;
    }
    
    public static String secondsToTimeFormat(int seconds){
        sb.setLength(0);
        
        int time = seconds;
        int h = time/3600;
        int m = time/60;
        int s = time%60;
        String sh = null;
        String sm = null;
        String ss = null;
        
        sh = "" + h;
        if (m < 10) sm = "0" + m;
        else sm = "" + m;
        if (s < 10) ss = "0" + s;
        else ss = "" + s;
        
        sb.append(sh);
        sb.append(':');
        sb.append(sm);
        sb.append(':');
        sb.append(ss);
        
        return sb.toString();
    }
    
    public static String filesizeToMBFormat(int size){
        //  file size
        int m = size/(1024*1024);
        int s = size%(1024*1024);
        s = s/1024;
        s /= 10;
        
        return "" + m + "." + s + " MB";
    }
    
    //  ThuyPS: use this to increase readUTF performance - a lots
    public static String utf8ToUnicode(byte[] utf8, int off, int len) {
        if ((utf8 == null)||(len - off <= 0)) {
            return "";
        }
       /* if (len == 0) {
            len = utf8.length;
        }*/

        int y, x, w, v, u;
        int sFinal;

        StringBuffer s;
        if (len < xUtils.sb.length() / 2) {
            s = xUtils.sb;
        } else {
            s = new StringBuffer();
        }
        s.setLength(0);

        int z;

        for (int i = off; i < len; i++) {
            z = utf8[i];
            if (z < 0) {
                z += 256;
            }
            if (z <= 127) {
                s.append((char) z);
            } else {
                //String dd = "��";
                //int t = dd.charAt(0);
                sFinal = 0;
                if (z >= 192 && z <= 223) {
                    y = utf8[i + 1];
                    if (y < 0) {
                        y += 256;
                    }
                    sFinal = ((z - 192) * 64 + (y - 128));
                    i += 1;
                } else if (z >= 224 && z <= 239) {
                    // character is three bytes
                    y = utf8[i + 1];
                    if (y < 0) {
                        y += 256;
                    }
                    x = utf8[i + 2];
                    if (x < 0) {
                        x += 256;
                    }
                    sFinal = ((z - 224) * 4096 + (y - 128) * 64 + (x - 128));
                    i += 2;
                } else if (z >= 240 && z <= 247) {
                    // character is four bytes
                    y = utf8[i + 1];
                    if (y < 0) {
                        y += 256;
                    }
                    x = utf8[i + 2];
                    if (x < 0) {
                        x += 256;
                    }
                    w = utf8[i + 3];
                    if (w < 0) {
                        w += 256;
                    }
                    sFinal = ((z - 240) * 262144 + (y - 128) * 4096 + (x - 128) * 64 + (w - 128));
                    i += 3;
                } else if (z >= 248 && z <= 251) {
                    // character is five bytes
                    y = utf8[i + 1];
                    if (y < 0) {
                        y += 256;
                    }
                    x = utf8[i + 2];
                    if (x < 0) {
                        x += 256;
                    }
                    w = utf8[i + 3];
                    if (w < 0) {
                        w += 256;
                    }
                    v = utf8[i + 4];
                    if (z < 0) {
                        z += 256;
                    }
                    sFinal += ((z - 248) * 16777216 + (y - 128) * 262144
                            + (x - 128) * 4096 + (w - 128) * 64 + (v - 128));
                    i += 4;
                } else if (z >= 252 && z <= 253) {
                    // character is six bytes
                    y = utf8[i + 1];
                    if (y < 0) {
                        y += 256;
                    }
                    x = utf8[i + 2];
                    if (x < 0) {
                        x += 256;
                    }
                    w = utf8[i + 3];
                    if (w < 0) {
                        w += 256;
                    }
                    v = utf8[i + 4];
                    if (v < 0) {
                        v += 256;
                    }
                    u = utf8[i + 5];
                    if (u < 0) {
                        u += 256;
                    }
                    sFinal += ((z - 252) * 1073741824 + (y - 128) * 16777216
                            + (x - 128) * 262144 + (w - 128) * 4096 + (v - 128) * 64 + (u - 128));
                    i += 5;
                }
                s.append((char) sFinal);
            }
        }

        return s.toString();
    }
    
    public static void writeBytes(byte[] p, int off, byte[] src, int srcOff, int len)
    {
        for (int i = 0; i < len; i++)
        {
            p[off + i] = src[srcOff + i];
        }
    }
    
    public static void strcpy(byte[] dst, byte[] src)
    {
        int l = src.length;
        int i = 0;
        for (i = 0; i < l; i++)
        {
            if (src[i] == 0)
                break;

            dst[i] = src[i];
        }
        dst[i] = 0;
    }

    public static boolean strcmp(byte[] s1, int off1, byte[] s2, int off2, int maxCnt)
    {
        if (s1[off1] == 0 && s2[off2] != 0)
            return false;
        if (s1[off1] != 0 && s2[off2] == 0)
            return false;

        for (int i = 0; i < maxCnt; i++)
        {
            if (s1[off1 + i] == 0 || s2[off2 + i] == 0)
                break;
            if (s1[off1 + i] != s2[off2 + i])
            {
                return false;
            }
        }

        return true;
    }    
    
    public static String bytesNullTerminatedToString(byte[] p, int offset, int len)
    {
        int l = 0;

        for (int i = 0; i < len; i++)
        {
            if (offset + i >= p.length)
                break;

            if (p[offset + i] == 0)
                break;
            l++;
        }

        String enc = utf8ToUnicode(p, offset, len);
        return enc;
    }

    static public String intToMonthString(int date)
    {
        switch (date) {
            case 0:
                return "Jan";
            case 1:
                return "Feb";
            case 2:
                return "Mar";
            case 3:
                return "Apr";
            case 4:
                return "May";
            case 5:
                return "Jun";
            case 6:
                return "Jul";
            case 7:
                return "Aug";
            case 8:
                return "Sep";
            case 9:
                return "Oct";
            case 10:
                return "Nov";
            case 11:
                return "Dec";
            default:
                return "-";
        }
    }

    static public int weekOfTheYear(int date)
    {
        /*
        Calendar calender = Calendar.getInstance();

        Date d;
        SimpleDateFormat format = new SimpleDateFormat("dd/M/yyyy");
        try {
            String s = String.format("%d/%d/%d",
                    date & 0xff,
                    (date >> 8)&0xff,
                    (date >> 16)&0xffff);
            d = format.parse(s);

            calender.setTime(d);
            int weekOfTheYear = calender.get(Calendar.WEEK_OF_YEAR);
            return  weekOfTheYear;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
         */
        //  fast calculation
        int year = EXTRACT_YEAR(date);
        int julian =  dayOfYear(date);  // Jan 1 = 1, Jan 2 = 2, etc...
        int dow = dayOfWeek(date);     // Sun = 0, Mon = 1, etc...
        int dowJan1 = dayOfWeek(CREATE_DATE(year, 1, 1));   // find out first of year's day
// int badWeekNum = (julian / 7) + 1  // Get our week# (wrong!  Don't use this)
        int weekNum = ((julian + 6) / 7);   // probably better.  CHECK THIS LINE. (See comments.)
        if (dow < dowJan1)                 // adjust for being after Saturday of week #1
        {
            ++weekNum;
        }
        return (weekNum);
    }

    static public boolean isLeapYear( int year)
    {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    static int nonleap[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
    static int leap[] = { 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335 };
    public static int dayOfYear(int date)
    {
        int year = xUtils.EXTRACT_YEAR(date);
        int month = xUtils.EXTRACT_MONTH(date);
        int day = xUtils.EXTRACT_DAY(date);
        
        if (month == 0){
            xUtils.trace("");
        }

        if (isLeapYear(year)) {
            return leap[month-1] + day;
        }
        else
            return nonleap[month-1]+day;
    }

    static int[] t = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    public static int dayOfWeek(int date)
    {
        int y = (date >> 16)&0xffff;
        int m = (date >> 8)&0xff;
        int d = date&0xff;

        return dayOfWeek(y, m, d);
    }
    

    static public int dayOfWeek(int y, int m, int d) /* 0 = Sunday */
    {
        if (m >= 0 && m < t.length){
            y -= (m < 3)?1:0;
            return (y + y / 4 - y / 100 + y / 400 + t[m - 1] + d) % 7;
        }
        return 0;
    }
    
    static public void trace(String s)
    {
        String time = timeAsString();
    	System.out.println(time + " : " + s);
    }

    static public void trace(String tag, String s)
    {
        System.out.println(s);
    }
    
    
    private static boolean isWhitespace(char aChar) {
        return (aChar == ' ' || aChar == '\n');
    }
    
    static public int stringToInt(String a)
    {
    	try
    	{
            a = a.replace(",", "");
    		return Integer.parseInt(a);
    	}
    	catch(Throwable e)
    	{
    		
    	}
    	return 0;
    }

    static public long stringToLong(String a)
    {
        try
        {
            return Long.parseLong(a);
        }
        catch(Throwable e)
        {

        }
        return 0;
    }
    
    static public float stringToFloat(String a)
    {
    	try
    	{
            return Float.parseFloat(a);
            /*
            //  too slow
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            DecimalFormat format = new DecimalFormat("0.#");
            format.setDecimalFormatSymbols(symbols);
            float f = format.parse(a).floatValue();

            return f;

             */
    	}
    	catch(Throwable e)
    	{
    		
    	}
    	return 0;
    }

    static public String floatToString(float f, int maxFractionDigits)
    {
        String format = String.format("%%.%df", maxFractionDigits);
        return String.format(Locale.US, format, f);
    }

    static public double stringToDouble(String input)
    {
        try{
            return Double.parseDouble(input);

            /*
            too slow

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
            ParsePosition parsePosition = new ParsePosition(0);
            Number number = numberFormat.parse(input, parsePosition);

            if(parsePosition.getIndex() != input.length()){
                return 0;
            }

            return number.doubleValue();

             */
        }
        catch(Throwable e){
            e.printStackTrace();
        }

        return 0;
    }
    
    static public String encodeQuery(String query)
    {
    	try{
    		query = URLEncoder.encode(query, "utf-8");
    	}catch(Exception e){}
    	
    	return query;
    }

    public static String decodeQuery(String url){
        try {
            url = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        }
        catch (Throwable e){

        }
        return url;
    }

    static boolean isReadableChar(char c){
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z');
    }

    static public String charToHex(char c) {
        // Returns hex String representation of char c
        byte hi = (byte) (c >>> 8);
        byte lo = (byte) (c & 0xff);
        return byteToHex(hi) + byteToHex(lo);
    }

    static public String byteToHex(byte b) {
        // Returns hex String representation of byte b
        char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        return new String(array);
    }

    static public String encodeUnicodeString(String str)
    {
        try{
            xDataOutput o = new xDataOutput(str.length()*2);
            for (int i = 0; i < str.length(); i++)
            {
                char c = str.charAt(i);
                if (isReadableChar(c)){
                    o.writeByte((byte)c);
                }
                else{
                    String s = "|" + (int)c + ".";
                    for (int j = 0; j < s.length(); j++){
                        o.writeByte((byte)s.charAt(j));
                    }
                }
            }

            String encoded = new String(o.getBytes(), 0, o.size(), "utf-8");
            return encoded;
        }catch(Exception e){}

        return str;
    }

    static public String decodeUnicodeString(String str){
        try{
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < str.length(); i++){
                char c = str.charAt(i);
                if (c == '|')
                {
                    int n = 0;
                    for (int j = i+1; j < str.length(); j++){
                        char c1 = str.charAt(j);
                        if (c1 == '.'){
                            i = j;
                            break;
                        }
                        c1 -= '0';
                        n = n*10 + (int)c1;
                    }
                    b.append((char)n);
                }
                else{
                    b.append(c);
                }
            }
            return b.toString();
        }
        catch (Throwable e){

        }
        return str;
    }

    public static String htmlEntityDecode(String s) {
        int i = 0, j = 0, pos = 0;
        StringBuffer sb = new StringBuffer();
        while ((i = s.indexOf("&", pos)) != -1
                && (j = s.indexOf(';', i)) != -1) {
            int n = -1;
            for (i += 1; i < j; ++i) {
                char c = s.charAt(i);
                if ('0' <= c && c <= '9')
                    n = (n == -1 ? 0 : n * 10) + c - '0';
                else
                    break;
            }

            // skip malformed html entities
            if (i != j)
                n = -1;

            if (n != -1) {
                sb.append((char) n);
            } else {
                // force deletion of chars
                for (int k = pos; k < i - 1; ++k) {
                    sb.append(s.charAt(k));
                }
                sb.append(" ");
            }
            // skip ';'
            i = j + 1;
            pos = i;
        }
        if (sb.length() == 0)
            return s;
        else
            sb.append(s.substring(pos, s.length()));
        return sb.toString();

    }

    public static int stringToMonth(String sm)
	{
		if (sm.length() < 3)
			return 0;
		sm = sm.toUpperCase();
		
		int mm = 0;
		char c = sm.charAt(0);
		if (c >= 'A' && c <= 'Z')
		{
			if (sm.indexOf("JAN") != -1) mm = 1;
			else if (sm.indexOf("FEB") != -1) mm = 2;
			else if (sm.indexOf("MAR") != -1) mm = 3;
			else if (sm.indexOf("APR") != -1) mm = 4;
			else if (sm.indexOf("MAY") != -1) mm = 5;
			else if (sm.indexOf("JUN") != -1) mm = 6;
			else if (sm.indexOf("JUL") != -1) mm = 7;
			else if (sm.indexOf("AUG") != -1) mm = 8;
			else if (sm.indexOf("SEP") != -1) mm = 9;
			else if (sm.indexOf("OCT") != -1) mm = 10;
			else if (sm.indexOf("NOV") != -1) mm = 11;
			else if (sm.indexOf("DEC") != -1) mm = 12;
		}
		
		return mm;
	}

    public static boolean isNullString(String s){
        return s == null || s.length() == 0;
    }
    
    static public float density(){
        return 1.0f;
    }
    
    static public float densityDpi(){
        return 160.0f;
    }

    public static float pointToPixels(float dp)
    {
        float density = density();
        return Math.round(dp * density);
        /*
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, point, context.getResources().getDisplayMetrics());

        return pixels;
        */
    }

    public static int toPixels(float dp)
    {
        float density = density();
        return Math.round(dp * density);
        //return (int)pointToPixels(point, MainActivity.getInstance());
    }

    static public float pixelsToPoints(float pixels)
    {
        float desityDPI = densityDpi();
        return ((pixels*160)/desityDPI);
    }
    
    static public float pixelsToPoints(float pixels, Object ctx)
    {
        float desityDPI = densityDpi();
        return ((pixels*160)/desityDPI);
    }

    public static String formatFloatNumberPrice(float t){
        String s;
        double d = t*100;
        int remains = (int)Math.abs(d) % 100;
        if ((remains % 10) == 0){
            if (remains > 0){
                s = String.format(Locale.US, "%.1f", t);
            }
            else{
                s = String.format(Locale.US, "%.1f", t);
            }
        }
        if (remains > 1){
            s = String.format(Locale.US, "%.2f", t);
        }
        else{
            s = String.format(Locale.US, "%.1f", t);
        }
        return s;
    }

    static Random _random;
    static public int random(int n){
        if (_random == null){
            _random = new Random();
        }
        return _random.nextInt(n);
    }

    static public boolean isNetworkAvailable(){
        return true;
    }

    public static int MAKEINT_BIT(int n, int b)
    {
        return n << b;
    }

    public static int MAKE_N1(int len){
        return ((1<<len)-1);
    }

    public static int GETINT(int n, int bitstart, int len)
    {
        int v = (n >> bitstart) & MAKE_N1(len);
        return v;
    }

    //  format: dd/mm/yyyy
    public static String dateIntToStringWithFormat(int date, String format){
        format = format.toLowerCase();

        int day = EXTRACT_DAY(date);
        int month = EXTRACT_MONTH(date);
        int year = EXTRACT_YEAR(date);

        String sday = String.format("%02d", day);
        String smonth = String.format("%02d", month);
        String syear2 = String.format("%02d", (year - 2000));
        String syear4 = String.format("%04d", year);

        if (format.indexOf("yyyy") != -1)
        {
            format = format.replace("dd", sday);
            format = format.replace("mm", smonth);
            format = format.replace("yyyy", syear4);
        }
        else{
            format = format.replace("dd", sday);
            format = format.replace("mm", smonth);
            format = format.replace("yy", syear2);
        }

        return format;
    }

    public static int stringDateToIntWithFormat(String s, String format)
    {
        String delim = "[-]";
        for (int i = 0; i < format.length(); i++)
        {
            char c = format.charAt(i);
            if (c != 'd' && c != 'm' && c != 'y'){
                delim = String.format("[%c]", c);
                break;
            }
        }

        String ss[] = format.split(delim);
        if (ss.length != 3)
        {
            xUtils.trace("Cannot parse date format");
            return 0;
        }

        String ssDate[] = s.split(delim);
        int d = 0;
        int m = 0;
        int y = 0;
        for (int i = 0; i < ss.length; i++)
        {
            String label = ss[i];
            if (label.equalsIgnoreCase("dd"))
            {
                d = xUtils.stringToInt(ssDate[i]);
            }
            else if (label.equalsIgnoreCase("mm")){
                m = xUtils.stringToInt(ssDate[i]);
            }
            else if (label.equalsIgnoreCase("mmm")){
                m = stringMonthToInt(ssDate[i]);
            }
            else if (label.equalsIgnoreCase("yy")){
                y = 2000 + xUtils.stringToInt(ssDate[i]);
            }
            else if (label.equalsIgnoreCase("yyyy")){
                y = xUtils.stringToInt(ssDate[i]);
            }
        }

        return (y << 16) | (m << 8) | d;
    }

    static public int stringMonthToInt(String s)
    {
        if (s.equalsIgnoreCase("JAN")) return 1;
        else if (s.equalsIgnoreCase("FEB")) return 2;
        else if (s.equalsIgnoreCase("MAR")) return 3;
        else if (s.equalsIgnoreCase("APR")) return 4;
        else if (s.equalsIgnoreCase("MAY")) return 5;
        else if (s.equalsIgnoreCase("JUN")) return 6;
        else if (s.equalsIgnoreCase("JUL")) return 7;
        else if (s.equalsIgnoreCase("AUG")) return 8;
        else if (s.equalsIgnoreCase("SEP")) return 9;
        else if (s.equalsIgnoreCase("OCT")) return 10;
        else if (s.equalsIgnoreCase("NOV")) return 11;
        else if (s.equalsIgnoreCase("DEC")) return 12;

        return 0;
    }

    public static int stringYYYYMMDDToDateInt(String s, String delim)
    {
        int date = 0;
        if (s == null){
            return 0;
        }
        try
        {
            if (s.length() == 8 && (delim == null || delim.length() == 0)){
                int yyyy = Integer.parseInt(s.substring(0, 4));
                int mm = Integer.parseInt(s.substring(4, 6));
                int dd = Integer.parseInt(s.substring(6, 8));

                if (yyyy < 100){
                    yyyy += 2000;
                }

                date = (yyyy << 16) | (mm << 8) | dd;
            }
            else{
                String[] ss = s.split(delim);
                if (ss.length == 3)
                {
                    int yyyy = Integer.parseInt(ss[0]);
                    int mm = Integer.parseInt(ss[1]);
                    int dd = Integer.parseInt(ss[2]);

                    if (yyyy < 100){
                        yyyy += 2000;
                    }

                    date = (yyyy << 16) | (mm << 8) | dd;
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
        }

        return date;
    }

    public static int stringHHMMSSToTimeInt(String s, String delim)
    {
        int date = 0;
        if (s == null){
            return 0;
        }
        try
        {
            if (s.length() == 4 && (delim == null || delim.length() == 0)){
                int hh = xUtils.stringToInt(s.substring(0, 2));
                int mm = xUtils.stringToInt(s.substring(2, 4));
                
                date = (hh << 16) | (mm << 8) | 0;
            }
            else if (s.length() == 6 && (delim == null || delim.length() == 0)){
                int hh = xUtils.stringToInt(s.substring(0, 2));
                int mm = xUtils.stringToInt(s.substring(2, 4));
                int ss = xUtils.stringToInt(s.substring(4, 6));
                
                date = (hh << 16) | (mm << 8) | ss;
            }            
            else{
                String[] ss = s.split(delim);

                int hh = Integer.parseInt(ss[0]);
                int mm = Integer.parseInt(ss[1]);
                int secs = 0;
                if (ss.length == 3){
                    secs = Integer.parseInt(ss[2]);
                }

                date = (hh << 16) | (mm << 8) | secs;
            }
        }catch(Throwable e){
            e.printStackTrace();
        }

        return date;
    }

    public static void getDateFromTime(long time, int[] date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(time);

        date[0] = calendar.get(Calendar.YEAR);
        date[1] = calendar.get(Calendar.MONTH) + 1;
        date[2] = calendar.get(Calendar.DATE);
        date[3] = calendar.get(Calendar.HOUR_OF_DAY);
        date[4] = calendar.get(Calendar.MINUTE);
        date[5] = calendar.get(Calendar.MILLISECOND);
    }

    public static int getDateAsInt(String timezoneId) {
        Calendar calendar = Calendar.getInstance();
        if (timezoneId != null)
        {
            TimeZone fromTZ = TimeZone.getTimeZone(timezoneId);
            calendar.setTimeZone(fromTZ);
        }

        calendar.setTime(new Date());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);

        return (year << 16) | (month << 8) | (date);
    }
    
    public static int getDateIntFromString(String sdate, String format)
    {
        try{
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
                .appendLiteral('Z')
                .toFormatter();
            LocalDateTime dateTime = LocalDateTime.parse(sdate, formatter);

            int year = dateTime.getYear();
            int month = dateTime.getMonthValue();
            int day = dateTime.getDayOfMonth();

            return (year << 16) | (month << 8) | (day);
            
        }catch(Throwable e){
            
        }
        
        return 0;
    }

    public static String formatPrice(float price)
    {
        if (price > 10000){
            return String.format("%.1f", price);
        }
        else if (price > 1000){
            return String.format("%.1f", price);
        }
        else if (price > 100){
            return String.format("%.2f", price);
        }
        else if (price > 10){
            return String.format("%.2f", price);
        }
        else if (price > 0.01f){
            return String.format("%.3f", price);
        }
        else if (price > 0.001f){
            return String.format("%.4f", price);
        }
        else if (price > 0.0001f){
            return String.format("%.5f", price);
        }
        else if (price > 0.00001f){
            return String.format("%.6f", price);
        }
        else if (price > 0.000001f){
            return String.format("%.7f", price);
        }
        else if (price > 0.000001f){
            return String.format("%.8f", price);
        }

        return String.format("%f", price);
    }

    public static String formatPrice(float price, float refPrice)
    {
        if (refPrice > 10000){
            return String.format("%.1f", price);
        }
        if (refPrice > 1000){
            return String.format("%.2f", price);
        }
        if (refPrice > 100){
            return String.format("%.2f", price);
        }
        if (refPrice > 10){
            return String.format("%.2f", price);
        }
        if (refPrice > 0.1f){
            return String.format("%.3f", price);
        }
        if (refPrice > 0.01f){
            return String.format("%.4f", price);
        }
        if (refPrice > 0.001f){
            return String.format("%.5f", price);
        }

        return String.format("%f", price);
    }

    public static String readTerminatedString(byte[] p, int off, int max) {
        sb.setLength(0);

        int end = p.length;
        int j = 0;

        for (int i = off; i < end; i++) {
            if (p[i] == 0) {
                break;
            }
            sb.append((char) p[i]);
            j++;
            if (j >= max){
                break;
            }
        }
        return sb.toString();
    }

    public static void writeLong(byte[] p, int off, long v) {
        for (int i = 7; i >= 0; i--) {
            p[off++] = (byte) ((v >> (8 * i))&0xff);
        }
    }

    public static void writeFloat2(byte[] p, int off, float v){
        try{
            int val = Float.floatToIntBits(v);
            xUtils.writeInt(p, off, val);
        }catch(Throwable e){

        }
    }

    public static float readFloat2(byte[] p, int off) {
        int v = readInt(p, off);
        float f = Float.intBitsToFloat(v);

        return f;
    }

    public static long readLong(byte[] p, int off){
        long t = 0;
        for (int i = 0; i < 8; i++) {
            t |= ((256 + p[off+i]) & 0xff) << (8 * (8 - i - 1));
        }
        return t;
    }

    public static int dateFromEpochSeconds(long time)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(time*1000);

        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH) + 1;
        int d = calendar.get(Calendar.DATE);

        return (y << 16) | (m << 8) | d;
    }

    public static int timeFromEpochSeconds(long time)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(time*1000);

        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        int mm = calendar.get(Calendar.MINUTE);
        int ss = calendar.get(Calendar.DATE);

        return (hh << 16) | (mm << 8) | ss;
    }

    //  HH:MM:SS
    static public int timeStringToInt(String str){
        try{
            String[] ss = str.split("[:]");
            int hh = xUtils.stringToInt(ss[0]);
            int mm = xUtils.stringToInt(ss[1]);
            int secs = xUtils.stringToInt(ss[2]);

            return (hh<<16) | (mm<<8) | secs;
        }
        catch(Throwable e){

        }

        return 0;
    }

    static public int timeStringToInt(String str, String defValue){
        try{
            String[] ss = str.split("[:]");
            int hh = xUtils.stringToInt(ss[0]);
            int mm = xUtils.stringToInt(ss[1]);
            int secs = xUtils.stringToInt(ss[2]);

            return (hh<<16) | (mm<<8) | secs;
        }
        catch(Throwable e){

        }

        return timeStringToInt(defValue);
    }
    
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static public String timeAsString()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
    
    public static final String DATE_FORMAT_NOW_HHMMSS = "HH:mm:ss";
    static public String timeAsStringHHMMSS()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW_HHMMSS);
        return sdf.format(cal.getTime());
    }
    
    public static final String DATE_FORMAT_NOW2 = "dd HH:mm:ss";
    static public String timeAsStringDDHHMMSS()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW2);
        return sdf.format(cal.getTime());
    }
    static public String timeAsStringDDHHMM()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm");
        return sdf.format(cal.getTime());
    }
    
    static public long currentTimeMS(){
        return System.currentTimeMillis();
    }
    
    static public String timeToStringHHMMSS(int hhmmss)
    {
        int hh = (hhmmss>>16)&0xff;
        int mm = (hhmmss>>8)&0xff;
        int ss = (hhmmss&0xff);
        
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }
    
    static public String timeToStringHHMM(int hhmm)
    {
        int hh = (hhmm>>16)&0xff;
        int mm = (hhmm>>8)&0xff;
        
        return String.format("%02d:%02d", hh, mm);
    }
    
    static public double timeIntervalSinceReferenceDate(){
        // Reference date: January 1, 2001, 00:00:00 UTC
        long referenceDateMillis = 978307200000L;

        // Current date and time
        long currentTimeMillis = System.currentTimeMillis();

        // Calculate the time interval since the reference date in seconds
        double _timeIntervalSinceReferenceDate = (currentTimeMillis - referenceDateMillis) / 1000.0;

        return _timeIntervalSinceReferenceDate;
    }
        
     /*              
    static String kEncryptKey = String.format("%s%s%s", "0123456789", "aAbBcCdDeE", "!@#$%^&*()-+");        
    public static String decryptBase64Bytes(byte[] data)
    {
        String key = kEncryptKey;
        
        if (data.length < 1){
            return "";
        }
        //  <!xml
        if (data[0] == '<' && data[1] == '?' && data[2] == 'x'){
            try{
                String s = new String(data, 0, data.length, "utf-8");
                return s;
            }
            catch (Throwable e){
                e.printStackTrace();
            }
        }

        //=========================================================
        byte[] symKeyDataTmp = key.getBytes();
        byte[] symKeyData = new byte[32];
        for (int i = 0; i < 32; i++){
            if (i < symKeyDataTmp.length) symKeyData[i] = symKeyDataTmp[i];
            else symKeyData[i] = 0;
        }
        
        byte[] base64Encrypted = new byte[10*data.length+64];

        Base64.getDecoder().decode(data, base64Encrypted);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            int blockSize = cipher.getBlockSize();

            // create the key
            SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // retrieve random IV from start of the received message
            byte[] ivData = new byte[blockSize];
            IvParameterSpec iv = new IvParameterSpec(ivData);

            // retrieve the encrypted message itself
            cipher.init(Cipher.DECRYPT_MODE, symKey, iv);

            byte[] encodedMessage = cipher.doFinal(base64Encrypted);

            String message = new String(encodedMessage, "UTF-8");

            return message;
        }
        catch(Throwable e){
            e.printStackTrace();
        }

        return "";
    }
        */
        
    static Random _rand = new Random(123);
    //*
    public static char randomChar()
    {
        String string = "0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz";

        int t = _rand.nextInt(string.length());

        char randomChar = string.charAt(t);
        return randomChar;
    }        
    
    public static String encryptString(String string)
    {
        //return string;
        //*
        if (string == null)
        {
            return null;
        }

        byte[] c = string.getBytes();

        int size = c.length;

        byte[] b = new byte[2 * size];

        for (int i = 0; i < size; i++)
        {
            byte t = c[i];

            if (i % 6 == 0) t += 3;
            else if (i % 6 == 1) t += 1;
            else if (i % 6 == 2) t += 5;
            else if (i % 6 == 3) t += 0;
            else if (i % 6 == 4) t += -3;
            else if (i % 6 == 5) t += 2;

            b[2 * i] = t;
            b[2 * i + 1] = (byte)randomChar();
        }

        byte[] d2 = new byte[10*1024];
        int len = Base64.getEncoder().encode(b, d2);
        String s = null;
        try{
            if (len > 0){
                s = new String(d2, 0, len, "utf-8");
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
        return s;
        //*/
    }

    public static String decryptString(String string) {
        //return string;
        //*
        if (string == null) {
            return null;
        }

        byte[] c = new byte[10*1024];
        int size = Base64.getDecoder().decode(string.getBytes(), c);

        byte[] b = new byte[size / 2];

        for (int i = 0; i < size / 2; i++) {
            byte t = c[2 * i];

            if (i % 6 == 0)
                t -= 3;
            else if (i % 6 == 1)
                t -= 1;
            else if (i % 6 == 2)
                t -= 5;
            else if (i % 6 == 3)
                t -= 0;
            else if (i % 6 == 4)
                t -= -3;
            else if (i % 6 == 5)
                t -= 2;

            b[i] = t;
        }

        String s = null;
        try {
            s = new String(b, 0, b.length, "utf-8");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return s;
        //*/
    }
    
    public static int getThirdThursday(int year, int month) {
        // Lấy ngày đầu tiên của tháng
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        // Tìm ngày thứ Năm đầu tiên của tháng
        LocalDate firstThursday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));

        // Tìm ngày thứ Năm lần thứ 3 của tháng
        LocalDate thirdThursday = firstThursday.plusWeeks(2);
        
        int yyyy = thirdThursday.getYear();
        int mm = thirdThursday.getMonthValue();
        int dd = thirdThursday.getDayOfMonth();
        
        return (yyyy<<16) | (mm << 8) | dd;
    }
    
    public static int getThirdThursday() {
        int today = getDateAsInt();
        int year = EXTRACT_YEAR(today);
        int month = EXTRACT_MONTH(today);
        
        // Lấy ngày đầu tiên của tháng
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        // Tìm ngày thứ Năm đầu tiên của tháng
        LocalDate firstThursday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));

        // Tìm ngày thứ Năm lần thứ 3 của tháng
        LocalDate thirdThursday = firstThursday.plusWeeks(2);
        
        int yyyy = thirdThursday.getYear();
        int mm = thirdThursday.getMonthValue();
        int dd = thirdThursday.getDayOfMonth();
        
        return (yyyy<<16) | (mm << 8) | dd;
    }
    
    static public String doubleToStringPresentation(double value, 
            int minPartialParts,
            int maxPartialParts
            )
    {
        long l = (long)value;
        double partial = value - l;
        
        //  29.299
        //  0.01
        
        
        int mul10 = 1;
        
        if (partial > 0){
            while (partial < 1 && mul10 < 6){
                mul10++;
                partial *= 10;
            }
        }
        else{
            mul10 = 1;
        }
        if (mul10 < minPartialParts){
            mul10 = minPartialParts;
        }
        if (mul10 > maxPartialParts){
            mul10 = maxPartialParts;
        }
        
        String format = String.format("%%.%df", mul10);
        String present = String.format(format, value);
        return present;
    }
    
    public static int dateTimeToPackaged(int date, int time){
        try{
            int Y = xUtils.EXTRACT_YEAR(date);            
            int M = xUtils.EXTRACT_MONTH(date);
            M = (Y - 2020)*12 + (M-1);
            
            int D = xUtils.EXTRACT_DAY(date);
            
            int hh = xUtils.EXTRACT_HOUR(time);
            int mm = xUtils.EXTRACT_MINUTE(time);
            
            return (M << 24) | (D << 16) | (hh << 8) | mm;
        }
        catch(Throwable e){
            
        }
        
        return 0;
    }
    
    public static int dateFromPackagedDate(int packedDateTime){
        if (packedDateTime == 0){
            return 0;
        }
        int my = (packedDateTime>>24)&0xff;
        int year = 2020+(my/12);
        int month = (my%12)+1;
        int day = (packedDateTime>>16)&0xff;
        //int hh = (packedDateTime>>8)&0xff;
        //int mm = (packedDateTime&0xff);
        
        return (year << 16) | (month << 8) | day;
    }
    
    public static int timeFromPackagedDate(int packedDateTime){
        if (packedDateTime == 0){
            return 0;
        }
        int my = (packedDateTime>>24)&0xff;
        int year = 2020+(my/12);
        int month = (my%12)+1;
        int day = (packedDateTime>>16)&0xff;
        int hh = (packedDateTime>>8)&0xff;
        int mm = (packedDateTime&0xff);
        
        return (hh<<16) | (mm<<8) | 0;
    }    
}
