package com.data;


/*
import org.json.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
*/
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import xframe.framework.xFileManager;



public class VTDictionary extends JSONObject {
    public VTDictionary(){

    }
    public VTDictionary(String json) throws JSONException {
        super(json);
    }


    public VTDictionary(JSONObject json) throws JSONException {
        super(json.toString());
    }

    public static VTDictionary createDictionary(String key, Object o) {
        VTDictionary dictionary = new VTDictionary();
        dictionary.setValue(o, key);
        return dictionary;
    }

    public static VTDictionary createDictionary(JSONObject jsonObject){
        if (jsonObject == null){
            return null;
        }
        try{
            String json = jsonObject.toString();
            return createDictionary(json);
        }
        catch (Throwable e){

        }
        return null;
    }

    public static VTDictionary createDictionary(HashMap<String, Object> hashMap){
        if (hashMap == null){
            return null;
        }
        try{
            VTDictionary dictionary = new VTDictionary();
            for (String key: hashMap.keySet())
            {
                Object v = hashMap.get(key);
                dictionary.setValue(v, key);
            }
            return dictionary;
        }
        catch (Throwable e){

        }
        return null;
    }

    public static VTDictionary createDictionary(String json)
    {
        if (json == null){
            return new VTDictionary();
        }
        try {
            VTDictionary dictionary = new VTDictionary(json);
            return dictionary;
        }
        catch (Throwable e){
            e.printStackTrace();
        }

        return null;
    }

    public void safeSetObject(Object o, String key){
        if (o != null){
            setValue(o, key);
        }
    }


    public void setValueInt(int value, String key){
        try {
            this.put(key, Integer.valueOf(value));
        }
        catch (Throwable e){

        }
    }

    public int getValueInt(String key, int defaultValue){
        try {
            return getInt(key);
        }
        catch (Throwable e){

        }
        return defaultValue;
    }

    public void setValueLong(long value, String key){
        try {
            this.put(key, Long.valueOf(value));
        }
        catch (Throwable e){

        }
    }

    public long getValueLong(String key, long defaultValue){
        try {
            return getLong(key);
        }
        catch (Throwable e){

        }
        return defaultValue;
    }

    public void setValueDouble(double value, String key){
        try {
            this.put(key, Double.valueOf(value));
        }
        catch (Throwable e){

        }
    }

    public double getValueDouble(String key, double defaultValue){
        double d = 0;
        try {
            d = getDouble(key);
        }
        catch (Throwable e){

        }
        
        if (d == 0){
            try{
                String s = getString(key);
                s = s.replace(",", "");
                
                return Double.parseDouble(s);
            }
            catch(Throwable e){
                
            }
        }
        else{
            return d;
        }
        
        return defaultValue;
    }
    
    public float getValueFloat(String key, float defaultValue){
        try {
            return (float)getValueDouble(key, defaultValue);
        }
        catch (Throwable e){

        }
        return defaultValue;
    }

    public void setValueBool(boolean value, String key){
        try {
            this.put(key, Boolean.valueOf(value));
        }
        catch (Throwable e){

        }
    }

    public boolean getValueBool(String key, boolean defaultValue){
        try {
            Object o = getObject(key);
            if (o instanceof Boolean){
                return ((Boolean)o).booleanValue();
            }
            else if (o instanceof Number){
                return ((Number)o).intValue() == 1?true:false;
            }
            else if (o instanceof String){
                String s = (String)o;
                if (s.equalsIgnoreCase("true")) return true;
                else if (s.equalsIgnoreCase("false")) return false;
                else if (s.equalsIgnoreCase("yes")) return true;
                else if (s.equalsIgnoreCase("no")) return false;

                return s.equalsIgnoreCase("1")?true:false;
            }
        }
        catch (Throwable e){

        }
        return defaultValue;
    }

    public void setValueString(String value, String key){
        try {
            if (value == null){
                this.removeObject(key);
            }
            else {
                this.put(key, value);
            }
        }
        catch (Throwable e){

        }
    }

    public String getValueString(String key, String defaultValue){
        try {
            String s = getString(key);
            if (s != null && s.equals("null")){
                s = "";
            }
            return s;
        }
        catch (Throwable e){

        }
        return defaultValue;
    }

    public Object valueForKey(String key){
        return getValue(key, null);
    }
    public String valueForKeyAsString(String key){
        return getValueString(key, null);
    }

    public VTDictionary valueForKeyAsDictionary(String key){
        try{
            Object obj = valueForKey(key);
            if (obj instanceof VTDictionary){
                return (VTDictionary)obj;
            }
            else if (obj instanceof JSONObject){
                return VTDictionary.createDictionary((JSONObject) obj);
            }
            else {
                String json = valueForKeyAsString(key);
                return VTDictionary.createDictionary(json);
            }

        }
        catch (Throwable e){

        }
        return null;
    }

