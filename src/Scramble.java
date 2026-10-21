/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.Dimension;
import java.awt.event.*;
import java.nio.file.Paths;
import javax.swing.*;

/** Launches the independent Scramble-inspired game. */
public final class Scramble{
    public static void main(String[] args){SwingUtilities.invokeLater(()->{
        try{
            FlightPanel panel=new FlightPanel(Paths.get("scramble-scores.bin"),true,true);
            JFrame frame=new JFrame("Scramble — Charlie's Arcade");frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel);frame.pack();frame.setMinimumSize(new Dimension(720,500));frame.setLocationRelativeTo(null);
            frame.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){panel.lostFocus();}});
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){panel.close();}});
            frame.setVisible(true);panel.requestFocusInWindow();
        }catch(Exception ex){ex.printStackTrace();JOptionPane.showMessageDialog(null,"Could not start Scramble: "+ex.getMessage(),"Scramble",JOptionPane.ERROR_MESSAGE);}
    });}
}
