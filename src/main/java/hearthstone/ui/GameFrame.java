package hearthstone.ui;

import hearthstone.engine.GameEngine;
import hearthstone.engine.GameRuleException;
import hearthstone.model.card.Card;
import hearthstone.model.card.MinionCard;
import hearthstone.model.game.GameState;
import hearthstone.model.game.GameStatus;
import hearthstone.model.game.Player;
import hearthstone.persistence.FileGameRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GameFrame extends JFrame {

    private final GameState game;
    private final FileGameRepository repository;
    private final GameEngine engine = new GameEngine();

    private final JLabel turnLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JLabel manaLabel = new JLabel();
    private final JPanel manaCrystals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
    private final JButton opponentHeroButton = new JButton();
    private final JLabel currentHeroLabel = new JLabel();
    private final JPanel opponentBoard = createCardRow();
    private final JPanel currentBoard = createCardRow();
    private final JPanel handPanel = createCardRow();
    private final JButton endTurnButton = new JButton("KẾT THÚC LƯỢT");
    private final JButton mainMenuButton = new JButton("VỀ MÀN HÌNH CHÍNH");

    private String selectedAttackerId;
    private boolean matchSaved;
    private boolean resultShown;
    private Timer messageTimer;

    public GameFrame(GameState game, FileGameRepository repository) {
        this.game = game;
        this.repository = repository;

        setTitle("Hearthstone Swing - Trận đấu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 720));
        setSize(1240, 850);
        setLocationRelativeTo(null);
        setContentPane(createContent());
        refreshGame();
    }

    private JPanel createContent() {
        JPanel root = new GradientPanel("design/ArenaWood.jpg", 0.48f);
        root.setLayout(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 18, 14, 18));
        root.add(createStatusBar(), BorderLayout.NORTH);
        root.add(createBattlefield(), BorderLayout.CENTER);
        return root;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setBackground(AppTheme.PANEL);
        bar.setBorder(AppTheme.roundedLine(new Color(83, 70, 53), 1));

        turnLabel.setForeground(AppTheme.GOLD);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bar.add(turnLabel, BorderLayout.WEST);

        messageLabel.setForeground(AppTheme.TEXT);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 14f));
        bar.add(messageLabel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(AppTheme.soundToggle());

        AppTheme.styleSecondaryButton(mainMenuButton);
        mainMenuButton.addActionListener(event -> returnToMainMenu());
        mainMenuButton.setVisible(false);
        actionPanel.add(mainMenuButton);

        AppTheme.stylePrimaryButton(endTurnButton);
        endTurnButton.addActionListener(event -> endTurn());
        actionPanel.add(endTurnButton);

        bar.add(actionPanel, BorderLayout.EAST);
        return bar;
    }

    private JPanel createBattlefield() {
        JPanel field = new JPanel(new BorderLayout(0, 12));
        field.setOpaque(false);
        JPanel boards = new JPanel(new GridLayout(2, 1, 0, 12));
        boards.setOpaque(false);
        styleHeroButton(opponentHeroButton, true);
        opponentHeroButton.addActionListener(event -> attackTarget(GameEngine.HERO_TARGET));
        opponentHeroButton.setToolTipText("Chọn Minion của bạn, rồi bấm vào đây để tấn công Hero");
        boards.add(createPlayerZone("ĐỐI PHƯƠNG", opponentHeroButton, opponentBoard));
        currentHeroLabel.setOpaque(true);
        currentHeroLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentHeroLabel.setPreferredSize(new Dimension(250, 108));
        currentHeroLabel.setBackground(new Color(23, 41, 54));
        currentHeroLabel.setForeground(AppTheme.TEXT);
        currentHeroLabel.setBorder(AppTheme.roundedLine(AppTheme.GOLD_DARK, 2));
        boards.add(createPlayerZone("HERO CỦA BẠN", currentHeroLabel, currentBoard));
        field.add(boards, BorderLayout.CENTER);

        JPanel hand = new JPanel(new BorderLayout(0, 8));
        hand.setOpaque(false);
        JPanel handHeader = new JPanel(new BorderLayout());
        handHeader.setOpaque(false);
        JLabel hint = new JLabel("BÀI TRÊN TAY  ·  Bấm bài để chơi, chọn Minion để tấn công");
        hint.setForeground(AppTheme.TEXT);
        hint.setFont(hint.getFont().deriveFont(Font.BOLD, 12f));
        handHeader.add(hint, BorderLayout.WEST);
        JPanel mana = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        mana.setOpaque(false);
        manaCrystals.setOpaque(false);
        manaLabel.setFont(manaLabel.getFont().deriveFont(Font.BOLD, 14f));
        manaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        manaLabel.setForeground(Color.WHITE);
        mana.add(manaCrystals);
        mana.add(manaLabel);
        handHeader.add(mana, BorderLayout.EAST);
        hand.add(handHeader, BorderLayout.NORTH);
        JScrollPane handScroll = createHorizontalScroll(handPanel);
        handScroll.setPreferredSize(new Dimension(900, 220));
        hand.add(handScroll, BorderLayout.CENTER);
        field.add(hand, BorderLayout.SOUTH);
        return field;
    }

    private JPanel createPlayerZone(String title, JComponent hero, JPanel board) {
        JPanel zone = new JPanel(new BorderLayout(14, 0));
        zone.setOpaque(false);
        JPanel heroPanel = new JPanel(new GridBagLayout());
        heroPanel.setOpaque(false);
        heroPanel.setPreferredSize(new Dimension(250, 130));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 8, 0);
        JLabel caption = new JLabel(title);
        caption.setForeground(AppTheme.GOLD);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 12f));
        heroPanel.add(caption, constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        heroPanel.add(hero, constraints);
        zone.add(heroPanel, BorderLayout.WEST);
        zone.add(createHorizontalScroll(board), BorderLayout.CENTER);
        return zone;
    }

    private JPanel createCardRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        panel.setOpaque(false);
        return panel;
    }

    private JScrollPane createHorizontalScroll(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(112, 132, 150), 1, true));
        scroll.setPreferredSize(new Dimension(900, 182));
        scroll.getHorizontalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private void styleHeroButton(JButton button, boolean opponent) {
        button.setPreferredSize(new Dimension(250, 108));
        button.setBackground(opponent ? new Color(92, 42, 40) : new Color(55, 47, 41));
        button.setForeground(AppTheme.TEXT);
        button.setFocusPainted(false);
        button.setBorder(AppTheme.roundedLine(opponent ? AppTheme.RED : AppTheme.GOLD_DARK, 2));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void refreshGame() {
        Player current = game.getCurrentPlayer();
        Player opponent = game.getOpponent();

        turnLabel.setText("LƯỢT " + game.getTurnNumber());
        messageLabel.setText("Đến lượt " + current.getName());
        messageLabel.setForeground(AppTheme.TEXT);
        manaLabel.setText(current.getCurrentMana() + "/" + current.getTotalMana() + " MANA");
        manaCrystals.removeAll();
        for (int i = 0; i < current.getTotalMana(); i++) {
            boolean available = i < current.getCurrentMana();
            JLabel crystal = new JLabel(GameAssets.icon(available
                    ? "design/manacrystal.png" : "design/manacrystal_dark.png", 20, 24));
            crystal.setToolTipText(available ? "Mana có thể dùng" : "Mana đã dùng");
            manaCrystals.add(crystal);
        }

        opponentHeroButton.setText(heroHtml(opponent));
        opponentHeroButton.setIcon(GameAssets.heroPortrait(opponent.getDeckCode(), 64, 76));
        opponentHeroButton.setToolTipText(opponent.hasDefendingMinions()
                ? "Hero được bảo vệ: hãy hạ hết Minion phòng thủ trước"
                : "Chọn Minion của bạn, rồi bấm vào đây để tấn công Hero");
        currentHeroLabel.setText(heroHtml(current));
        currentHeroLabel.setIcon(GameAssets.heroPortrait(current.getDeckCode(), 64, 76));
        renderBoard(opponentBoard, opponent, false);
        renderBoard(currentBoard, current, true);
        renderHand(current);

        boolean inProgress = game.getStatus() == GameStatus.IN_PROGRESS;
        endTurnButton.setEnabled(inProgress);
        endTurnButton.setVisible(inProgress);
        mainMenuButton.setVisible(!inProgress);
        revalidate();
        repaint();
        checkFinished();
    }

    private String heroHtml(Player player) {
        return "<html><div style='width:100px;text-align:center'><b>"
                + AppTheme.escapeHtml(player.getName())
                + "</b><br><span style='color:#ff9a86'>♥ " + player.getHero().getHealth()
                + "/30</span><br><small>" + AppTheme.escapeHtml(player.getDeckName())
                + " · " + player.getDeckSize() + " lá</small></div></html>";
    }

    private void renderBoard(JPanel panel, Player owner, boolean ownBoard) {
        panel.removeAll();
        if (owner.getBoard().isEmpty()) {
            JLabel empty = new JLabel("Chưa có Minion");
            empty.setForeground(AppTheme.MUTED);
            panel.add(empty);
            resizeCardRow(panel, 0, true);
            return;
        }

        for (MinionCard minion : owner.getBoard()) {
            CardButton button = new CardButton(minion, true);
            if (ownBoard) {
                button.setPlayable(minion.canAttack());
                button.setSelectedCard(minion.getInstanceId().equals(selectedAttackerId));
                button.addActionListener(event -> selectAttacker(minion));
            } else {
                button.addActionListener(event -> attackTarget(minion.getInstanceId()));
            }
            panel.add(button);
        }
        resizeCardRow(panel, owner.getBoard().size(), true);
    }

    private void renderHand(Player player) {
        handPanel.removeAll();
        if (player.getHand().isEmpty()) {
            JLabel empty = new JLabel("Không có bài trên tay");
            empty.setForeground(AppTheme.MUTED);
            handPanel.add(empty);
            resizeCardRow(handPanel, 0, false);
            return;
        }

        for (Card card : player.getHand()) {
            CardButton button = new CardButton(card, false);
            boolean playable = player.hasEnoughMana(card.getManaCost());
            button.setPlayable(playable);
            button.addActionListener(event -> playCard(card));
            handPanel.add(button);
        }
        resizeCardRow(handPanel, player.getHand().size(), false);
    }

    private void resizeCardRow(JPanel panel, int cardCount, boolean onBoard) {
        int cardWidth = onBoard ? 144 : 157;
        int height = onBoard ? 142 : 202;
        panel.setPreferredSize(new Dimension(
                Math.max(300, cardCount * cardWidth + 24),
                height
        ));
    }

    private void playCard(Card card) {
        try {
            String actionMessage = card.getType() == hearthstone.model.card.CardType.MINION
                    ? "Đã triệu hồi " + card.getName()
                    : "Đã sử dụng " + card.getName();
            engine.playCard(game, game.getCurrentPlayerId(), card.getInstanceId());
            SoundPlayer.play(SoundPlayer.Effect.CARD);
            selectedAttackerId = null;
            refreshGame();
            showTemporaryMessage(actionMessage, AppTheme.GREEN);
        } catch (GameRuleException | IllegalArgumentException exception) {
            showRuleError(exception.getMessage());
        }
    }

    private void selectAttacker(MinionCard minion) {
        if (!minion.canAttack()) {
            showRuleError("Minion vừa được triệu hồi hoặc đã tấn công trong lượt này");
            return;
        }
        selectedAttackerId = minion.getInstanceId();
        refreshGame();
        messageLabel.setText(game.getOpponent().hasDefendingMinions()
                ? "Chọn Minion phòng thủ của đối phương"
                : "Chọn Hero của đối phương để tấn công");
        messageLabel.setForeground(AppTheme.BLUE);
    }

    private void attackTarget(String targetId) {
        if (selectedAttackerId == null) {
            showRuleError("Hãy chọn một Minion của bạn trước");
            return;
        }
        try {
            engine.attack(game, game.getCurrentPlayerId(), selectedAttackerId, targetId);
            SoundPlayer.play(SoundPlayer.Effect.ATTACK);
            selectedAttackerId = null;
            refreshGame();
            showTemporaryMessage("Đòn tấn công đã được thực hiện", AppTheme.RED);
        } catch (GameRuleException | IllegalArgumentException exception) {
            showRuleError(exception.getMessage());
        }
    }

    private void endTurn() {
        try {
            String nextName = game.getOpponent().getName();
            engine.endTurn(game, game.getCurrentPlayerId());
            SoundPlayer.play(SoundPlayer.Effect.END_TURN);
            selectedAttackerId = null;
            refreshGame();
            showTemporaryMessage("Đã chuyển lượt cho " + nextName, AppTheme.GOLD);
        } catch (GameRuleException exception) {
            showRuleError(exception.getMessage());
        }
    }

    private void showRuleError(String message) {
        if (SoundPlayer.isEnabled()) {
            Toolkit.getDefaultToolkit().beep();
        }
        showTemporaryMessage(message, AppTheme.RED);
    }

    private void showTemporaryMessage(String message, Color color) {
        if (game.getStatus() == GameStatus.FINISHED) {
            return;
        }
        if (messageTimer != null) {
            messageTimer.stop();
        }
        messageLabel.setText(message);
        messageLabel.setForeground(color);
        messageTimer = new Timer(1800, event -> {
            messageLabel.setForeground(AppTheme.TEXT);
            if (game.getStatus() == GameStatus.IN_PROGRESS) {
                messageLabel.setText("Đến lượt " + game.getCurrentPlayer().getName());
            }
        });
        messageTimer.setRepeats(false);
        messageTimer.start();
    }

    private void checkFinished() {
        if (game.getStatus() != GameStatus.FINISHED) {
            return;
        }
        if (!matchSaved) {
            repository.recordMatch(game);
            matchSaved = true;
        }
        if (resultShown) {
            return;
        }
        resultShown = true;
        if (messageTimer != null) {
            messageTimer.stop();
        }
        messageLabel.setText("Chiến thắng: " + game.getWinnerName());
        messageLabel.setForeground(AppTheme.GOLD);
        SoundPlayer.play(SoundPlayer.Effect.VICTORY);
        SwingUtilities.invokeLater(this::showResultDialog);
    }

    private void returnToMainMenu() {
        if (messageTimer != null) {
            messageTimer.stop();
        }
        new LobbyFrame(repository).setVisible(true);
        dispose();
    }

    private void showResultDialog() {
        Object[] options = {"Về màn hình chính", "Xem lại bàn"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Người chiến thắng: " + game.getWinnerName()
                        + "\nSố lượt: " + game.getTurnNumber(),
                "TRẬN ĐẤU KẾT THÚC",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == JOptionPane.YES_OPTION) {
            returnToMainMenu();
        }
    }
}
