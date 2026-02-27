package com.data;

public class DBHelper {
   public static String convertVNChartSymbolToDB(String symbol) {
      if (!symbol.contains(".VC") && !symbol.contains(".COM") && !symbol.contains(".FX")) {
         if (symbol.contains(".C") && !symbol.contains(".VC")) {
            return symbol.replace(".C", ".VC");
         } else if (symbol.contains(".F") && !symbol.contains(".FX")) {
            return symbol.replace(".F", ".FX");
         } else if (symbol.contains(".M")) {
            return symbol.replace(".M", ".COM");
         } else {
            return symbol.contains(".US") ? symbol.replace(".US", "") : symbol;
         }
      } else {
         return symbol;
      }
   }

   public static String convertDBSymbolToVNChartSymbol(String symbol) {
      if (symbol != null && symbol.indexOf(".VC") > 0) {
         return symbol.replace(".VC", ".C");
      } else if (symbol != null && symbol.indexOf(".FX") > 0) {
         return symbol.replace(".FX", ".F");
      } else if (symbol != null && symbol.indexOf(".COM") > 0) {
         return symbol.replace(".COM", ".M");
      } else {
         return symbol;// != null && !symbol.contains(".") ? symbol;
      }
   }

   public static String convertVNChartMarketToDB(String market) {
      if (market != null && market.equalsIgnoreCase("C")) {
         return ".VC";
      } else if (market != null && market.equalsIgnoreCase("F")) {
         return ".FX";
      } else if (market != null && market.equalsIgnoreCase("M")) {
         return ".COM";
      } else {
         return market != null && market.equalsIgnoreCase("US") ? "" : market;
      }
   }
}
