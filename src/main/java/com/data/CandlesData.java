package com.data;

import java.sql.Date;
import xframe.framework.xDataInput;
import xframe.framework.xDataOutput;
import xframe.utils.xUtils;

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
   public float[] close;
   public float[] open;
   public float[] hi;
   public float[] lo;
   public int[] date;
   public int[] volume;
   public long timeLastUpdate;
   public int firsDate;
   public long timeCreated;
   public boolean isIntraday;
   public int candleFrame;
   int cursor;

   public CandlesData(int shareId, String symbol, int market) {
      this.shareId = shareId;
      this.symbol = symbol;
      this.marketId = market;
      this.isIntraday = false;
      int candleCnt = 2000;
      this.prepare(candleCnt);
   }

   public CandlesData(int shareId, String symbol, int market, int candleCnt) {
      this.shareId = shareId;
      this.symbol = symbol;
      this.marketId = market;
      this.prepare(candleCnt + 512);
   }

   void prepare(int candleCnt) {
      if (candleCnt < 128) {
         candleCnt = 128;
      }

      this.close = new float[candleCnt];
      this.open = new float[candleCnt];
      this.hi = new float[candleCnt];
      this.lo = new float[candleCnt];
      this.date = new int[candleCnt];
      this.volume = new int[candleCnt];
   }

   public void addCandle(float c, float o, float h, float l, int d, int v) {
      if (this.close.length <= this.cursor) {
         int newLen = this.cursor + 1000;
         float[] pC = new float[newLen];
         float[] pO = new float[newLen];
         float[] pH = new float[newLen];
         float[] pL = new float[newLen];
         int[] pD = new int[newLen];
         int[] pV = new int[newLen];
         System.arraycopy(this.open, 0, pO, 0, this.cursor);
         System.arraycopy(this.close, 0, pC, 0, this.cursor);
         System.arraycopy(this.hi, 0, pH, 0, this.cursor);
         System.arraycopy(this.lo, 0, pL, 0, this.cursor);
         System.arraycopy(this.date, 0, pD, 0, this.cursor);
         System.arraycopy(this.volume, 0, pV, 0, this.cursor);
         this.open = pO;
         this.close = pC;
         this.hi = pH;
         this.lo = pL;
         this.date = pD;
         this.volume = pV;
      }

      this.close[this.cursor] = c;
      this.open[this.cursor] = o;
      this.hi[this.cursor] = h;
      this.lo[this.cursor] = l;
      this.date[this.cursor] = d;
      this.volume[this.cursor] = v;
      ++this.cursor;
   }

   public void log() {
      for(int i = 0; i < this.cursor; ++i) {
         String sd;
         if (this.isIntraday) {
            int d = xUtils.dateFromPackagedDate(this.date[i]);
            int t = xUtils.timeFromPackagedDate(this.date[i]);
            sd = String.format("%s %s", xUtils.dateIntToStringDDMMYY(d), xUtils.timeIntToStringHHMM(t));
         } else {
            sd = String.format("%s", xUtils.dateIntToStringDDMMYY(this.date[i]));
         }

         String s = String.format("%s: %.2f/%.2f/%.2f/%.2f; V=%d", sd, this.open[i], this.hi[i], this.lo[i], this.close[i], this.volume[i]);
         xUtils.trace(s);
      }

   }

   public void reverse() {
      for(int i = 0; i < this.cursor / 2; ++i) {
         int last = this.cursor - 1 - i;
         float c = this.close[i];
         this.close[i] = this.close[last];
         this.close[last] = c;
         c = this.open[i];
         this.open[i] = this.open[last];
         this.open[last] = c;
         c = this.hi[i];
         this.hi[i] = this.hi[last];
         this.hi[last] = c;
         c = this.lo[i];
         this.lo[i] = this.lo[last];
         this.lo[last] = c;
         int n = this.date[i];
         this.date[i] = this.date[last];
         this.date[last] = n;
         n = this.volume[i];
         this.volume[i] = this.volume[last];
         this.volume[last] = n;
      }

   }

   public void updateLastCandles(CandlesData news) {
      if (news.getCandleCnt() > 0) {
         this.removeCandlesIncludeDate(news.date[0]);
      }

      for(int i = 0; i < news.getCandleCnt(); ++i) {
         this.addCandle(news.close[i], news.open[i], news.hi[i], news.lo[i], news.date[i], news.volume[i]);
      }

      this.timeLastUpdate = System.currentTimeMillis();
   }

   public void removeCandlesIncludeDate(int includeDate) {
      for(int i = this.cursor - 1; i >= 0 && this.date[i] >= includeDate; this.cursor = i--) {
      }

   }

   public int getCandleCnt() {
      return this.cursor;
   }

   public int measureDataSize() {
      return this.cursor * 24 + 12 + 8 + 32;
   }

   public void clear() {
      this.cursor = 0;
   }

   public CandlesData clone() {
      CandlesData c = new CandlesData(this.shareId, this.symbol, this.marketId);
      c.prepare(this.cursor);
      c.symbol = this.symbol;
      c.marketId = this.marketId;
      c.factor = this.factor;
      c.cursor = this.cursor;
      c.timeCreated = this.timeCreated;
      System.arraycopy(this.open, 0, c.open, 0, this.cursor);
      System.arraycopy(this.close, 0, c.close, 0, this.cursor);
      System.arraycopy(this.hi, 0, c.hi, 0, this.cursor);
      System.arraycopy(this.lo, 0, c.lo, 0, this.cursor);
      System.arraycopy(this.date, 0, c.date, 0, this.cursor);
      System.arraycopy(this.volume, 0, c.volume, 0, this.cursor);
      return c;
   }

   public CandlesData cloneFromDate(int from) {
      CandlesData c = new CandlesData(this.shareId, this.symbol, this.marketId);
      c.marketId = this.marketId;
      c.symbol = this.symbol;
      c.factor = this.factor;
      c.timeCreated = this.timeCreated;
      c.timeLastUpdate = this.timeLastUpdate;
      int j = 0;

      int candleCnt;
      for(candleCnt = 0; candleCnt < this.cursor; ++candleCnt) {
         if (this.date[candleCnt] >= from) {
            j = candleCnt - 3;
            if (j < 0) {
               j = 0;
            }
            break;
         }
      }

      candleCnt = this.cursor - j;
      if (candleCnt > 0) {
         c.prepare(candleCnt);
      }

      while(j < this.cursor) {
         c.addCandle(this.close[j], this.open[j], this.hi[j], this.lo[j], this.date[j], this.volume[j]);
         ++j;
      }

      c.cursor = candleCnt;
      return c;
   }

   public CandlesData cloneWithCandleCnt(int candleCnt) {
      CandlesData c = new CandlesData(this.shareId, this.symbol, this.marketId);
      c.marketId = this.marketId;
      c.symbol = this.symbol;
      c.factor = this.factor;
      c.timeCreated = this.timeCreated;
      c.timeLastUpdate = this.timeLastUpdate;
      int j = this.cursor - candleCnt;
      if (j < 0) {
         j = 0;
      }

      candleCnt = this.cursor - j;
      if (candleCnt > 0) {
         c.prepare(candleCnt);
      }

      while(j < this.cursor) {
         c.addCandle(this.close[j], this.open[j], this.hi[j], this.lo[j], this.date[j], this.volume[j]);
         ++j;
      }

      c.cursor = candleCnt;
      return c;
   }

   public int lastDate() {
      return this.cursor > 0 ? this.date[this.cursor - 1] : 0;
   }

   public int firstDate() {
      return this.firsDate;
   }

   public int avgVolume() {
      if (this.cursor <= 20) {
         return 0;
      } else {
         double v = 0.0D;

         for(int i = 0; i < 5; ++i) {
            v += (double)this.volume[this.cursor - 1 - i];
         }

         return (int)(v / 5.0D);
      }
   }

   public static int dateFromSqlDate(Date date) {
      try {
         return xUtils.getDateAsInt(new java.util.Date(date.getTime()));
      } catch (Throwable var2) {
         return 0;
      }
   }

   public boolean isExpired() {
      long now = System.currentTimeMillis();
      long elapsed = now - this.timeCreated;
      elapsed /= 1000L;
      return elapsed > 14400L;
   }

   public void writeTo(xDataOutput aOut) {
      aOut.writeUTF(DBHelper.convertDBSymbolToVNChartSymbol(this.symbol));
      int cnt = this.cursor;
      aOut.writeInt(cnt);

      for(int i = 0; i < cnt; ++i) {
         aOut.writeInt(this.date[i]);
         aOut.writeFloat2(this.open[i]);
         aOut.writeFloat2(this.close[i]);
         aOut.writeFloat2(this.lo[i]);
         aOut.writeFloat2(this.hi[i]);
         aOut.writeInt(this.volume[i]);
      }

   }

   public void writeTo2(xDataOutput aOut) {
      aOut.writeUTF(DBHelper.convertDBSymbolToVNChartSymbol(this.symbol));
      aOut.writeInt(this.candleFrame);
      int cnt = this.cursor;
      aOut.writeInt(cnt);

      for(int i = 0; i < cnt; ++i) {
         aOut.writeInt(this.date[i]);
         aOut.writeFloat2(this.open[i]);
         aOut.writeFloat2(this.close[i]);
         aOut.writeFloat2(this.lo[i]);
         aOut.writeFloat2(this.hi[i]);
         aOut.writeInt(this.volume[i]);
      }

   }

   public xDataOutput writeTo() {
      xDataOutput o = new xDataOutput(64 + this.cursor * CANDLE_SIZE);
      this.writeTo(o);
      return o;
   }

   public void writeToOutputForPacking(xDataOutput o, int candles) {
      o.writeUTF(DBHelper.convertDBSymbolToVNChartSymbol(this.symbol));
      int candleCnt = candles < this.cursor ? candles : this.cursor;
      int begin = this.cursor - candles;
      if (begin < 0) {
         begin = 0;
      }

      o.writeInt(candleCnt);

      for(int i = 0; i < candleCnt; ++i) {
         int idx = begin + i;
         o.writeInt(this.date[idx]);
         o.writeFloat2(this.open[idx]);
         o.writeFloat2(this.close[idx]);
         o.writeFloat2(this.hi[idx]);
         o.writeFloat2(this.lo[idx]);
         o.writeInt(this.volume[idx]);
      }

   }

   public static void readFromPacked(xDataInput di, CandlesData share) {
      try {
         share.symbol = di.readUTF();
         int candleCnt = di.readInt();
         share.prepare(candleCnt);

         for(int i = 0; i < candleCnt; ++i) {
            share.date[i] = di.readInt();
            share.open[i] = di.readFloat2();
            share.close[i] = di.readFloat2();
            share.hi[i] = di.readFloat2();
            share.lo[i] = di.readFloat2();
            share.volume[i] = di.readInt();
         }
      } catch (Throwable var4) {
      }

   }

   public int getAvgVol(int days) {
      int begin = this.cursor - days;
      if (this.cursor == 0) {
         return 0;
      } else if (begin >= 0 && days != 0) {
         int cnt = 0;
         int vol = 0;

         for(int i = begin; i < this.cursor; ++i) {
            vol += this.volume[i];
            ++cnt;
         }

         return vol / cnt;
      } else {
         return this.volume[this.cursor - 1];
      }
   }

   public void changeCandleType(int minutesPerCandle) {
      if (minutesPerCandle > 1) {
         if (this.isIntraday) {
            int cnt = this.getCandleCnt();
            int FIRST_MARK = -1412584499;
            int last = FIRST_MARK;
            if (cnt >= 4) {
               int D = 0;
               int V = 0;
               float C = 0.0F;
               float H = 0.0F;
               float L = 0.0F;
               float O = 0.0F;
               int j = 0;

               for(int i = 0; i < cnt; ++i) {
                  int ndate = this.date[i];
                  int dateInt = xUtils.dateFromPackagedDate(ndate);
                  int timeInt = xUtils.timeFromPackagedDate(ndate);
                  int hh = xUtils.EXTRACT_HOUR(timeInt);
                  int mm = xUtils.EXTRACT_MINUTE(timeInt);
                  int MM = hh * 60 + mm;
                  int idx = (MM + 5) / 60;
                  if (minutesPerCandle == 5) {
                     idx = (MM - 1) / 5;
                  } else if (minutesPerCandle == 10) {
                     idx = (MM - 1) / 10;
                  } else if (minutesPerCandle == 15) {
                     idx = (MM - 1) / 15;
                  } else if (minutesPerCandle == 30) {
                     idx = (MM - 1) / 30;
                  } else if (minutesPerCandle == 60) {
                     idx = (MM - 1) / 60;
                  }

                  if (idx != last) {
                     if (last != FIRST_MARK) {
                        this.close[j] = C;
                        this.date[j] = D;
                        this.hi[j] = H;
                        this.lo[j] = L;
                        this.open[j] = O;
                        this.volume[j] = V;
                        ++j;
                     }

                     O = this.open[i];
                     H = this.hi[i];
                     L = this.lo[i];
                     C = this.close[i];
                     D = this.date[i];
                     V = this.volume[i];
                     last = idx;
                  } else {
                     V += this.volume[i];
                     D = this.date[i];
                     C = this.close[i];
                     if (this.lo[i] < L) {
                        L = this.lo[i];
                     }

                     if (this.hi[i] > H) {
                        H = this.hi[i];
                     }

                     last = idx;
                  }
               }

               if (j < cnt) {
                  this.close[j] = C;
                  this.date[j] = D;
                  this.hi[j] = H;
                  this.lo[j] = L;
                  this.open[j] = O;
                  this.volume[j] = V;
                  ++j;
               }

               this.cursor = j;
            }
         }
      }
   }
}
