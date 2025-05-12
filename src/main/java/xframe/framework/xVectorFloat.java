/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package xframe.framework;

/**
 *
 * @author ThuyPham
 */
public class xVectorFloat {
    static final int INC_SIZE       = 16;
    float[] mFloatData;
    int mSize;

    public xVectorFloat(){
        mSize = 0;
        mFloatData = new float[INC_SIZE];
    }

    public xVectorFloat(int initSize){
        mSize = 0;
        mFloatData = new float[initSize];
    }

    void allocBuffer(){
        if (mSize >= mFloatData.length){
            float[] p = new float[mFloatData.length + INC_SIZE];
            System.arraycopy(mFloatData, 0, p, 0, mSize);
            mFloatData = p;
            p = null;
        }
    }

    public int size(){
        return mSize;
    }

    public void addElement(float v){
        allocBuffer();
        mFloatData[mSize++] = v;
    }

    public void removeAllElements(){
        mSize = 0;
    }

    public float elementAt(int idx){
        if (idx >= 0 && idx < mSize){
            return mFloatData[idx];
        }
        return 0;
    }

    public void insertElementAt(int v, int idx) {
        mSize++;
        allocBuffer();
        for (int i = mSize - 1; i > idx; i--) {
            mFloatData[i] = mFloatData[i-1];
        }
        mFloatData[idx] = v;
    }
    
    public float firstElement()
    {
        if (mSize > 0)
            return mFloatData[0];

        return 0;
    }    

    public float lastElement(){
        if (mSize > 0)
            return mFloatData[mSize-1];

        return 0;
    }

    public void removeElementAt(int idx){
        if (idx >= 0 && idx < mSize){
            for (int i = idx; i < mSize-1; i++){
                mFloatData[i] = mFloatData[i+1];
            }
            mSize--;
        }
    }
    
    public void setElementAt(int idx, int v){
        if (idx >= 0 && idx < mSize){
            mFloatData[idx] = v;
        }
    }

    public boolean contains(int id){
        for (int i = 0; i < mSize; i++) {
            if (id == mFloatData[i])
                return true;
        }

        return false;
    }    
}
