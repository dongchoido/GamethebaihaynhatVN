package hearthstone.ui;

import hearthstone.model.card.Card;
import hearthstone.model.card.CardType;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** Images are loaded from the classpath, so they also work inside a JAR. */
public final class GameAssets {
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static final Map<String, ImageIcon> ICONS = new HashMap<>();
    private static final Map<String, String> CARD_ART = Map.of(
            "FIRE_IMP", "Core Hound.png",
            "FLAME_GUARD", "Argent Commander.png",
            "LAVA_GOLEM", "Boulderfist Ogre.png",
            "FIREBALL", "Pyroblast.png",
            "WARM_LIGHT", "Holy Nova.png",
            "SAPLING", "Frostwolf Grunt.png",
            "WOLF", "Wolfrider.png",
            "ANCIENT", "Sunwalker.png",
            "THORN", "Kill Command.png",
            "RENEW", "Divine Spirit.png"
    );

    private GameAssets() { }

    public static BufferedImage image(String path) {
        if (!IMAGES.containsKey(path)) {
            URL resource = GameAssets.class.getResource("/images/" + path);
            BufferedImage loaded = null;
            try {
                if (resource != null) {
                    loaded = ImageIO.read(resource);
                }
            } catch (IOException ignored) {
                // The text-based interface remains usable if an asset is missing.
            }
            IMAGES.put(path, loaded);
        }
        return IMAGES.get(path);
    }

    public static ImageIcon icon(String path, int width, int height) {
        return scaledIcon(path, width, height, false, false);
    }

    public static ImageIcon cardArt(Card card, int width, int height) {
        String file = CARD_ART.get(card.getCode());
        if (file == null) {
            return icon("design/CardViewBack.png", width, height);
        }
        return scaledIcon("Minions/" + file, width, height, true,
                card.getType() == CardType.MINION);
    }

    public static ImageIcon heroPortrait(String deckCode, int width, int height) {
        String file = "EMBER".equals(deckCode) ? "Jaina Proudmoore.png" : "Rexxar.png";
        return scaledIcon("Heros/" + file, width, height, true, true);
    }

    private static ImageIcon scaledIcon(String path, int width, int height,
                                       boolean cropArt, boolean portrait) {
        String key = path + ":" + width + ":" + height + ":" + cropArt + ":" + portrait;
        if (ICONS.containsKey(key)) {
            return ICONS.get(key);
        }
        BufferedImage source = image(path);
        if (source == null) {
            return null;
        }
        if (cropArt) {
            // Skip the printed name, mana and stats: the game supplies its own values.
            double left = portrait ? 0.27 : 0.22;
            source = source.getSubimage((int) (source.getWidth() * left),
                    (int) (source.getHeight() * 0.12),
                    (int) (source.getWidth() * (portrait ? 0.46 : 0.56)),
                    (int) (source.getHeight() * 0.30));
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = cropArt ? Math.max((double) width / source.getWidth(),
                (double) height / source.getHeight()) : Math.min((double) width / source.getWidth(),
                (double) height / source.getHeight());
        int drawWidth = (int) Math.round(source.getWidth() * scale);
        int drawHeight = (int) Math.round(source.getHeight() * scale);
        g.drawImage(source, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, null);
        g.dispose();
        ImageIcon icon = new ImageIcon(scaled);
        ICONS.put(key, icon);
        return icon;
    }
}
