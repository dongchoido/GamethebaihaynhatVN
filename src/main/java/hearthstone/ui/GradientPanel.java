package hearthstone.ui;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {

    private final Color start;
    private final Color end;
    private Image background;
    private float shade;

    public GradientPanel(Color start, Color end) {
        this.start = start;
        this.end = end;
        setOpaque(false);
    }

    public GradientPanel(String imagePath, float shade) {
        this(AppTheme.BACKGROUND, AppTheme.PANEL);
        this.background = GameAssets.image(imagePath);
        this.shade = shade;
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
        if (background != null) {
            double scale = Math.max((double) getWidth() / background.getWidth(null),
                    (double) getHeight() / background.getHeight(null));
            int width = (int) Math.ceil(background.getWidth(null) * scale);
            int height = (int) Math.ceil(background.getHeight(null) * scale);
            graphics2D.drawImage(background, (getWidth() - width) / 2,
                    (getHeight() - height) / 2, width, height, null);
            graphics2D.setColor(new Color(0, 0, 0, shade));
            graphics2D.fillRect(0, 0, getWidth(), getHeight());
        }
        graphics2D.dispose();
        super.paintComponent(graphics);
    }
}
