package hearthstone.ui;

import hearthstone.model.game.GameState;
import hearthstone.model.game.Player;
import hearthstone.persistence.FileGameRepository;
import hearthstone.service.DeckCatalog;
import hearthstone.service.DeckOption;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LobbyFrame extends JFrame {

    private final FileGameRepository repository;
    private final DeckCatalog deckCatalog = new DeckCatalog();
    private final JTextField player1Field = new JTextField("Duc Anh", 18);
    private final JTextField player2Field = new JTextField("Opponent", 18);
    private final JComboBox<DeckOption> player1Deck;
    private final JComboBox<DeckOption> player2Deck;
    private final JLabel deckDescription = new JLabel(" ");
    private final JLabel player1Preview = new JLabel();
    private final JLabel player2Preview = new JLabel();

    public LobbyFrame(FileGameRepository repository) {
        this.repository = repository;
        DeckOption[] options = deckCatalog.getOptions().toArray(DeckOption[]::new);
        player1Deck = new JComboBox<>(options);
        player2Deck = new JComboBox<>(options);
        if (options.length > 1) {
            player2Deck.setSelectedIndex(1);
        }

        setTitle("Hearthstone Swing - Lobby");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 700));
        setSize(1040, 740);
        setLocationRelativeTo(null);
        setContentPane(createContent());
    }

    private JPanel createContent() {
        GradientPanel root = new GradientPanel("design/StartBG.jpg", 0.64f);
        root.setLayout(new BorderLayout(30, 30));
        root.setBorder(new EmptyBorder(36, 50, 42, 50));

        root.add(createHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 35, 0));
        center.setOpaque(false);
        center.add(createIntroduction());
        center.add(createForm());
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel logo = new JLabel("HEARTHSTONE  LITE");
        logo.setForeground(AppTheme.GOLD);
        logo.setFont(new Font("Georgia", Font.BOLD, 24));
        header.add(logo, BorderLayout.WEST);

        JButton historyButton = new JButton("Lịch sử & thống kê");
        AppTheme.styleSecondaryButton(historyButton);
        historyButton.addActionListener(event ->
                new HistoryDialog(this, repository).setVisible(true));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(AppTheme.soundToggle());
        actions.add(historyButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createIntroduction() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(28, 0, 16, 10));

        JLabel eyebrow = new JLabel("HAI NGƯỜI CHƠI · ĐẤU OFFLINE");
        eyebrow.setForeground(AppTheme.GOLD);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 12f));

        JLabel title = new JLabel("<html>Đấu thẻ bài<br>trên cùng một máy</html>");
        title.setForeground(AppTheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));

        JLabel description = new JLabel(
                "<html><div style='width:250px'>Hai người thay phiên chơi. "
                        + "Triệu hồi Minion, dùng Spell và hạ Hero đối phương về 0 máu.</div></html>");
        description.setForeground(AppTheme.MUTED);
        description.setFont(description.getFont().deriveFont(15f));

        panel.add(eyebrow);
        panel.add(Box.createVerticalStrut(18));
        panel.add(title);
        panel.add(Box.createVerticalStrut(24));
        panel.add(description);
        panel.add(Box.createVerticalStrut(26));
        JPanel heroes = new JPanel(new GridLayout(1, 2, 14, 0));
        heroes.setOpaque(false);
        heroes.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroes.setMaximumSize(new Dimension(360, 138));
        for (JLabel preview : new JLabel[]{player1Preview, player2Preview}) {
            preview.setHorizontalAlignment(SwingConstants.CENTER);
            preview.setHorizontalTextPosition(SwingConstants.CENTER);
            preview.setVerticalTextPosition(SwingConstants.BOTTOM);
            preview.setIconTextGap(8);
            preview.setForeground(AppTheme.TEXT);
            preview.setOpaque(true);
            preview.setBackground(AppTheme.PANEL);
            preview.setBorder(AppTheme.roundedLine(AppTheme.GOLD_DARK, 1));
            heroes.add(preview);
        }
        panel.add(heroes);
        panel.add(Box.createVerticalStrut(20));
        panel.add(featureLabel("MINION · PHÉP SÁT THƯƠNG · HỒI MÁU"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(featureLabel("30 máu · 10 mana · 7 Minion trên bàn"));
        return panel;
    }

    private JLabel featureLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(24, 33, 46));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(91, 75, 57), 1, true),
                new EmptyBorder(24, 28, 24, 28)
        ));

        AppTheme.styleField(player1Field);
        AppTheme.styleField(player2Field);
        AppTheme.styleField(player1Deck);
        AppTheme.styleField(player2Deck);
        player1Deck.addActionListener(event -> deckSelected());
        player2Deck.addActionListener(event -> deckSelected());
        deckDescription.setForeground(AppTheme.MUTED);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(7, 0, 4, 0);

        int row = 0;
        addFormLabel(form, constraints, row++, "Người chơi 1");
        addField(form, constraints, row++, player1Field);
        addFormLabel(form, constraints, row++, "Deck người chơi 1");
        addField(form, constraints, row++, player1Deck);
        addFormLabel(form, constraints, row++, "Người chơi 2");
        addField(form, constraints, row++, player2Field);
        addFormLabel(form, constraints, row++, "Deck người chơi 2");
        addField(form, constraints, row++, player2Deck);

        constraints.gridy = row++;
        constraints.insets = new Insets(12, 0, 12, 0);
        form.add(deckDescription, constraints);

        JButton startButton = new JButton("BẮT ĐẦU TRẬN");
        AppTheme.stylePrimaryButton(startButton);
        startButton.addActionListener(event -> startGame());
        constraints.gridy = row;
        constraints.insets = new Insets(8, 0, 0, 0);
        form.add(startButton, constraints);
        updateDeckDescription();
        return form;
    }

    private void addFormLabel(JPanel panel, GridBagConstraints constraints,
                              int row, String text) {
        constraints.gridy = row;
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(label, constraints);
    }

    private void addField(JPanel panel, GridBagConstraints constraints,
                          int row, JComponent field) {
        constraints.gridy = row;
        field.setPreferredSize(new Dimension(250, 38));
        panel.add(field, constraints);
    }

    private void updateDeckDescription() {
        DeckOption deck = (DeckOption) player1Deck.getSelectedItem();
        if (deck != null) {
            deckDescription.setText("<html><div style='width:220px'>"
                    + deck.description() + "</div></html>");
        }
        updateHeroPreview(player1Preview, (DeckOption) player1Deck.getSelectedItem());
        updateHeroPreview(player2Preview, (DeckOption) player2Deck.getSelectedItem());
    }

    private void deckSelected() {
        updateDeckDescription();
        SoundPlayer.play(SoundPlayer.Effect.SELECT);
    }

    private void updateHeroPreview(JLabel preview, DeckOption deck) {
        if (deck != null) {
            preview.setIcon(GameAssets.heroPortrait(deck.code(), 130, 76));
            preview.setText(deck.name());
            preview.setToolTipText(deck.description());
        }
    }

    private void startGame() {
        String player1Name = player1Field.getText().trim();
        String player2Name = player2Field.getText().trim();
        if (player1Name.isEmpty() || player2Name.isEmpty()) {
            showError("Tên người chơi không được để trống");
            return;
        }
        if (player1Name.equalsIgnoreCase(player2Name)) {
            showError("Hai người chơi cần có tên khác nhau");
            return;
        }

        DeckOption deck1 = (DeckOption) player1Deck.getSelectedItem();
        DeckOption deck2 = (DeckOption) player2Deck.getSelectedItem();
        if (deck1 == null || deck2 == null) {
            showError("Hãy chọn deck cho cả hai người chơi");
            return;
        }

        try {
            repository.ensureUser(player1Name);
            repository.ensureUser(player2Name);
            repository.recordDeckSelection(player1Name, deck1.code(), deck1.name());
            repository.recordDeckSelection(player2Name, deck2.code(), deck2.name());

            Player player1 = new Player(player1Name, deck1.code(), deck1.name(),
                    deckCatalog.createDeck(deck1.code()));
            Player player2 = new Player(player2Name, deck2.code(), deck2.name(),
                    deckCatalog.createDeck(deck2.code()));
            GameState game = new GameState(player1, player2);

            new GameFrame(game, repository).setVisible(true);
            SoundPlayer.play(SoundPlayer.Effect.PLAY);
            dispose();
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                "Không thể bắt đầu", JOptionPane.WARNING_MESSAGE);
    }
}
