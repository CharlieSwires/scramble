import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Dependency-free deterministic model, persistence, input and rendering checks. */
public final class ScrambleTests{
    static int count;
    static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
    static ScrambleModel model(){ScrambleModel m=new ScrambleModel(new Random(42),e->{});m.startGame();return m;}
    static void key(FlightPanel p,String stroke){p.getActionMap().get(stroke).actionPerformed(null);}
    static void render(FlightPanel panel,String filename)throws Exception{
        panel.doLayout();BufferedImage image=new BufferedImage(960,640,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
        ImageIO.write(image,"png",Paths.get("build",filename).toFile());
    }
    public static void main(String[] args)throws Exception{
        Terrain terrain=new Terrain();
        for(int x=0;x<100000;x+=13){check(terrain.floor(x)-terrain.ceiling(x)>=185,"navigable terrain gap");check(terrain.ceiling(x)>=88-1e-9&&terrain.floor(x)<=585,"terrain limits");}
        for(int x=80;x<20000;x+=80){check(Math.abs(terrain.floor(x-0.001)-terrain.floor(x+0.001))<0.01,"continuous floor");check(Math.abs(terrain.ceiling(x-0.001)-terrain.ceiling(x+0.001))<0.01,"continuous ceiling");}
        check(!terrain.hits(200,terrain.corridorMiddle(200),19,10),"spawn corridor");
        check(terrain.hits(200,terrain.floor(200),19,10),"floor collision");check(terrain.hits(8200,terrain.ceiling(8200),19,10),"cave collision");
        ScrambleModel m=model();check(m.lives==3&&m.score==0&&m.fuel==100,"new mission");
        m.fire=m.bomb=true;m.update(0.02);check(m.shots.size()==2,"forward shot and bomb");
        ScrambleModel.Projectile bullet=m.shots.stream().filter(p->!p.bomb).findFirst().get();
        ScrambleModel.Projectile bomb=m.shots.stream().filter(p->p.bomb).findFirst().get();
        double y=bullet.y,vy=bomb.vy;m.update(0.02);check(bullet.y==y&&bullet.vx>m.speed(),"forward horizontal shot");check(bomb.vy>vy,"bomb gravity");
        m=model();m.fuel=40;m.destroy(new ScrambleModel.Target(ScrambleModel.Kind.FUEL,100,400,0));check(m.fuel==68&&m.score==150,"fuel refill on tank destruction");
        m.destroy(new ScrambleModel.Target(ScrambleModel.Kind.FUEL,100,400,0));m.destroy(new ScrambleModel.Target(ScrambleModel.Kind.FUEL,100,400,0));check(m.fuel==100,"fuel capped");
        m=model();m.invulnerable=0;m.playerY=m.terrain.floor(m.playerWorldX());m.update(0.01);check(m.state==ScrambleModel.State.RESPAWN&&m.lives==2,"terrain crash");
        m.update(1.7);check(m.state==ScrambleModel.State.PLAYING&&m.invulnerable>0&&m.fuel==100,"safe respawn");
        check(!m.terrain.hits(m.playerWorldX(),m.playerY,19,10),"respawn clear of terrain");
        m.fuel=0;m.update(0.01);check(m.lives==1&&m.state==ScrambleModel.State.RESPAWN,"fuel exhaustion");
        m.update(1.7);m.crash("TEST");check(m.state==ScrambleModel.State.GAME_OVER&&m.lives==0,"game over");
        m.startGame();check(m.score==0&&m.lives==3&&m.scroll==0,"new mission resets model");
        m=model();m.scroll=3999;m.invulnerable=10;int points=m.score;m.update(0.02);check(m.section()==1&&m.score==points+500,"section completion bonus");
        m.scroll=16001;check(m.level()==2&&m.speed()>150,"loop increases difficulty");
        m=model();m.targets.clear();double tx=m.playerWorldX()+100,ty=m.playerY;
        m.targets.add(new ScrambleModel.Target(ScrambleModel.Kind.DRONE,tx,ty,0));
        m.shots.add(new ScrambleModel.Projectile(tx,ty,0,0,false,false));m.update(0.001);check(m.score==120,"projectile destroys flying enemy");
        m=model();m.targets.clear();m.fuel=40;
        double fuelX=m.playerWorldX()+100,fuelY=m.terrain.floor(fuelX)-20;
        m.targets.add(new ScrambleModel.Target(ScrambleModel.Kind.FUEL,fuelX,fuelY,0));
        m.shots.add(new ScrambleModel.Projectile(fuelX,fuelY-30,0,100,false,true));m.update(0.12);
        check(m.score==150&&m.fuel>60,"actual bomb hit refuels the ship");
        m=model();m.targets.clear();m.invulnerable=0;
        m.shots.add(new ScrambleModel.Projectile(m.playerWorldX(),m.playerY,150,0,true,false));m.update(0.001);
        check(m.state==ScrambleModel.State.RESPAWN,"enemy projectile collision");
        m=model();m.addScore(10000);check(m.lives==4,"extra life");
        // Autopilot the safe corridor for several sections, stressing generation, weapons and pruning.
        m=model();for(int i=0;i<45000;i++){
            m.invulnerable=1;m.fuel=100;m.playerY=m.terrain.corridorMiddle(m.playerWorldX()+m.speed()/120);m.fire=true;m.bomb=true;m.update(1.0/120);
            check(m.state==ScrambleModel.State.PLAYING,"sustained simulation alive");
            check(m.targets.size()<40&&m.shots.size()<80,"entity lists bounded");
        }
        check(m.section()>=12,"several section cycles completed");
        Path dir=Files.createTempDirectory("scramble-tests-");Path path=dir.resolve("scores.bin");HighScores hs=new HighScores(path);hs.load();
        for(int i=1;i<=10;i++)hs.add(i*100,"Pilot"+i);hs.add(550,"Middle");hs.save();
        HighScores loaded=new HighScores(path);loaded.load();check(loaded.entries().size()==10&&loaded.entries().get(5).score==550,"ranked persistence");
        FlightPanel[] holder=new FlightPanel[1];
        SwingUtilities.invokeAndWait(()->{try{
            FlightPanel p=new FlightPanel(dir.resolve("ui.bin"),false,false);holder[0]=p;p.setSize(960,640);render(p,"title.png");p.start();render(p,"flight.png");
            p.game.scroll=8350;p.game.playerY=p.game.terrain.corridorMiddle(p.game.playerWorldX());render(p,"cave.png");
            key(p,"pressed RIGHT");double x=p.game.playerX;p.advance(0.02);check(p.game.playerX>x,"right input");key(p,"released RIGHT");
            key(p,"pressed UP");yHolder[0]=p.game.playerY;p.advance(0.02);check(p.game.playerY<yHolder[0],"up input");key(p,"released UP");
            key(p,"pressed SPACE");key(p,"pressed B");p.advance(0.02);check(p.game.fire&&p.game.bomb,"weapons input");
            p.game.scroll=11999;p.game.invulnerable=3;p.advance(0.02);check(p.game.section()==3&&p.game.fire&&p.game.bomb,"held controls across section change");
            p.lostFocus();check(!p.game.fire&&!p.game.bomb,"focus loss clears input");key(p,"released P");
            p.game.score=900;p.game.state=ScrambleModel.State.GAME_OVER;p.finishGame();p.scoreName.setText("Charlie");render(p,"name-entry.png");p.scoreName.postActionEvent();
            check(p.game.state==ScrambleModel.State.SCORES,"form submission");p.submitScore();check(p.scores.entries().size()==1,"submit once");render(p,"scores.png");
            key(p,"released ENTER");check(p.game.state==ScrambleModel.State.SCORES,"submission release consumed");key(p,"released ENTER");check(p.game.state==ScrambleModel.State.PLAYING,"restart after scores");
        }catch(Exception ex){throw new RuntimeException(ex);}});
        SwingUtilities.invokeAndWait(()->{});
        SwingUtilities.invokeAndWait(()->{FlightPanel p=holder[0];key(p,"pressed D");key(p,"pressed SPACE");p.advance(0.01);check(p.game.right&&p.game.fire,"controls after focus restoration");p.close();});
        Files.deleteIfExists(path);Files.deleteIfExists(dir.resolve("ui.bin"));Files.delete(dir);
        System.out.println("PASS: "+count+" checks; terrain, collisions, fuel, bombs, enemies, sections, persistence, controls and renders.");
    }
    static final double[] yHolder=new double[1];
}