    public void setValue(Object value, String key){
        try {
            this.put(key, value);
        }
        catch (Throwable e){

        }
    }

    public void setValueKO(String key, Object value){
        try {
            this.put(key, value);
        }
        catch (Throwable e){

        }
    }

    public void removeObject(String key){
        if (hasObject(key)){
            super.remove(key);
        }
    }

    public Object getValue(String key, Object defaultValue){
        Object o = getObject(key);

        return o != null?o:defaultValue;
    }

    public boolean hasObject(String key){
        return has(key);
    }
    public Object getObject(String key){
        if (has(key)){
            try {
                if (isNull(key)) {
                    return null;
                }
                return get(key);
            }
            catch (Throwable e){

            }
        }
        return null;
    }
    //  compatible
    public boolean objectForKeyAsBoolean(String key){
        return getValueBool(key, false);
    }

    public String objectForKey(String key){
        return objectForKeyAsString(key);
    }

    public int objectForKeyAsInt(String key){
        return getValueInt(key, 0);
    }
    public long objectForKeyAsLong(String key){
        return getValueLong(key, 0);
    }
    public double objectForKeyAsDouble(String key){
        return getValueDouble(key, 0.0f);
    }
    public float objectForKeyAsFloat(String key){
        return (float) getValueDouble(key, 0.0f);
    }
    public String objectForKeyAsString(String key){
        return getValueString(key, null);
    }
    
    public String objectForKeyAsString(String key, String defValue){
        String s = getValueString(key, null);
        if (s == null){
            s = defValue;
        }
        return s;
    }

    public VTDictionary objectForKeyAsDictionary(String key){
        try{
            if (hasObject(key)){
                Object o = getObject(key);
                if (o instanceof VTDictionary){
                    return (VTDictionary)o;
                }
                else if (o instanceof JSONObject){
                    JSONObject jsonObject = getJSONObject(key);
                    VTDictionary vtDictionary = VTDictionary.createDictionary(jsonObject);
                    return vtDictionary;
                }
                else{
                    String s = getValueString(key, null);
                    VTDictionary vtDictionary = VTDictionary.createDictionary(s);
                    return vtDictionary;
                }
            }
        }
        catch (Throwable e){

        }
        return null;
    }
    //===========================================

    public void saveToFile(String filename)
    {
        //saveToFile(C.FOLDER_APP, filename);
    }

    public void saveToFile(String folder, String filename)
    {
        String json = this.toString();
        byte[] p = json.getBytes();
        xFileManager.saveFile(p, 0, p.length, folder, filename);
    }

    static public VTDictionary loadFromFile(String folder, String filename)
    {
        try {
            String json = xFileManager.readFileAsString(folder, filename);
            VTDictionary dictionary = new VTDictionary(json);
            return dictionary;
        }
        catch (Throwable e){
            //e.printStackTrace();
        }
        
        return new VTDictionary();
    }

    public ArrayList<VTDictionary> objectForKeyAsArray(String key){
        return getArrayDictionary(key);
    }

    public Object objectForKeyO(String key){
        return getObject(key);
    }

