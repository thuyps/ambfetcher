package xframe.framework;

import java.util.ArrayList;
import java.util.Vector;



public class xVector {
	Vector<Object> mArray;
    public xVector(int capacity)
    {
        mArray = new Vector<Object>(capacity);
    }
    public xVector()
    {
    	mArray = new Vector<Object>(10);
    }

    public void addElement(Object o)
    {
        mArray.add(o);
    }

    public Object elementAt(int idx)
    {
        if (idx < mArray.size() && idx >= 0)
        {
            return mArray.elementAt(idx);
        }

        return null;
    }

    public int size()
    {
        return mArray.size();
    }

    public boolean contains(Object o)
    {
        return mArray.contains(o);
    }

    public void removeAllElements()
    {
        mArray.clear();
    }

    public void removeElement(Object o)
    {
        mArray.remove(o);
    }

    public void removeElementAt(int idx)
    {
        if (idx >= 0 && idx < size())
            mArray.remove(idx);
    }

    public boolean isEmpty()
    {
        return mArray.size() == 0;
    }

    public void insertElementAt(Object o, int at)
    {
        if (at < mArray.size())
            mArray.insertElementAt(o, at);
        else
            mArray.add(o);
    }

    public Object firstElement()
    {
        if (mArray.size() > 0)
        {
            return mArray.firstElement();
        }

        return null;
    }

    public Object lastElement()
    {
        if (mArray.size() > 0)
        {
            return mArray.lastElement();
        }

        return null;
    }

    public Object pop()
    {
        Object o = null;
        if (mArray.size() > 0)
        {
            int idx = mArray.size() - 1;
            o = mArray.elementAt(idx);
            mArray.remove(idx);
        }

        return o;
    }

    public void swap(int idx1, int idx2)
    {
    	if (idx1 == idx2)
    		return;
        if ((idx1 >= 0 && idx1 < mArray.size())
            && (idx2 >= 0 && idx2 < mArray.size()))
        {
            Object tmp = mArray.elementAt(idx1);
            Object tmp2 = mArray.elementAt(idx2);
            mArray.set(idx1, tmp2);
            mArray.set(idx2, tmp);
        }
    }
/*
    public List<Object> getInternalList()
    {
        return mArray;
    }
*/
    public void makeReverse()
    {
    	ArrayList<Object> a;
    	
        if (mArray == null)
            return;
        
        int cnt = mArray.size();
        Vector<Object> v = new Vector<Object>(cnt);
        for (int i = 0; i < cnt; i++)
        {
        	v.add(mArray.elementAt(cnt-1-i));
        }
        mArray = v;
    }
    
    public Object removeLastElement(){
    	if (mArray.size() > 0){
    		Object o = mArray.lastElement();
    		mArray.removeElementAt(mArray.size()-1);
    		
    		return o;
    	}
    	
    	return null;
    }
    
    public void sortAsStrings()
    {
    	int cnt = size();
    	for (int i = 0; i < cnt-1; i++)
    	{
    		String s0 = (String)mArray.elementAt(i);
    		int idx0 = i;
    		for (int j = i+1; j < cnt; j++)
    		{
    			String s1 = (String)mArray.elementAt(j);
    			//	s1 is smallest
    			if (s1.compareTo(s0) < 0)
    			{
    				s0 = s1;
    				idx0 = j;
    			}
    		}
    		
    		swap(i, idx0);
    	}
    }
    
    public xVector clone()
    {
    	if (size() == 0)
    		return new xVector(1);
    	xVector v = new xVector(size());
    	
    	for (int i = 0; i < size(); i++)
    	{
    		v.addElement(elementAt(i));
    	}
    	return v;
    }
    
    public void reverse()
    {
    	int cnt = size();
    	int mid = cnt/2;
    	for (int i = 0; i < mid; i++)
    	{
    		swap(i, cnt-i-1);
    	}
    }
}
