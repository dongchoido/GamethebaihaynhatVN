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
    private boolean selectedCard;

    public CardButton(Card card, boolean onBoard) {
        setText(createHtml(card, onBoard));
        setPreferredSize(onBoard ? new Dimension(132, 158) : new Dimension(145, 195));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setVerticalAlignment(SwingConstants.TOP);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFocusPainted(false);
        setOpaque(true);
        setContentAreaFilled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBackground(card.getType() == CardType.MINION
                ? new Color(224, 192, 133)
                : new Color(181, 164, 205));
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
        if (!playable) {
            setBackground(new Color(125, 122, 116));
        }
    }

    private String createHtml(Card card, boolean onBoard) {
        String mana = onBoard ? "" : "<b style='color:#175f9f'>◉ "
                + card.getManaCost() + " mana</b><br>";
        String stats;
        if (card instanceof MinionCard minion) {
            stats = "<br><b>⚔ " + minion.getAttack()
                    + " &nbsp;&nbsp; ♥ " + minion.getCurrentHealth() + "</b>";
        } else {
            SpellCard spell = (SpellCard) card;
            stats = "<br><b>✦ " + spell.getPower() + "</b>";
        }

        int width = onBoard ? 105 : 115;
        return "<html><div style='width:" + width + "px;text-align:center'>"
                + mana
                + "<span style='font-size:8px'>" + card.getType().getDisplayName().toUpperCase() + "</span><br>"
                + "<b style='font-size:12px'>" + card.getName() + "</b><br>"
                + "<span style='font-size:9px'>" + card.getDescription() + "</span>"
                + stats
                + "</div></html>";
    }
}
