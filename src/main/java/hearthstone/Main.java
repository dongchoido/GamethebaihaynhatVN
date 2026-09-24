package hearthstone;

import hearthstone.persistence.FileGameRepository;
import hearthstone.ui.LobbyFrame;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            LobbyFrame lobby = new LobbyFrame(new FileGameRepository());
            lobby.setVisible(true);
        });
    }

    private static void configureLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Giao diện mặc định của hệ điều hành vẫn dùng được.
        }

        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
    }
}
