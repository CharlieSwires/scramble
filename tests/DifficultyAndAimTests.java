import java.util.Random;
/** Closed-form geometry, integration and progression regression checks. */
public final class DifficultyAndAimTests {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void near(double actual,double expected,double epsilon,String label){check(Math.abs(actual-expected)<=epsilon,label+": "+actual+" != "+expected);}
    static void intercept(double rx,double ry,double vx,double vy,double speed){
        double t=InterceptAim.time(rx,ry,vx,vy,speed);check(Double.isFinite(t)&&t>=0,"future intercept");
        double[] bullet=InterceptAim.velocity(0,0,rx,ry,vx,vy,speed);
        near(Math.hypot(bullet[0],bullet[1]),speed,1e-8,"constant bullet speed");
        near(bullet[0]*t,rx+vx*t,1e-6,"X intersection");near(bullet[1]*t,ry+vy*t,1e-6,"Y intersection");
        near(Math.hypot(rx+vx*t,ry+vy*t),speed*t,1e-6,"expanding circle intersection");
    }
    public static void main(String[] args){
        near(InterceptAim.time(100,0,0,0,200),0.5,1e-12,"stationary ship");
        near(InterceptAim.time(100,0,-200,0,200),0.25,1e-12,"equal speed approaching");
        check(Double.isNaN(InterceptAim.time(100,0,200,0,200)),"equal speed escaping");
        check(Double.isNaN(InterceptAim.time(100,0,300,0,200)),"faster escaping target");
        check(Double.isNaN(InterceptAim.time(100,0,0,300,200)),"negative discriminant");
        near(InterceptAim.time(100,0,-300,0,200),0.2,1e-12,"earliest of two positive roots");
        near(InterceptAim.time(100,0,-100,100,100),1,1e-10,"tangent double root");
        near(InterceptAim.time(0,0,50,80,200),0,0,"coincident positions");
        intercept(100,0,-200,0,200);intercept(100,0,-300,0,200);intercept(100,0,-100,100,100);
        intercept(-400,-230,150,0,220);intercept(400,-230,150,-90,220);intercept(150,110,-150,150,220);
        double[] fallback=InterceptAim.velocity(0,0,100,0,300,0,200);
        near(fallback[0],200,1e-10,"no solution direct-aim fallback");near(fallback[1],0,0,"fallback Y");
        Random random=new Random(123);
        for(int i=0;i<2000;i++){
            double rx=random.nextDouble()*1600-800,ry=random.nextDouble()*900-450;
            double angle=random.nextDouble()*Math.PI*2,speed=random.nextDouble()*200;
            intercept(rx,ry,Math.cos(angle)*speed,Math.sin(angle)*speed,220);
        }
        ScrambleModel m=new ScrambleModel(new Random(42),e->{});m.startGame();
        for(int cycle=0;cycle<15;cycle++){
            m.scroll=cycle*4.0*Terrain.SECTION_LENGTH;
            near(m.difficultyMultiplier(),Math.pow(1.05,cycle),1e-12,"5 percent compounded");
            near(m.speed(),150*Math.pow(1.05,cycle),1e-9,"progressive speed");
            m.scroll+=3*Terrain.SECTION_LENGTH+Terrain.SECTION_LENGTH-1;
            near(m.speed(),150*Math.pow(1.05,cycle),1e-9,"no early increase within cycle");
        }
        m.startGame();m.scroll=4*Terrain.SECTION_LENGTH-1;m.invulnerable=10;m.update(0.02);
        check(m.level()==2,"cycle boundary reached");near(m.speed(),157.5,1e-9,"speed increases at repeat");
        m.startGame();m.update(0.01);near(m.shipVelocityX,150,1e-8,"scroll contributes to ship world velocity");near(m.shipVelocityY,0,0,"no vertical input");
        m.playerX=ScrambleModel.WIDTH-90;m.right=true;m.update(0.01);near(m.shipVelocityX,150,1e-8,"screen clamp respected");
        m.playerX=160;m.right=false;m.up=true;m.update(0.01);near(m.shipVelocityY,-280,1e-8,"upward velocity measured");
        m.right=true;m.update(0.01);near(m.shipVelocityX,150+280/Math.sqrt(2),1e-8,"diagonal world velocity");near(m.shipVelocityY,-280/Math.sqrt(2),1e-8,"normalised diagonal velocity");
        m.up=m.right=false;m.playerY=m.terrain.floor(m.playerWorldX());m.invulnerable=10;m.update(0.01);near(m.shipVelocityY,0,0,"protection correction is not real velocity");
        // Real turret path, not merely the helper: fire while ship moves right and up.
        m.startGame();m.targets.clear();m.playerX=160;m.playerY=300;m.right=true;m.up=true;
        ScrambleModel.Target turret=new ScrambleModel.Target(ScrambleModel.Kind.TURRET,600,490,0);turret.cooldown=0;m.targets.add(turret);
        // This diagonal speed exceeds bullet speed, but an approaching geometry still has a solution.
        m.update(0.001);ScrambleModel.Projectile shot=m.shots.stream().filter(p->p.enemy).findFirst().orElseThrow(()->new AssertionError("turret did not fire"));
        double mx=turret.x-16,my=turret.y-22-11;
        double[] expected=InterceptAim.velocity(mx,my,m.playerWorldX(),m.playerY,m.shipVelocityX,m.shipVelocityY,220);
        near(shot.vx,expected[0],1e-9,"turret uses predicted X aim");near(shot.vy,expected[1],1e-9,"turret uses predicted Y aim");
        near(shot.x,mx+shot.vx*0.001,1e-9,"turret muzzle X");
        double savedVx=shot.vx,savedVy=shot.vy;m.up=m.right=false;m.left=true;m.down=true;m.update(0.001);
        near(shot.vx,savedVx,0,"not homing X");near(shot.vy,savedVy,0,"not homing Y");
        ScrambleModel.Target base=new ScrambleModel.Target(ScrambleModel.Kind.BASE,650,470,0);
        ScrambleModel.Projectile baseShot=m.enemyShot(base);
        double[] expectedBase=InterceptAim.velocity(base.x-16,base.y-27-11,m.playerWorldX(),m.playerY,m.shipVelocityX,m.shipVelocityY,220);
        near(baseShot.vx,expectedBase[0],1e-9,"base prediction");near(baseShot.y,base.y-38,0,"base barrel muzzle");
        System.out.println("PASS: "+checks+" checks; 5% cycles, intercept roots, expanding-circle intersections, world velocity, turret/base integration and fixed flight.");
    }
}