    public ArrayList<VTDictionary> getArrayDictionary(String key){
        try{
            if (hasObject(key)){
                Object o = getObject(key);
                JSONArray array;
                if (o instanceof String){
                    array = new JSONArray((String)o);
                }
                else if (o instanceof ArrayList){
                    return (ArrayList<VTDictionary>)o;
                }
                else {
                    array = getJSONArray(key);
                }

                if (array != null){
                    ArrayList<VTDictionary> vtDictionaryArrayList = new ArrayList<VTDictionary>();
                    for (int i = 0; i < array.length(); i++){
                        JSONObject jsonObject = array.getJSONObject(i);
                        VTDictionary dictionary = VTDictionary.createDictionary(jsonObject);
                        vtDictionaryArrayList.add(dictionary);
                    }

                    return vtDictionaryArrayList;
                }
            }


        }
        catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public void setValueArrayDictionary(ArrayList<VTDictionary> array, String key) {
        try {
            JSONArray jsonArray = new JSONArray();
            if (array != null) {
                for (VTDictionary dic : array) {
                    jsonArray.put(dic);
                }
            }
            put(key, jsonArray);
        } catch (Throwable e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getArrayString(String key){

        JSONArray arr = null;
        try {
            arr = getJSONArray(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (arr == null) {
            try {
                String infoArr = getString(key);
                arr = new JSONArray(infoArr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (arr != null){
            try {
                ArrayList<String> vtDictionaryArrayList = new ArrayList<String>();
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.getString(i);
                    vtDictionaryArrayList.add(s);
                }
                return vtDictionaryArrayList;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;

        /*try{
            JSONArray array = getJSONArray(key);
            if (array != null){
                ArrayList<String> vtDictionaryArrayList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++){
                    String s = array.getString(i);

                    vtDictionaryArrayList.add(s);
                }

                return vtDictionaryArrayList;
            }
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        return null;*/
    }

    public static < E > void printArray( E[] inputArray ) {
        // Display array elements
        for(E element : inputArray) {
            System.out.printf("%s ", element);
        }
        System.out.println();
    }

    public ArrayList<Long> getArrayLong(String key){
        try{
            ArrayList<Long> longs = new ArrayList<Long>();
            JSONArray array = getJSONArray(key);
            if (array != null){
                for (int i = 0; i < array.length(); i++){
                    Object item = array.get(i);

                    long v = 0;

                    if (item instanceof Boolean) v = 0;
                    else if (item instanceof Number){
                        v = ((Number)item).longValue();
                    }

                    longs.add(v);
                }

                return longs;
            }
        }
        catch (Throwable e){
            //e.printStackTrace();
            //---------------------------

            Object o = getObject(key);
            if (o instanceof String) {
                String s = (String) o;

                try {
                    JSONArray jsonArray = new JSONArray(s);
                    if (jsonArray != null) {
                        ArrayList<Long> longs = new ArrayList<Long>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            Object item = jsonArray.get(i);

                            long v = 0;

                            if (item instanceof Boolean) v = 0;
                            else if (item instanceof Number) {
                                v = ((Number) item).longValue();
                            }

                            longs.add(v);
                        }

                        return longs;
                    }
                } catch (Throwable e2) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    static public JSONArray arrayJavaToJArrayJSON(Object o){
        try{
            if (o instanceof ArrayList)
            {
                JSONArray jsonArray = new JSONArray();
                ArrayList arr = (ArrayList)o;
                for (Object item: arr)
                {
                    jsonArray.put(item);
                }

                return jsonArray;
            }
            else if (o instanceof List){
                JSONArray jsonArray = new JSONArray();
                List arr = (List)o;
                for (Object item: arr)
                {
                    jsonArray.put(item);
                }

                return jsonArray;
            }
            else if (o instanceof Vector){
                JSONArray jsonArray = new JSONArray();
                Vector arr = (Vector) o;
                for (Object item: arr)
                {
                    jsonArray.put(item);
                }

                return jsonArray;
            }
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public void setValueArrayString(ArrayList<String> array, String key) {
        try {
            JSONArray jsonArray = new JSONArray();
            if (array != null) {
                for (String s: array) {
                    jsonArray.put(s);
                }
            }
            put(key, jsonArray);
        } catch (Throwable e){
            e.printStackTrace();
        }
    }
    
    public ArrayList<String> getKeysAsArray(){
        ArrayList<String> arr = new ArrayList<>();
        try {
            Iterator<String> keysItr = this.keys();

            while (keysItr.hasNext()) {
                String k = keysItr.next();
                arr.add(k);
            }
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        
        return arr;
    }

    public static Map<String, Object> toMap(JSONObject object){
        try {
            Map<String, Object> map = new HashMap<String, Object>();

            Iterator<String> keysItr = object.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = object.get(key);

                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                map.put(key, value);
            }
            return map;
        }
        catch (Throwable e){
            e.printStackTrace();
        }
        return new HashMap<String, Object>();
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public void copyFromDictionary(VTDictionary from)
    {
        try {
            Iterator<String> keysItr = from.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = from.get(key);

                setValue(value, key);
            }
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }

    public int count(){
        Iterator<String> keysItr = this.keys();

        int cnt = 0;
        while (keysItr.hasNext()) {
            keysItr.next();
            cnt++;
        }

        return cnt;
    }

    public String toJSON(){
        return toString();
    }

    public boolean isValid(){
        //  for test only

        return true;
    }


    public HashMap<String, Object> getHashMap2(){
        HashMap hashMap = new HashMap();

        Iterator<String> keys = keys();

        while (keys.hasNext()) {
            String key = keys.next();
            hashMap.put(key, getObject(key));
        }

        return hashMap;
    }
    
    public HashMap<String, String> getHashMapSS(){
        HashMap hashMap = new HashMap();

        Iterator<String> keys = keys();

        while (keys.hasNext()) {
            String key = keys.next();
            hashMap.put(key, getValueString(key, ""));
        }

        return hashMap;
    }

    public void clear(){
        getHashMap2().clear();
    }
}
