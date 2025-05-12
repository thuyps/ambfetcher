/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package xframe.framework;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 *
 * @author MAC
 */
public class PathCreator{
    public static Shape createRounedRectShape(float left, float top, float w, float h, float r)
    {
        // T/L----------B
        //  |           |
        //  |           |
        //  |           |
        //  C-----------D
        Path2D.Float path = new Path2D.Float();
        if (r > 0)
        {
            path.moveTo(left+r, top);

            path.lineTo(left+w - r, top);
            //  B
            path.curveTo(left+w-r, top, left+w, top, left+w, top+r);
            
            path.lineTo(left+w, top+h-r);
            //  D
            path.curveTo(left+w, top+h-r, left+w, top+h, left+w-r, top+h);
            path.lineTo(left+r, top+h);
            //  C
            path.curveTo(left+r, top+h, left, top+h, left, top+h-r);
            path.lineTo(left, top+r);
            
            //  A
            path.curveTo(left, top+r, left, top, left+r, top);
            
            path.closePath();
        }
        else{
            //  A
            path.moveTo(left, top);

            //  A-B
            path.lineTo(left+w, top);

            //  B-C
            path.lineTo(left+w, top+h);

            //  C-D
            path.lineTo(left, top+h);

            //  D-A
            path.lineTo(left, top);
            
            path.closePath();
        }
        
        return path;
    }
    
    static public Shape createTriangleShape(float x1, float y1, float x2, float y2, float x3, float y3){
        Path2D.Float path = new Path2D.Float();
        
        path.moveTo(x1, y1);

        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x1, y1);

        path.closePath();
        
        return path;
    }
    
    static public Shape createCirleShape(float x, float y, float radiu)
    {
        Ellipse2D.Float circle = new Ellipse2D.Float(x-radiu, y-radiu, 2*radiu, 2*radiu);
        return circle;
    }
    
    static public Path2D createPath(float[] x, float[] y, int offset, int cnt)
    {
        Path2D.Float path = new Path2D.Float();
        
        int t = offset;
        path.moveTo(x[t], y[t]);
        for (int i = 1; i < cnt; i++) {
            path.lineTo(x[t], y[t]);
        }
        
        return path;
    }
    
    static public Path2D createPath(float[] xy, int offset, int cnt)
    {
        Path2D.Float path = new Path2D.Float();
        
        int t = offset;
        path.moveTo(xy[t], xy[t+1]);
        for (int i = 1; i < cnt; i++) {
            t = offset + (i << 1);
            path.lineTo(xy[t], xy[t + 1]);
        }
        
        return path;
    }
    
    static public Path2D createClosedPath(float[] xy, int offset, int cnt)
    {
        Path2D.Float path = new Path2D.Float();
        
        int t = offset;
        path.moveTo(xy[t], xy[t+1]);
        for (int i = 1; i < cnt; i++) {
            t = offset + (i << 1);
            path.lineTo(xy[t], xy[t + 1]);
        }
        path.closePath();
        
        return path;
    }
}
