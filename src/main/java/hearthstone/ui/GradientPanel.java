package hearthstone.ui;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {

    private final Color start;
    private final Color end;

    public GradientPanel(Color start, Color end) {
        this.start = start;
        this.end = end;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setPaint(new GradientPaint(
                0, 0, start,
                getWidth(), getHeight(), end
        ));
        graphics2D.fillRect(0, 0, getWidth(), getHeight());
        graphics2D.dispose();
        super.paintComponent(graphics);
    }
}
