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
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(AppTheme.BACKGROUND);
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
        JPanel field = new JPanel();
        field.setBackground(AppTheme.BACKGROUND);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));

        JPanel opponentHeroRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        opponentHeroRow.setBackground(new Color(48, 28, 31));
        styleHeroButton(opponentHeroButton, true);
        opponentHeroButton.addActionListener(event -> attackTarget(GameEngine.HERO_TARGET));
        opponentHeroRow.add(opponentHeroButton);

        JScrollPane opponentScroll = createHorizontalScroll(opponentBoard, new Color(42, 29, 32));

        JLabel versus = new JLabel("—  VS  —", SwingConstants.CENTER);
        versus.setForeground(AppTheme.GOLD);
        versus.setFont(new Font("Georgia", Font.BOLD, 19));
        versus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JScrollPane currentScroll = createHorizontalScroll(currentBoard, new Color(25, 39, 48));

        JPanel currentHeroRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 8));
        currentHeroRow.setBackground(new Color(25, 39, 48));
        currentHeroLabel.setOpaque(true);
        currentHeroLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentHeroLabel.setPreferredSize(new Dimension(190, 76));
        currentHeroLabel.setBackground(new Color(55, 47, 41));
        currentHeroLabel.setForeground(AppTheme.TEXT);
        currentHeroLabel.setBorder(AppTheme.roundedLine(AppTheme.GOLD_DARK, 2));

        manaLabel.setOpaque(true);
        manaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        manaLabel.setPreferredSize(new Dimension(85, 70));
        manaLabel.setBackground(new Color(24, 103, 161));
        manaLabel.setForeground(Color.WHITE);
        manaLabel.setBorder(BorderFactory.createLineBorder(new Color(105, 200, 247), 3, true));
        currentHeroRow.add(currentHeroLabel);
        currentHeroRow.add(manaLabel);

        JScrollPane handScroll = createHorizontalScroll(handPanel, new Color(29, 31, 39));
        handScroll.setPreferredSize(new Dimension(900, 220));

        field.add(opponentHeroRow);
        field.add(opponentScroll);
        field.add(Box.createVerticalStrut(6));
        field.add(versus);
        field.add(Box.createVerticalStrut(6));
        field.add(currentScroll);
        field.add(currentHeroRow);
        field.add(handScroll);
        return field;
    }

    private JPanel createCardRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        panel.setOpaque(false);
        return panel;
    }

    private JScrollPane createHorizontalScroll(JPanel content, Color background) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(background);
        wrapper.add(content, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(65, 63, 61), 1, true));
        scroll.getViewport().setBackground(background);
        scroll.setPreferredSize(new Dimension(900, 182));
        scroll.getHorizontalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private void styleHeroButton(JButton button, boolean opponent) {
        button.setPreferredSize(new Dimension(210, 78));
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
        manaLabel.setText("<html><div style='text-align:center'><b>"
                + current.getCurrentMana() + "/" + current.getTotalMana()
                + "</b><br><small>MANA</small></div></html>");

        opponentHeroButton.setText(heroHtml(opponent));
        currentHeroLabel.setText(heroHtml(current));
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
        return "<html><div style='text-align:center'><b>" + player.getName()
                + "</b><br><span style='color:#ff9a86'>♥ " + player.getHero().getHealth()
                + "/30</span><br><small>" + player.getDeckName()
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
        int height = onBoard ? 178 : 215;
        panel.setPreferredSize(new Dimension(
                Math.max(920, cardCount * cardWidth + 24),
                height
        ));
    }

    private void playCard(Card card) {
        try {
            String actionMessage = card.getType() == hearthstone.model.card.CardType.MINION
                    ? "Đã triệu hồi " + card.getName()
                    : "Đã sử dụng " + card.getName();
            engine.playCard(game, game.getCurrentPlayerId(), card.getInstanceId());
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
        messageLabel.setText("Chọn Hero hoặc Minion của đối phương");
        messageLabel.setForeground(AppTheme.BLUE);
    }

    private void attackTarget(String targetId) {
        if (selectedAttackerId == null) {
            showRuleError("Hãy chọn một Minion của bạn trước");
            return;
        }
        try {
            engine.attack(game, game.getCurrentPlayerId(), selectedAttackerId, targetId);
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
            selectedAttackerId = null;
            refreshGame();
            showTemporaryMessage("Đã chuyển lượt cho " + nextName, AppTheme.GOLD);
        } catch (GameRuleException exception) {
            showRuleError(exception.getMessage());
        }
    }

    private void showRuleError(String message) {
        Toolkit.getDefaultToolkit().beep();
        showTemporaryMessage(message, AppTheme.RED);
    }

    private void showTemporaryMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setForeground(color);
        Timer timer = new Timer(1800, event -> {
            messageLabel.setForeground(AppTheme.TEXT);
            if (game.getStatus() == GameStatus.IN_PROGRESS) {
                messageLabel.setText("Đến lượt " + game.getCurrentPlayer().getName());
            }
        });
        timer.setRepeats(false);
        timer.start();
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
        SwingUtilities.invokeLater(this::showResultDialog);
    }

    private void returnToMainMenu() {
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
