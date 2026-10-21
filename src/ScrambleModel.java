/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.util.*;
import java.util.function.Consumer;

/** Side-scrolling game model. All entity coordinates except playerX are world coordinates. */
final class ScrambleModel {
    static final int WIDTH=960,HEIGHT=640,PLAYER_Y_MARGIN=16;
    enum State{TITLE,PLAYING,RESPAWN,GAME_OVER,SCORES}
    enum Kind{FUEL,ROCKET,TURRET,DRONE,BASE}
    static final class Target{
        final Kind kind;final double x0,y0,phase;
        double x,y,age,cooldown;boolean launched;
        Target(Kind kind,double x,double y,double phase){this.kind=kind;this.x=x;x0=x;this.y=y;y0=y;this.phase=phase;cooldown=0.9+phase;}
        int points(){switch(kind){case FUEL:return 150;case ROCKET:return launched?100:50;case TURRET:return 100;case DRONE:return 120;case BASE:return 1000;default:return 0;}}
        double radiusX(){return kind==Kind.BASE?29:kind==Kind.FUEL?20:kind==Kind.DRONE?20:13;}
        double radiusY(){return kind==Kind.BASE?27:kind==Kind.FUEL?20:kind==Kind.DRONE?11:22;}
    }
    static final class Projectile{
        double x,y,vx,vy,age;final boolean enemy,bomb;
        Projectile(double x,double y,double vx,double vy,boolean enemy,boolean bomb){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.enemy=enemy;this.bomb=bomb;}
    }
    final Terrain terrain=new Terrain();
    final List<Target> targets=new ArrayList<>();
    final List<Projectile> shots=new ArrayList<>();
    final List<SaturnExplosion> explosions=new ArrayList<>();
    private final Random random;
    private final Consumer<SoundEffects.Effect> sound;
    State state=State.TITLE;
    int lives=3,score;
    double scroll,playerX=160,playerY=300,fuel=100,time,stateTime,invulnerable;
    boolean left,right,up,down,fire,bomb;
    String message="";
    private double shotDelay,bombDelay,nextObject,nextDrone;
    private int nextBonus;
    ScrambleModel(Random random,Consumer<SoundEffects.Effect> sound){this.random=random;this.sound=sound;}
    void resetControls(){left=right=up=down=fire=bomb=false;}
    void startGame(){
        scroll=0;time=0;stateTime=0;lives=3;score=0;fuel=100;playerX=160;playerY=300;
        nextObject=650;nextDrone=1200;shotDelay=bombDelay=0;nextBonus=10000;
        invulnerable=2.4;state=State.PLAYING;message="";resetControls();
        targets.clear();shots.clear();explosions.clear();populate();
    }
    double playerWorldX(){return scroll+playerX;}
    double speed(){return 150+Math.min(70,Terrain.section(scroll)/4*12);}
    int section(){return Terrain.section(scroll);}
    int level(){return section()/4+1;}
    void update(double dt){
        if(dt<=0)return;time+=dt;stateTime+=dt;
        for(Iterator<SaturnExplosion> it=explosions.iterator();it.hasNext();){SaturnExplosion e=it.next();e.update(dt);if(e.getCount()==0)it.remove();}
        if(state==State.RESPAWN){
            if(stateTime>=1.6){playerX=160;playerY=terrain.corridorMiddle(playerWorldX());fuel=100;invulnerable=2.6;state=State.PLAYING;stateTime=0;}
            return;
        }
        if(state!=State.PLAYING)return;
        int oldSection=section();scroll+=speed()*dt;
        if(section()!=oldSection){addScore(500);message="SECTION CLEAR +500";sound.accept(SoundEffects.Effect.LEVEL_UP);}
        invulnerable=Math.max(0,invulnerable-dt);
        double dx=(right?1:0)-(left?1:0),dy=(down?1:0)-(up?1:0),length=Math.hypot(dx,dy);
        if(length>0){playerX+=dx/length*280*dt;playerY+=dy/length*280*dt;}
        playerX=Math.max(36,Math.min(WIDTH-90,playerX));playerY=Math.max(106,Math.min(HEIGHT-40,playerY));
        fuel=Math.max(0,fuel-(2.15+0.12*Math.min(6,level()-1))*dt);
        if(fuel<=0){crash("OUT OF FUEL");return;}
        // Terrain remains solid during respawn protection, but a protected ship is repositioned safely.
        if(terrain.hits(playerWorldX(),playerY,19,10)){
            if(invulnerable>0)playerY=terrain.corridorMiddle(playerWorldX());
            else{crash("TERRAIN COLLISION");return;}
        }
        shotDelay-=dt;bombDelay-=dt;
        if(fire&&shotDelay<=0&&shots.stream().filter(p->!p.enemy&&!p.bomb).count()<4){
            shots.add(new Projectile(playerWorldX()+25,playerY,speed()+540,0,false,false));shotDelay=0.16;sound.accept(SoundEffects.Effect.SHOT);
        }
        if(bomb&&bombDelay<=0&&shots.stream().filter(p->p.bomb).count()<3){
            shots.add(new Projectile(playerWorldX(),playerY+13,speed()+85,75,false,true));bombDelay=0.38;sound.accept(SoundEffects.Effect.BONUS);
        }
        populate();updateTargets(dt);
        if(state!=State.PLAYING)return;
        updateProjectiles(dt);
        if(state!=State.PLAYING)return;
        for(Target t:targets)if(Math.abs(t.x-playerWorldX())<t.radiusX()+17&&Math.abs(t.y-playerY)<t.radiusY()+9){
            if(invulnerable<=0){crash("ENEMY COLLISION");return;}
        }
        targets.removeIf(t->t.x<scroll-120||t.y<60||t.y>HEIGHT+80);
    }
    private void populate(){
        while(nextObject<scroll+WIDTH+650){
            long index=Math.round((nextObject-650)/210);int zone=Terrain.section(nextObject)%4;
            Kind type=index%4==0?Kind.FUEL:zone==3?Kind.TURRET:Kind.ROCKET;
            if(zone==3&&nextObject%Terrain.SECTION_LENGTH>3450)type=Kind.BASE;
            double height=type==Kind.BASE?27:type==Kind.FUEL?20:22;
            targets.add(new Target(type,nextObject,terrain.floor(nextObject)-height,random.nextDouble()*3));
            nextObject+=210;
        }
        while(nextDrone<scroll+WIDTH+200){
            if(Terrain.section(nextDrone)%4!=2){double y=terrain.ceiling(nextDrone)+90+random.nextDouble()*110;
                targets.add(new Target(Kind.DRONE,nextDrone,y,random.nextDouble()*6));}
            nextDrone+=620+random.nextDouble()*380;
        }
    }
    private void updateTargets(double dt){
        List<Projectile> fired=new ArrayList<>();
        for(Target t:targets){
            t.age+=dt;t.cooldown-=dt;double ahead=t.x-playerWorldX();
            if(t.kind==Kind.ROCKET){
                if(!t.launched&&ahead>70&&ahead<450&&t.cooldown<=0){t.launched=true;sound.accept(SoundEffects.Effect.ENEMY_SHOT);}
                if(t.launched)t.y-=(155+level()*12)*dt;
            }else if(t.kind==Kind.DRONE){
                t.x-=(42+level()*4)*dt;t.y=t.y0+Math.sin(t.age*2.2+t.phase)*32;
            }else if((t.kind==Kind.TURRET||t.kind==Kind.BASE)&&ahead>-50&&ahead<WIDTH&&t.cooldown<=0&&shots.size()<60){
                double dx=playerWorldX()-t.x,dy=playerY-t.y,d=Math.max(1,Math.hypot(dx,dy));
                fired.add(new Projectile(t.x,t.y-23,dx/d*220,dy/d*220,true,false));
                t.cooldown=1.8+random.nextDouble();sound.accept(SoundEffects.Effect.ENEMY_SHOT);
            }
        }
        shots.addAll(fired);
    }
    private void updateProjectiles(double dt){
        for(Iterator<Projectile> it=shots.iterator();it.hasNext();){
            Projectile p=it.next();p.age+=dt;if(p.bomb)p.vy+=310*dt;
            p.x+=p.vx*dt;p.y+=p.vy*dt;
            boolean remove=p.x<scroll-100||p.x>scroll+WIDTH+180||p.y<78||p.y>HEIGHT+50||p.age>7;
            if(!remove&&p.enemy&&Math.abs(p.x-playerWorldX())<20&&Math.abs(p.y-playerY)<12){
                remove=true;if(invulnerable<=0){it.remove();crash("HIT BY ENEMY FIRE");return;}
            }
            if(!remove&&!p.enemy){
                for(Iterator<Target> ti=targets.iterator();ti.hasNext();){Target t=ti.next();
                    if(Math.abs(p.x-t.x)<t.radiusX()+5&&Math.abs(p.y-t.y)<t.radiusY()+5){
                        ti.remove();destroy(t);remove=true;break;
                    }
                }
            }
            if(!remove&&terrain.hits(p.x,p.y,2,2)){
                if(p.bomb)explosions.add(new SaturnExplosion(p.x,p.y,random));remove=true;
            }
            if(remove)it.remove();
        }
    }
    void destroy(Target t){
        addScore(t.points());explosions.add(new SaturnExplosion(t.x,t.y,random));sound.accept(SoundEffects.Effect.EXPLOSION);
        if(t.kind==Kind.FUEL){fuel=Math.min(100,fuel+28);message="FUEL +28";sound.accept(SoundEffects.Effect.LEVEL_UP);}
        else if(t.kind==Kind.BASE)message="BASE DESTROYED +1000";
    }
    void addScore(int points){score+=points;while(score>=nextBonus){lives++;nextBonus+=10000;message="EXTRA LIFE";sound.accept(SoundEffects.Effect.LEVEL_UP);}}
    void crash(String reason){
        if(state!=State.PLAYING)return;message=reason;
        explosions.add(new SaturnExplosion(playerWorldX(),playerY,random));lives--;resetControls();shots.clear();
        sound.accept(SoundEffects.Effect.PLAYER_HIT);stateTime=0;
        if(lives<=0){state=State.GAME_OVER;sound.accept(SoundEffects.Effect.GAME_OVER);}
        else{state=State.RESPAWN;targets.removeIf(t->Math.abs(t.x-playerWorldX())<300);}
    }
}
