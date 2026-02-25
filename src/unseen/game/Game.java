package unseen.game;

import javax.swing.JFrame;
import unseen.ui.GamePanel;
import unseen.utils.Constants;

public class Game {

    public static void main(String[] args) {

        JFrame window = new JFrame("Stealth Dungeon");

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
