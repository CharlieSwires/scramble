/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

/** Single-window Swing UI: gameplay, score entry and input all live on the EDT. */
final class FlightPanel extends JPanel {
    private static final long serialVersionUID=1L;
    private static final Color INK=new Color(6,12,25),CYAN=new Color(85,227,233),GOLD=new Color(255,207,100),MUTED=new Color(138,162,181);
    final ScrambleModel game;
    final HighScores scores;
    final JTextField scoreName=new JTextField("PLAYER");
    final JButton saveScore=new JButton("SAVE SCORE");
    private final JPanel nameForm=new JPanel(new BorderLayout(0,10));
    private final Set<Integer> keys=new HashSet<>();
    private final Timer timer;
    private final double[][] stars=new double[90][3];
    private SoundEffects sound;
    private boolean paused,muted,entryActive,ignoreEnter,savingAllowed=true;
    private double clock;
    private long previous;
    private String notice="";
    FlightPanel(Path scorePath,boolean audio,boolean animate)throws IOException{
        setPreferredSize(new Dimension(ScrambleModel.WIDTH,ScrambleModel.HEIGHT));
        setBackground(INK);setFocusable(true);setDoubleBuffered(true);setLayout(null);
        scores=new HighScores(scorePath);
        try{scores.load();}catch(IOException ex){savingAllowed=false;notice="Could not read scores; original file retained.";}
        if(audio)sound=new SoundEffects();
        game=new ScrambleModel(new Random(),effect->{if(sound!=null)sound.play(effect);});
        Random random=new Random(1971);for(double[] star:stars){star[0]=random.nextDouble()*960;star[1]=95+random.nextDouble()*475;star[2]=1+random.nextDouble()*2;}
        JLabel label=new JLabel("PILOT NAME — UP TO 20 ASCII CHARACTERS");label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));nameForm.setBackground(new Color(10,25,42));
        nameForm.setBorder(BorderFactory.createEmptyBorder(14,18,14,18));nameForm.add(label,BorderLayout.NORTH);
        nameForm.add(scoreName,BorderLayout.CENTER);nameForm.add(saveScore,BorderLayout.SOUTH);add(nameForm);nameForm.setVisible(false);
        scoreName.addActionListener(e->{ignoreEnter=true;submitScore();});saveScore.addActionListener(e->submitScore());
        for(String key:new String[]{"A","D","W","S","LEFT","RIGHT","UP","DOWN","SPACE","B","X"})bindControl(key);
        bind("released ENTER",()->{if(ignoreEnter){ignoreEnter=false;return;}if(game.state==ScrambleModel.State.TITLE||game.state==ScrambleModel.State.SCORES)start();},false);
        bind("released H",()->{if(game.state==ScrambleModel.State.TITLE)game.state=ScrambleModel.State.SCORES;else if(game.state==ScrambleModel.State.SCORES)game.state=ScrambleModel.State.TITLE;},false);
        bind("released P",this::togglePause,false);
        bind("released ESCAPE",()->{if(game.state==ScrambleModel.State.SCORES)game.state=ScrambleModel.State.TITLE;else togglePause();},false);
        bind("released M",()->{muted=!muted;if(sound!=null)sound.toggleMute();},false);
        bind("released T",()->{
            if(sound==null||sound.status().equals("UNAVAILABLE")||sound.status().equals("CLOSED")){if(sound!=null)sound.close();sound=new SoundEffects();muted=false;}
            if(muted){muted=false;sound.toggleMute();}sound.play(SoundEffects.Effect.LEVEL_UP);
        },false);
        previous=System.nanoTime();timer=new Timer(16,e->tick());timer.setCoalesce(true);if(animate)timer.start();
    }
    private void bindControl(String key){int code=KeyStroke.getKeyStroke("pressed "+key).getKeyCode();
        bind("pressed "+key,()->{keys.add(code);applyControls();},true);bind("released "+key,()->{keys.remove(code);applyControls();},true);}
    private void bind(String key,Runnable command,boolean gameplay){
        AbstractAction action=new AbstractAction(){
            @Override public boolean isEnabled(){return !entryActive&&(!gameplay||((game.state==ScrambleModel.State.PLAYING||game.state==ScrambleModel.State.RESPAWN)&&(!paused||key.startsWith("released "))));}
            @Override public void actionPerformed(ActionEvent e){if(isEnabled())command.run();}
        };
        KeyStroke base=KeyStroke.getKeyStroke(key);for(int modifier:new int[]{0,InputEvent.SHIFT_DOWN_MASK})
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(base.getKeyCode(),modifier,base.isOnKeyRelease()),key);
        getActionMap().put(key,action);
    }
    private boolean held(int a,int b){return keys.contains(a)||keys.contains(b);}
    private void applyControls(){game.left=held(KeyEvent.VK_A,KeyEvent.VK_LEFT);game.right=held(KeyEvent.VK_D,KeyEvent.VK_RIGHT);game.up=held(KeyEvent.VK_W,KeyEvent.VK_UP);game.down=held(KeyEvent.VK_S,KeyEvent.VK_DOWN);game.fire=keys.contains(KeyEvent.VK_SPACE);game.bomb=held(KeyEvent.VK_B,KeyEvent.VK_X);}
    private void clearControls(){keys.clear();game.resetControls();}
    void start(){entryActive=false;ignoreEnter=false;paused=false;nameForm.setVisible(false);clearControls();game.startGame();previous=System.nanoTime();restoreFocus();}
    private void restoreFocus(){SwingUtilities.invokeLater(()->requestFocusInWindow());}
    private void togglePause(){if(game.state==ScrambleModel.State.PLAYING||game.state==ScrambleModel.State.RESPAWN){paused=!paused;clearControls();}}
    void lostFocus(){clearControls();if(game.state==ScrambleModel.State.PLAYING||game.state==ScrambleModel.State.RESPAWN)paused=true;}
    void close(){timer.stop();if(sound!=null)sound.close();}
    void advance(double dt){if(paused||entryActive)return;applyControls();game.update(dt);}
    private void tick(){long now=System.nanoTime();double dt=Math.min(0.04,(now-previous)/1e9);previous=now;clock+=dt;
        advance(dt);if(game.state==ScrambleModel.State.GAME_OVER&&game.stateTime>1.6&&!entryActive)finishGame();repaint();}
    void finishGame(){if(entryActive)return;clearControls();paused=false;
        if(scores.qualifies(game.score)){entryActive=true;nameForm.setVisible(true);revalidate();SwingUtilities.invokeLater(()->{scoreName.requestFocusInWindow();scoreName.selectAll();});}
        else showScores();}
    void submitScore(){if(!entryActive)return;scores.add(game.score,scoreName.getText());if(savingAllowed)try{scores.save();}catch(IOException ex){notice="Could not save scores: "+ex.getMessage();}showScores();}
    private void showScores(){entryActive=false;paused=false;nameForm.setVisible(false);clearControls();game.state=ScrambleModel.State.SCORES;previous=System.nanoTime();revalidate();repaint();restoreFocus();}
    @Override public void doLayout(){double scale=Math.min(getWidth()/960.0,getHeight()/640.0);int ox=(int)((getWidth()-960*scale)/2),oy=(int)((getHeight()-640*scale)/2);nameForm.setBounds(ox+(int)(240*scale),oy+(int)(400*scale),(int)(480*scale),(int)(145*scale));nameForm.doLayout();}
    @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        try{double scale=Math.min(getWidth()/960.0,getHeight()/640.0);g.translate((getWidth()-960*scale)/2,(getHeight()-640*scale)/2);g.scale(scale,scale);g.clipRect(0,0,960,640);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0,80,INK,0,640,new Color(13,29,44)));g.fillRect(0,0,960,640);
            if(game.state==ScrambleModel.State.TITLE)drawTitle(g);else if(game.state==ScrambleModel.State.SCORES)drawScores(g);else drawFlight(g);
            if(paused)overlay(g,"PAUSED","P / ESC TO RESUME");
            if(sound!=null&&sound.status().equals("UNAVAILABLE"))text(g,"NO AUDIO — T TO RETRY / sound-diagnostics.txt",11,GOLD,24,620);
            if(!notice.isEmpty())text(g,notice,11,GOLD,24,635);
        }finally{g.dispose();}}
    private void starfield(Graphics2D g,double scroll){for(double[] star:stars){double x=(star[0]-scroll*0.18*star[2])%960;if(x<0)x+=960;g.setColor(star[2]>2?new Color(91,133,154):new Color(32,66,88));g.fillRect((int)x,(int)star[1],star[2]>2?2:1,1);}}
    private void drawTitle(Graphics2D g){starfield(g,clock*35);
        text(g,"CHARLIE'S ARCADE  /  FLIGHT 02",13,CYAN,56,65);text(g,"SCRAMBLE",80,Color.WHITE,49,154);
        text(g,"FLY LOW. STRIKE FAST. WATCH YOUR FUEL.",16,GOLD,56,190);
        g.setColor(new Color(38,66,84));g.drawLine(56,215,904,215);
        Graphics2D scene=(Graphics2D)g.create();scene.clipRect(0,235,960,202);
        drawTerrain(scene,1200,0.31,246);scene.dispose();
        ship(g,220,327,1.8,clock,false);
        rocket(g,617,game.terrain.floor(1817)*0.31+246-22,false);
        tank(g,726,game.terrain.floor(1926)*0.31+246-20);drone(g,610,300,clock);
        text(g,"HILLS → MISSILES → CAVERNS → FORTRESSES",15,Color.WHITE,56,453);
        text(g,"BOMB FUEL TANKS TO KEEP YOUR MISSION ALIVE",13,MUTED,56,481);
        box(g,56,510,848,57,new Color(13,47,59));center(g,"ENTER  /  START MISSION",23,CYAN,548);
        center(g,"ARROWS / WASD MOVE    SPACE FIRE    B / X DROP BOMBS",13,Color.WHITE,596);
        center(g,"P PAUSE    M MUTE    T SOUND TEST    H SCORES",11,MUTED,621);
    }
    private void drawFlight(Graphics2D g){
        starfield(g,game.scroll);drawTerrain(g,game.scroll,1,0);
        for(ScrambleModel.Target t:game.targets){double x=t.x-game.scroll;if(x<-70||x>1030)continue;
            switch(t.kind){case FUEL:tank(g,x,t.y);break;case ROCKET:rocket(g,x,t.y,t.launched);break;case TURRET:turret(g,x,t.y,false);break;case BASE:turret(g,x,t.y,true);break;case DRONE:drone(g,x,t.y,game.time+t.phase);break;default:break;}}
        for(ScrambleModel.Projectile p:game.shots){int x=(int)(p.x-game.scroll),y=(int)p.y;
            g.setColor(p.enemy?new Color(255,115,120):p.bomb?GOLD:CYAN);
            if(p.bomb){g.fillOval(x-4,y-5,8,10);g.drawLine(x-5,y-6,x+5,y-6);}else if(p.enemy)g.fillOval(x-3,y-3,6,6);else g.fillRoundRect(x-7,y-2,17,4,3,3);}
        Graphics2D world=(Graphics2D)g.create();world.translate(-game.scroll,0);for(SaturnExplosion e:game.explosions)e.draw(world);world.dispose();
        if(game.state==ScrambleModel.State.PLAYING&&(game.invulnerable<=0||(int)(game.invulnerable*10)%2==0))ship(g,game.playerX,game.playerY,1,game.time,true);
        hud(g);
        if(game.state==ScrambleModel.State.RESPAWN)overlay(g,"SHIP LOST",game.message+"  /  RELAUNCHING");
        if(game.state==ScrambleModel.State.GAME_OVER)overlay(g,entryActive?"TOP TEN!":"MISSION OVER","FINAL SCORE  "+game.score);
    }
    private void drawTerrain(Graphics2D g,double scroll,double scale,double yOffset){
        Graphics2D t=(Graphics2D)g.create();t.translate(0,yOffset);t.scale(1,scale);
        Path2D ground=new Path2D.Double(),roof=new Path2D.Double();ground.moveTo(0,640);roof.moveTo(0,80);
        for(int x=0;x<=968;x+=8){ground.lineTo(x,game.terrain.floor(scroll+x));roof.lineTo(x,game.terrain.ceiling(scroll+x));}
        ground.lineTo(968,640);ground.closePath();roof.lineTo(968,80);roof.closePath();
        t.setColor(new Color(19,52,63));t.fill(ground);t.setColor(new Color(20,39,54));t.fill(roof);
        t.setStroke(new BasicStroke(2));t.setColor(new Color(71,188,179));
        for(int x=0;x<960;x+=8)t.draw(new Line2D.Double(x,game.terrain.floor(scroll+x),x+8,game.terrain.floor(scroll+x+8)));
        t.setColor(new Color(80,123,160));for(int x=0;x<960;x+=8)t.draw(new Line2D.Double(x,game.terrain.ceiling(scroll+x),x+8,game.terrain.ceiling(scroll+x+8)));
        t.setColor(new Color(25,72,78));for(int x=0;x<960;x+=36){double y=game.terrain.floor(scroll+x);t.draw(new Line2D.Double(x,y+12,x-40,y+90));}
        t.dispose();
    }
    private void hud(Graphics2D g){box(g,12,12,936,65,new Color(7,18,32));
        text(g,"SCORE",10,MUTED,28,32);text(g,String.format("%06d",game.score),22,Color.WHITE,28,59);
        text(g,"BEST",10,MUTED,182,32);text(g,String.format("%06d",Math.max(scores.best(),game.score)),22,GOLD,182,59);
        text(g,"FUEL",11,MUTED,358,32);box(g,358,42,200,14,new Color(39,51,63));
        Color fuelColor=game.fuel<25?new Color(255,112,110):CYAN;box(g,358,42,(int)(game.fuel*2),14,fuelColor);
        text(g,String.format("%03d%%",(int)game.fuel),12,fuelColor,569,54);
        text(g,"LIVES "+game.lives,12,Color.WHITE,655,33);text(g,"LEVEL "+game.level(),12,CYAN,655,57);
        String audio=muted?"MUTED":sound==null?"AUDIO OFF":sound.status();text(g,audio,10,MUTED,828,33);text(g,"P / PAUSE",10,MUTED,828,57);
        text(g,String.format("%02d  %s",game.section()+1,Terrain.name(game.scroll)),12,CYAN,25,102);
        double progress=(game.scroll%Terrain.SECTION_LENGTH)/Terrain.SECTION_LENGTH;box(g,704,91,225,4,new Color(27,48,63));box(g,704,91,(int)(225*progress),4,CYAN);
        if(game.fuel<25)center(g,"LOW FUEL — DESTROY A FUEL TANK",13,GOLD,124);
    }
    private void drawScores(Graphics2D g){starfield(g,clock*25);text(g,"FLIGHT RECORDS",12,CYAN,64,62);text(g,"HIGH SCORES",45,Color.WHITE,60,116);
        text(g,"RANK",11,MUTED,67,164);text(g,"PILOT",11,MUTED,166,164);text(g,"SCORE",11,MUTED,750,164);
        for(int i=0;i<10;i++){int y=200+i*30;if(i%2==0)box(g,55,y-21,850,28,new Color(11,27,43));Color c=i==0?GOLD:Color.WHITE;text(g,String.format("%02d",i+1),14,c,70,y);
            if(i<scores.entries().size()){HighScores.Entry e=scores.entries().get(i);text(g,e.name,14,c,166,y);text(g,String.format("%06d",e.score),14,c,750,y);}else text(g,"---",14,MUTED,166,y);}
        box(g,56,532,848,57,new Color(13,47,59));center(g,"ENTER  /  NEW MISSION",22,CYAN,569);center(g,"H / ESC  BACK TO TITLE",11,MUTED,618);
    }
    private void overlay(Graphics2D g,String heading,String sub){box(g,176,245,608,125,new Color(4,12,24,235));center(g,heading,38,Color.WHITE,300);center(g,sub,12,GOLD,337);}
    private static void ship(Graphics2D g,double x,double y,double scale,double time,boolean flame){Graphics2D s=(Graphics2D)g.create();s.translate(x,y);s.scale(scale,scale);
        if(flame){s.setColor(GOLD);s.fillPolygon(new int[]{-23,-42-(int)(Math.sin(time*30)*5),-23},new int[]{-5,0,5},3);}
        s.setColor(CYAN);s.fillPolygon(new int[]{27,6,-20,-12,-25,-8,5},new int[]{0,-8,-9,0,13,10,5},7);
        s.setColor(Color.WHITE);s.fillPolygon(new int[]{27,4,-11,2},new int[]{0,-3,0,4},4);s.setColor(new Color(11,56,80));s.fillRect(1,-6,9,4);s.dispose();}
    private static void tank(Graphics2D g,double x,double y){g.setColor(new Color(77,63,31));g.fillRoundRect((int)x-20,(int)y-20,40,40,4,4);g.setColor(GOLD);g.drawRoundRect((int)x-20,(int)y-20,40,40,4,4);text(g,"FUEL",10,GOLD,(int)x-14,(int)y+4);g.fillRect((int)x-11,(int)y-25,22,5);}
    private static void rocket(Graphics2D g,double x,double y,boolean launched){g.setColor(new Color(255,120,125));g.fillPolygon(new int[]{(int)x,(int)x-8,(int)x-8,(int)x-14,(int)x+14,(int)x+8,(int)x+8},new int[]{(int)y-23,(int)y-10,(int)y+8,(int)y+21,(int)y+21,(int)y+8,(int)y-10},7);g.setColor(Color.WHITE);g.fillRect((int)x-3,(int)y-9,6,18);if(launched){g.setColor(GOLD);g.fillPolygon(new int[]{(int)x-4,(int)x,(int)x+4},new int[]{(int)y+20,(int)y+36,(int)y+20},3);}}
    private static void drone(Graphics2D g,double x,double y,double time){g.setColor(new Color(182,134,255));int wing=(int)(Math.sin(time*12)*5);g.fillPolygon(new int[]{(int)x-23,(int)x-9,(int)x+11,(int)x+22,(int)x+6,(int)x-10},new int[]{(int)y+wing,(int)y-11,(int)y-8,(int)y,(int)y+9,(int)y+8},6);g.setColor(Color.WHITE);g.fillRect((int)x-10,(int)y-2,8,4);}
    private static void turret(Graphics2D g,double x,double y,boolean base){int w=base?58:26,h=base?54:44;g.setColor(new Color(69,93,111));g.fillRect((int)x-w/2,(int)y-h/2,w,h);g.setColor(new Color(161,191,211));g.drawRect((int)x-w/2,(int)y-h/2,w,h);g.setStroke(new BasicStroke(base?7:5));g.drawLine((int)x,(int)y-h/2+2,(int)x-16,(int)y-h/2-11);g.setStroke(new BasicStroke(1));if(base)text(g,"BASE",11,Color.WHITE,(int)x-14,(int)y+10);}
    private static void box(Graphics2D g,int x,int y,int w,int h,Color c){g.setColor(c);g.fillRoundRect(x,y,w,h,9,9);}
    private static void text(Graphics2D g,String str,int size,Color c,int x,int y){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,size));g.setColor(c);g.drawString(str,x,y);}
    private static void center(Graphics2D g,String str,int size,Color c,int y){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,size));text(g,str,size,c,(960-g.getFontMetrics().stringWidth(str))/2,y);}
}
