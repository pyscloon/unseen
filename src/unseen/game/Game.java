package unseen.game;

import javax.swing.JFrame;
import unseen.ui.GamePanel;

public class Game {

    public static void main(String[] args) {

        JFrame window = new JFrame("UNSEEN");

        GamePanel panel = new GamePanel();

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        panel.startGame();
    }
}
