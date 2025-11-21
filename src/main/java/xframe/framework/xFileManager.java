/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xframe.framework;

import android.content.Context;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import xframe.utils.xUtils;

/**
 *
 * @author Thuy Pham
 */
public class xFileManager {

    public static File[] listAllFiles(String folder) {
        File[] files = null;

        try {
            File THIS = new File(folder);
            files = THIS.listFiles();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return files;
    }

    public static int getFileSize(String folder, String filename) {
        int size = 0;
        try {
            File file = new File(folder, filename);
            if (file.isFile()) {
                size = (int) file.length();
            }
        } catch (Throwable e) {

        }

        return size;
    }

    public static int getFileSize(String filepath) {
        int size = 0;
        try {
            File file = new File(filepath);
            if (file.isFile()) {
                size = (int) file.length();
            }
        } catch (Throwable e) {

        }

        return size;
    }

    public static xDataInput readFile(String folder, String file) {
        xDataOutput out = new xDataOutput(1024);

        if (_readFile(out, folder, file)) {
            return new xDataInput(out.getBytes(), 0, out.size());
        }

        return null;
    }

    public static xDataInput readFile(String filepath) {
        xDataOutput out = new xDataOutput(1024);

        if (_readFile(out, null, filepath)) {
            return new xDataInput(out.getBytes(), 0, out.size());
        }

        return null;
    }

    public static byte[] readFileAsBytes(String folder, String file) {
        xDataOutput out = new xDataOutput(1024);

        if (_readFile(out, folder, file)) {
            byte[] data = new byte[out.size()];

            System.arraycopy(out.getBytes(), 0, data, 0, out.size());
            return data;
        }

        return null;
    }

    public static String readFileAsString(String folder, String filename) {
        try {
            byte[] bytes;

            if (folder == null){
                bytes = Files.readAllBytes(Paths.get(filename));
            }
            else {
                bytes = Files.readAllBytes(Paths.get(folder, filename));
            }

            String content;
            // Kiểm tra BOM
            if (bytes.length >= 3 &&
                    bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                // UTF-8 BOM
                content = new String(bytes, StandardCharsets.UTF_8);
                content = content.substring(1); // bỏ BOM
            } else if (bytes.length >= 2 &&
                    bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                // UTF-16 LE BOM
                content = new String(bytes, StandardCharsets.UTF_16LE);
                content = content.substring(1); // bỏ BOM
            } else if (bytes.length >= 2 &&
                    bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                // UTF-16 BE BOM
                content = new String(bytes, StandardCharsets.UTF_16BE);
                content = content.substring(1); // bỏ BOM
            } else {
                // Mặc định: UTF-8 không BOM
                content = new String(bytes, StandardCharsets.UTF_8);
            }
            return content;
        }
        catch (Throwable e){
            e.printStackTrace();
        }

        /*
        xDataOutput out = new xDataOutput(1024);

        String s = null;
        if (_readFile(out, folder, file)) {
            //s = xUtils.utf8ToUnicode((byte[])out.getBytes(), 0, out.size());
            try {
                s = new String((byte[]) out.getBytes(), 0, out.size(), "utf-8");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {

        }

        return s;

         */
        return null;
    }

    //	folder == null: check in RMS
    public static boolean isFileExist(String folder, String file) {
        try {
            if (folder == null){
                File fileHandle = new File(file);
                return fileHandle.exists();
            }
            else{
                File fileHandle = new File(folder, file);
                return fileHandle.exists();
            }
            
        } catch (Throwable e) {

        }
        return false;
    }
    //	folder == null: check in RMS

    public static boolean isFileExist(String filepath) {
        try {
            File fileHandle = new File(filepath);
            return fileHandle.exists();
        } catch (Throwable e) {

        }
        return false;
    }

    static byte[] buffer = new byte[10 * 1024];

    //	folder == null: read from RMS
    public static boolean _readFile(xDataOutput out, String folder, String file) {
        FileInputStream rs = null;

//		xUtils.trace("====READ file from EXTERNAL CARD: " + folder + file);
        try {
            if (folder == null) {
                File fileHandle = new File(file);
                if (fileHandle.exists()) {
                    rs = new FileInputStream(fileHandle);
                } else {
                    //xUtils.trace(String.format("no file: %s", file));
                }
            } else {
                File fileHandle = new File(folder, file);
                if (fileHandle.exists()) {
                    rs = new FileInputStream(fileHandle);
                } else {
                    //xUtils.trace(String.format("no file: %s/%s", folder, file));
                }

            }

            if (rs != null) {
                int read = -1;
                int total = rs.available();
                if (total > 0) {
                    out.setSize(total);
                }
                while ((read = rs.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }

//				xUtils.trace("====READ file from EXTERNAL CARD success");
                return true;
            } else {

            }
        } catch (Throwable t) {
            xUtils.trace("====READ file from EXTERNAL CARD FAILED");
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        return false;
    }

    public static void appendDataToFile(byte[] data, int offset, int len, String folder, String file) {
        FileOutputStream rs = null;
        boolean ret = true;

        try {
            //xUtils.trace("====Append com.data to file - start " + folder + file);
            File fileHandle = new File(folder, file);

            rs = new FileOutputStream(fileHandle, true);
            rs.write(data, offset, len);
            //xUtils.trace("====Append com.data to file - end");
        } catch (Throwable t) {
            t.printStackTrace();
            ret = false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public static void saveFile(xDataOutput o, String folder, String file) {
        saveRecord(o.getBytes(), 0, o.size(), folder, file);
    }

    public static void saveFile(String content, String folder, String file) {
        byte p[] = content.getBytes();

        saveRecord(p, 0, p.length, folder, file);
    }

    public static void saveFile(byte[] data, int offset, int len, String folder, String file) {
        saveRecord(data, offset, len, folder, file);
    }

    public static void saveRecord(xDataOutput o, String folder, String file) {
        saveRecord(o.getBytes(), 0, o.size(), folder, file);
    }

    //	dir = mydir/		;	end with a slash
    static boolean mSDCardOK = true;

    public static void createDirOnExternalStore(String path) {
        try {
            File myDir = new File(path);
            if (!myDir.exists()) {
                myDir.mkdir();
            }
        } catch (Throwable e) {
            mSDCardOK = false;
        }
    }

    public static void removeDirOnExternalStore(String path) {
        try {
            File myDir = new File(path);
            if (myDir.exists()) {
                myDir.delete();
            }
        } catch (Throwable e) {
        }
    }

    public static void removeContentsOfFolder(String folder) {
        File thisFolder = new File(folder);

        File[] childs = thisFolder.listFiles();
        
        if (childs != null){
            for (File child : childs) {
                removeFolderAndItsContents(child);

                child.delete();
            }
        }
    }

    public static void removeFolderAndItsContents(File folder) {
        File[] childs = folder.listFiles();
        if (childs != null){
            for (File child : childs) {
                if (child.isDirectory()) {
                    removeFolderAndItsContents(child);
                }
                child.delete();
            }
        }
    }

    static String removePathFromFile(String file) {
        //file = file.replace('/', '_');
        return file;
    }

    public static boolean saveRecord(byte[] data, int offset, int len,
            String folder, String file) {
        FileOutputStream rs = null;
        boolean ret = true;

        try {
            createAllDirsByFilePath(folder + "/" + file);
            File fileHandle = new File(folder, file);
            if (fileHandle.exists()) {
                fileHandle.delete();
            }

            rs = new FileOutputStream(fileHandle);
            rs.write(data, offset, len);
        } catch (Throwable t) {
            xUtils.trace("====SAVE file on EXTERNAL CARD failed");
            t.printStackTrace();
            ret = false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        return ret;
    }

    public static void _removeFile(String folder, String file) {
        try {
            File fileHandle = new File(folder, file);
            if (fileHandle.exists()) {
                fileHandle.delete();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void removeFile(String folder, String file) {
        _removeFile(folder, file);
    }

    public static void removeFile(String filepath) {
        try {
            File f = new File(filepath);
            f.delete();
        } catch (Throwable e) {

        }
    }

    public static void createAllDirs(String path) {
        String[] paths = path.split("[/]");
        String fullPath = "";
        for (int i = 0; i < paths.length; ++i) {
            if (!paths[i].equals("")) {
                fullPath += "/" + paths[i];
                if (!isFolderExist(fullPath)) {
                    createFolder(fullPath);
                }
            }
        }
    }

    public static void createAllDirsByFilePath(String filePath) {
        String[] paths = filePath.split("[/]");
        if (paths.length <= 1) {
            return;
        }
        String fullPath = "";
        for (int i = 0; i < paths.length - 1; ++i) {
            if (!paths[i].equals("")) {
                fullPath += "/" + paths[i];
                if (!isFolderExist(fullPath)) {
                    createFolder(fullPath);
                }
            }
        }
    }

    //	folder == null: check in RMS
    public static boolean isFolderExist(String folder) {
        try {
            File fileHandle = new File(folder);
            return fileHandle.isDirectory();
        } catch (Throwable e) {
        }
        return false;
    }

    public static boolean createFolder(String folderPath) {
        try {
            File fileHandle = new File(folderPath);
            return fileHandle.mkdir();
        } catch (Throwable e) {
        }
        return false;
    }

    public static boolean renameFile(String src, String dst) {
        if (xFileManager.isFileExist(src)) {
            removeFile(dst);

            File fileSrc = new File(src);
            File fileDst = new File(dst);

            fileSrc.renameTo(fileDst);

            return true;
        }

        return false;
    }
    
    static String _fullpath = null;
    public static String getFolderFullpath(Context context){
        if (_fullpath == null){
            _fullpath = context.getExternalFilesDir(null);
        }
        return _fullpath;
    }
    
    public static String getFolderFullpath(Context context, String folder){
        String workingFolder = getFolderFullpath(context);
        
        String fullpath = workingFolder + File.separator + folder;
        return fullpath;
    }
    
    static public String readResourceAsString(String resource){
        try {
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource);
            //byte[] p = stream.readAllBytes();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byte[] p = buffer.toByteArray(); // Đây là mảng byte bạn cần
            return new String(p, 0, p.length);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    static public InputStream readResourceAsStream(String resource){
        try {
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource);
            return stream;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static long timeSecondsSinceModified(String folder, String filename)
    {
            try{
                    if (isFileExist(folder, filename)){
                            File f = new File(folder, filename);
                            long modified = f.lastModified();

                            long now = System.currentTimeMillis();

                            long delta = (now - modified)/1000; //  to seconds
                            return delta;
                    }
            }
            catch(Throwable e){
                    e.printStackTrace();
            }

            return 1000000;
    }
    
    public static void setFileManager(Context context){
        
    }
    
    public static void copyFile(String folder1, String file1, 
            String folder2, String file2)
    {
        try{
            if (file1.compareTo("*") == 0){
                File sourceDirectory = new File(folder1);
                File targetDirectory = new File(folder2);
                FileUtils.copyDirectory(sourceDirectory, targetDirectory);
            }
            else{
                Path source = Paths.get(folder1 + "/" + file1);
                Path target = Paths.get(folder2 + "/" + file2);

                try {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    //System.out.println("File copied successfully!");
                } catch (IOException e) {
                    e.printStackTrace();
                }    
            }
        }
        catch(Throwable e){
            
        }
    }
    
}
