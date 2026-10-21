/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.util.Random;

/** Continuous deterministic terrain; adjoining sections share their boundary knots. */
final class Terrain {
    static final int SECTION_LENGTH=4000, STEP=80;
    static final String[] NAMES={"RIDGELINE","MISSILE VALLEY","THE CAVERNS","FORTRESS RUN"};
    static int section(double worldX){return Math.max(0,(int)Math.floor(worldX/SECTION_LENGTH));}
    static String name(double worldX){return NAMES[section(worldX)%NAMES.length];}
    double floor(double x){return interpolate(x,false);}
    double ceiling(double x){return interpolate(x,true);}
    private double interpolate(double x,boolean top){
        x=Math.max(0,x);long knot=(long)Math.floor(x/STEP);
        double t=(x-knot*STEP)/STEP;t=t*t*(3-2*t);
        return value(knot,top)*(1-t)+value(knot+1,top)*t;
    }
    private double value(long knot,boolean top){
        Random r=new Random(knot*6364136223846793005L+739391L);
        double ground=450+r.nextDouble()*134;
        double roof=section(knot*STEP)%4==2?158+r.nextDouble()*105:88;
        return top?Math.min(roof,ground-190):ground;
    }
    boolean hits(double x,double y,double rx,double ry){
        for(double offset:new double[]{-rx,0,rx})
            if(y+ry>=floor(x+offset)||y-ry<=ceiling(x+offset))return true;
        return false;
    }
    double corridorMiddle(double x){
        double roof=Math.max(ceiling(x-22),Math.max(ceiling(x),ceiling(x+22)));
        double ground=Math.min(floor(x-22),Math.min(floor(x),floor(x+22)));
        return (roof+ground)/2;
    }
}
