/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
/** Straight-line interception: |relativePosition + targetVelocity*t| = bulletSpeed*t. */
final class InterceptAim {
    private InterceptAim() {}

    /** Earliest non-negative interception time; NaN when no future intercept exists. */
    static double time(double rx,double ry,double vx,double vy,double speed) {
        if(!Double.isFinite(rx)||!Double.isFinite(ry)||!Double.isFinite(vx)
                ||!Double.isFinite(vy)||!Double.isFinite(speed)||speed<=0)return Double.NaN;
        double c=rx*rx+ry*ry;
        if(c==0)return 0;
        double vv=vx*vx+vy*vy,a=vv-speed*speed,b=2*(rx*vx+ry*vy);
        // Equal speeds reduce the quadratic to a linear equation.
        if(Math.abs(a)<=1e-12*Math.max(1,vv+speed*speed)) {
            if(b==0)return Double.NaN;
            double t=-c/b;
            return t>0&&Double.isFinite(t)?t:Double.NaN;
        }
        double discriminant=b*b-4*a*c;
        double tolerance=1e-12*Math.max(1,b*b+Math.abs(4*a*c));
        if(discriminant < -tolerance)return Double.NaN;
        double root=Math.sqrt(Math.max(0,discriminant));
        // Stable quadratic roots avoid cancellation when b is large.
        double q=-0.5*(b+Math.copySign(root,b));
        double t1=q/a,t2=q==0?Double.NaN:c/q;
        double t=Double.POSITIVE_INFINITY;
        if(t1>0&&Double.isFinite(t1))t=t1;
        if(t2>0&&Double.isFinite(t2))t=Math.min(t,t2);
        return Double.isFinite(t)?t:Double.NaN;
    }

    /** Bullet velocity of exactly speed; direct aim if interception is impossible. */
    static double[] velocity(double gunX,double gunY,double shipX,double shipY,
                             double shipVx,double shipVy,double speed) {
        double rx=shipX-gunX,ry=shipY-gunY;
        double t=time(rx,ry,shipVx,shipVy,speed);
        if(Double.isFinite(t)){rx+=shipVx*t;ry+=shipVy*t;}
        double distance=Math.hypot(rx,ry);
        if(distance<1e-9)return new double[]{0,-speed};
        return new double[]{rx/distance*speed,ry/distance*speed};
    }
}
