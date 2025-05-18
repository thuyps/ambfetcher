/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xframe.framework;

import java.io.RandomAccessFile;

/**
 *
 * @author Administrator
 */
public class xDataInputRandomAccessFile {
    xDataInput _di;
    RandomAccessFile _file;
    int _fileLength;
    byte[] _buffer;
    public xDataInputRandomAccessFile(String filepath){
        try{
            RandomAccessFile f = new RandomAccessFile(filepath, "r");
            _file = f;
            _fileLength = (int)_file.length();
            _di = new xDataInput(1);
        }
        catch(Throwable e){
        }
    }
    
    public void close(){
        try{
            if (_file != null){
                _file.close();
            }
        }
        catch(Throwable e){
            
        }
    }
    
    public int fileSize(){
        return _fileLength;
    }
    public xDataInput DI(){
        return _di;
    }
    public xDataInput seekTo(int offset, int dataSize){
        if (_buffer == null || _buffer.length < dataSize){
            int size = dataSize < 10*1024?10*1024:dataSize;
            _buffer = new byte[size];
        }
        try{
            _file.seek(offset);
            dataSize = (offset+dataSize<=_fileLength)?dataSize:(_fileLength-offset);
            _file.read(_buffer, 0, dataSize);
            _di.bind(_buffer, dataSize);
        }
        catch(Throwable e){
            
        }
        
        return _di;
    }
}
