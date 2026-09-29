package hearthstone.ui;

import hearthstone.model.card.Card;
import hearthstone.model.card.CardType;
import hearthstone.model.card.MinionCard;
import hearthstone.model.card.SpellCard;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CardButton extends JButton {

    private final Border normalBorder;
    private final Color cardColor;
    private boolean selectedCard;

    public CardButton(Card card, boolean onBoard) {
        setText(createHtml(card, onBoard));
        setIcon(GameAssets.cardArt(card, onBoard ? 110 : 122, onBoard ? 55 : 65));
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(5);
        setToolTipText(card.getName() + " — " + card.getDescription());
        getAccessibleContext().setAccessibleName(card.getName());
        setPreferredSize(onBoard ? new Dimension(132, 122) : new Dimension(145, 182));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setVerticalAlignment(SwingConstants.TOP);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFocusPainted(false);
        setOpaque(true);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardColor = card.getType() == CardType.MINION
                ? new Color(224, 192, 133)
                : new Color(203, 189, 223);
        setBackground(cardColor);
        setForeground(new Color(45, 33, 24));

        normalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.GOLD_DARK, 3, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
        setBorder(normalBorder);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!selectedCard) {
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(AppTheme.GOLD, 4, true),
                            BorderFactory.createEmptyBorder(4, 4, 4, 4)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!selectedCard) {
                    setBorder(normalBorder);
                }
            }
        });
    }

    public void setSelectedCard(boolean selected) {
        selectedCard = selected;
        setBorder(selected
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BLUE, 5, true),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3))
                : normalBorder);
    }

    public void setPlayable(boolean playable) {
        setBackground(playable ? cardColor : new Color(147, 146, 142));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(0, 0, getBackground().brighter(),
                0, getHeight(), getBackground()));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g.dispose();
        super.paintComponent(graphics);
    }

    private String createHtml(Card card, boolean onBoard) {
        String mana = onBoard ? "" : "<b style='color:#175f9f'>"
                + card.getManaCost() + " MANA</b><br>";
        String stats;
        if (card instanceof MinionCard minion) {
            stats = "<br><b>ATK " + minion.getAttack()
                    + " &nbsp; <span style='color:#a82e2e'>HP "
                    + minion.getCurrentHealth() + "</span></b>";
        } else {
            SpellCard spell = (SpellCard) card;
            stats = "<br><b>" + (card.getType() == CardType.HEAL_SPELL ? "+" : "−")
                    + spell.getPower() + " HP</b>";
        }

        int width = onBoard ? 82 : 91;
        return "<html><div style='width:" + width + "px;text-align:center;font-size:9px'>"
                + mana
                + "<b style='font-size:10px'>" + AppTheme.escapeHtml(card.getName()) + "</b>"
                + (onBoard ? "" : "<br><span style='font-size:8px'>"
                + AppTheme.escapeHtml(card.getDescription()) + "</span>")
                + stats
                + "</div></html>";
    }
}
