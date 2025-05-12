/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package xframe.framework;

/**
 *
 * @author ThuyPham
 */
public class xVectorInt {
    static final int INC_SIZE       = 16;
    int[] mIntData;
    int mSize;

    public xVectorInt(){
        mSize = 0;
        mIntData = new int[INC_SIZE];
    }

    public xVectorInt(int initSize){
        mSize = 0;
        mIntData = new int[initSize];
    }

    void allocBuffer(){
        if (mSize >= mIntData.length){
            int[] p = new int[mIntData.length + INC_SIZE];
            System.arraycopy(mIntData, 0, p, 0, mSize);
            mIntData = p;
            p = null;
        }
    }

    public int size(){
        return mSize;
    }

    public void addElement(int v){
        allocBuffer();
        mIntData[mSize++] = v;
    }

    public void removeAllElements(){
        mSize = 0;
    }

    public int elementAt(int idx){
        if (idx >= 0 && idx < mSize){
            return mIntData[idx];
        }
        return 0;
    }

    public void insertElementAt(int v, int idx) {
        mSize++;
        allocBuffer();
        for (int i = mSize - 1; i > idx; i--) {
            mIntData[i] = mIntData[i-1];
        }
        mIntData[idx] = v;
    }
    
    public int firstElement()
    {
        if (mSize > 0)
            return mIntData[0];

        return 0;
    }    

    public int lastElement(){
        if (mSize > 0)
            return mIntData[mSize-1];

        return 0;
    }

    public void removeElementAt(int idx){
        if (idx >= 0 && idx < mSize){
            for (int i = idx; i < mSize-1; i++){
                mIntData[i] = mIntData[i+1];
            }
            mSize--;
        }
    }
    
    public void setElementAt(int idx, int v){
        if (idx >= 0 && idx < mSize){
            mIntData[idx] = v;
        }
    }

    public boolean contains(int id){
        for (int i = 0; i < mSize; i++) {
            if (id == mIntData[i])
                return true;
        }

        return false;
    }    
}
