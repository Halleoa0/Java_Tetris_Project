import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Swing 화면, 키 입력, 주기적인 게임 업데이트를 연결한다.
 * 사용 예: IntelliJ에서 Main을 실행한 뒤 방향키로 이동하고 Space로 즉시 낙하한다.
 * 조작: ←/→ 이동, ↓ 소프트드롭, ↑ 또는 X 시계 회전, Z 반시계 회전,
 * Space 하드드롭, C Hold, R 재시작.
 */
final class GamePanel extends JPanel {
    @FunctionalInterface
    interface InputFilter { boolean allow(String action); }

    // 조각의 크기는 25 (25x25), 보드의 위치는 x 517, x 111
    private static final int CELL = 25, BOARD_X = 517, BOARD_Y = 111;
    private static final int MATRIX_X = 378, MATRIX_Y = 111, MATRIX_W = 528, MATRIX_H = 505;
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color PANEL = new Color(30, 36, 51);
    private final Game game;
    private InputFilter inputFilter;
    private Consumer<Graphics2D> overlayRenderer;
    private long lastTick = System.nanoTime();
    private final Image matrixImage;
    private final Image guideImage;
    private final Image gameNamePanelImage;
    private final LabelUI labelUI;
    private final BufferedImage[] minoTiles;
    public final Font interBlack;
    public final Font interMedium;
    public final Font sansKRBlack;
    public final Font orbitBlack;
    public final Font orbitBold;
    private final Set<String> heldKeys = new HashSet<>();
    private final Set<String> repeatingKeys = new HashSet<>();
    private final Map<String, Integer> heldKeyElapsed = new HashMap<>();
    private final Map<String, Runnable> heldKeyActions = new HashMap<>();

    // 현재 진행 중 모드
    private String gameName = "테스트 플레이";

    // 키를 게임 동작에 연결하고 16ms 간격으로 게임 상태를 갱신한다.
    GamePanel() { this(new Game()); }

    GamePanel(Game game) {
        this.game = game;
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setFocusable(true);
        interBlack = loadBlack();
        interMedium = loadMedium();
        sansKRBlack = loadKRBlack();
        orbitBlack = loadOrbitBlack();
        orbitBold = loadOrbitBold();
        matrixImage = loadImage("Images/Board/Matrix.png");
        guideImage = loadImage("Images/Board/Guide.png");
        gameNamePanelImage = loadImage("Images/Board/GameNamePanel.png");
        labelUI = new LabelUI(game, interBlack, interMedium, sansKRBlack, orbitBlack, orbitBold, gameNamePanelImage, gameName);
        minoTiles = loadMinoTiles("Images/Mino.png");
        bindHeld("LEFT", () -> game.move(-1, 0));
        bindHeld("RIGHT", () -> game.move(1, 0));
        bindHeld("DOWN", game::softDrop);
        // bind("UP", "rotateCW", () -> game.rotate(1));
        bind("UP", "rotateCW", () -> game.rotate(1));
        bind("X", "rotateCW", () -> game.rotate(1));
        bind("Z", "rotateCCW", () -> game.rotate(-1));
        bind("SPACE", "hardDrop", game::hardDrop);
        bind("C", "hold", game::hold);
        bind("R", "restart", game::restart);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { clearHeldKeys(); }
        });

        new Timer(16, e -> {
            long now = System.nanoTime();
            int elapsed = (int) Math.min(100, (now - lastTick) / 1_000_000L);
            lastTick = now;
            game.tick(elapsed);
            tickHeldKeys(elapsed);
            game.scoring.tick(elapsed);
            repaint();
        }).start();
    }

    void setInputFilter(InputFilter inputFilter) { this.inputFilter = inputFilter; }

    void setOverlayRenderer(Consumer<Graphics2D> overlayRenderer) { this.overlayRenderer = overlayRenderer; }


    // 게임 윈도우 창을 만듦
    void showWindow() {
        JFrame frame = new JFrame("Modern Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(this);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        requestFocusInWindow();
    }

    /// 키보드 입력을 bind 하기
    private void bind(String key, String name, Runnable action) {
        // Swing의 Key Binding으로 키 입력을 게임 동작에 연결한다.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (inputFilter == null || inputFilter.allow(name)) action.run();
                repaint();
            }
        });
    }

    private void bindHeld(String key, Runnable action) {
        String pressedName = key + "HeldPressed";
        String releasedName = key + "HeldReleased";
        heldKeyActions.put(key, action);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), pressedName);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released " + key), releasedName);
        getActionMap().put(pressedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (heldKeys.add(key)) {
                    heldKeyElapsed.put(key, 0);
                    repeatingKeys.remove(key);
                    action.run();
                    repaint();
                }
            }
        });
        getActionMap().put(releasedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { releaseHeldKey(key); }
        });
    }


    /// 키 입력 후에 다시 재입력
    private void tickHeldKeys(int elapsedMs) {
        for (String key : heldKeys) {
            int elapsed = heldKeyElapsed.getOrDefault(key, 0) + elapsedMs;
            int delay = repeatingKeys.contains(key) ? 55 : 180;
            if (elapsed >= delay) {
                elapsed -= delay;
                repeatingKeys.add(key);
                heldKeyActions.get(key).run();
            }
            heldKeyElapsed.put(key, elapsed);
        }
    }

    private void releaseHeldKey(String key) {
        heldKeys.remove(key);
        repeatingKeys.remove(key);
        heldKeyElapsed.remove(key);
    }

    private void clearHeldKeys() {
        heldKeys.clear();
        repeatingKeys.clear();
        heldKeyElapsed.clear();
    }

    /// 실제로 그리기
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(matrixImage, MATRIX_X, MATRIX_Y, MATRIX_W, MATRIX_H, this);
        graphics.drawImage(guideImage, 40, 545, this);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawMatrixPreviews(g);
        drawBoard(g);
        labelUI.drawGameNamePanel(g);
        labelUI.drawPpsLabel(g);
        labelUI.drawLineLabel(g);
        labelUI.drawScoreValueLabel(g);
        labelUI.drawTimeLabel(g);
        labelUI.drawScoreLabel(g);
        if (game.gameOver) drawGameOver(g);
        if (overlayRenderer != null) overlayRenderer.accept(g);
        g.dispose();
    }

    /// 이미지를 가져오는 함수
    private Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(Path.of(path).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 불러올 수 없음. " + path, e);
        }
    }

    /// 미노 이미지를 불러옵니다. (25x25 사이즈가 8개가 가로로 존재하는 200x25 크기의 이미지)
    private BufferedImage[] loadMinoTiles(String path) {
        try {

            BufferedImage sheet = javax.imageio.ImageIO.read(Path.of(path).toFile());
            if (sheet.getWidth() != 200 || sheet.getHeight() != 25) {
                throw new IllegalStateException("이미지 사이즈가 200x25가 아님. " + path);
            }

            // 이미지를 25 크기마다 자름.
            BufferedImage[] tiles = new BufferedImage[8];
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = sheet.getSubimage(i * 25, 0, 25, 25);
            }

            return tiles;

        } catch (IOException e) {
            throw new IllegalStateException("미노 이미지에 문제 발생. " + path, e);
        }
    }


    /// 이 아래의 코드는 디자인 변화 시 수정될 수 있는 코드입니다.
    /// 보드 및 미노(블록)을 png 파일로 대체하여 아래보다 코드가 간단해질 수 있습니다.


    // 보드와 쌓은 블록, 현재블록과 고스트를 그림
    private void drawBoard(Graphics2D g) {
        for (int row = 0; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                Tetromino locked = game.board.get(col, row);
                if (locked != null) drawCell(g, sx, sy, locked, false);
            }
        }

        if (!game.gameOver) {
            // 현재 블록을 아래로 복사 이동해 예상 착지 위치(고스트)를 먼저 그린다.
            int ghostY = game.y;
            while (game.board.canPlace(game.active, game.x, ghostY + 1, game.rotation)) ghostY++;
            drawPiece(g, game.active, game.x, ghostY, game.rotation, true);
            drawPiece(g, game.active, game.x, game.y, game.rotation, false);
        }
    }


    // 미노 만들기
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            if (by < -Board.HIDDEN_ROWS || by >= Board.HEIGHT) continue;
            int visibleY = by - Board.HIDDEN_ROWS;
            drawCell(g, BOARD_X + bx * CELL, BOARD_Y + visibleY * CELL, type, ghost);
        }
    }


    // 보드 뒤에 격자 만들기
    private void drawCell(Graphics2D g, int x, int y, Tetromino type, boolean ghost) {
        int tile = ghost ? 7 : minoTileIndex(type);
        g.drawImage(minoTiles[tile], x, y, CELL, CELL, this);
    }

    private int minoTileIndex(Tetromino type) {
        return switch (type) {
            case Zmino -> 0;
            case Lmino -> 1;
            case Omino -> 2;
            case Smino -> 3;
            case Imino -> 4;
            case Jmino -> 5;
            case Tmino -> 6;
        };
    }

    // 텍스트 정보를 띄웁니다.
    private void drawInfoBox(Graphics2D g, int x, int y, int w, int h, int fontSize, String label, String value) {
        g.setColor(PANEL);
        g.setColor(new Color(255, 255, 255)); g.setFont(interBlack.deriveFont((float) fontSize));
        g.drawString(label, x, y);
        g.setColor(Color.WHITE); g.setFont(interBlack.deriveFont(20f));
        g.drawString(value, x + 12, y + h - 14);
    }

    // 200x15 사이즈 Mino.png를 25x25로 잘라서 미노로 씁니다.
    private void drawMiniCell(Graphics2D g, int x, int y, int size, Tetromino type) {
        g.drawImage(minoTiles[minoTileIndex(type)], x, y, size, size, this);
    }

    // Next와 Hold에 미노를 표시합니다.
    private void drawMatrixPreviews(Graphics2D g) {
        if (game.held != null) {
            drawPreviewPiece(g, game.held, 386, 112, 126, 78, 25);
        }

        List<Tetromino> next = game.preview();
        for (int i = 0; i < next.size(); i++) {
            drawPreviewPiece(g, next.get(i), 774, 120 + i * 68, 126, 50, 25);
        }
    }


    private void drawPreview(Graphics2D g, int x, int y, String title, List<Tetromino> pieces, boolean hold) {
        g.setColor(PANEL); g.fillRoundRect(x, y, 156, hold ? 120 : 204, 10, 10);
        g.setColor(new Color(164, 174, 195)); g.setFont(new Font("SansSerif", Font.BOLD, 11)); g.drawString(title, x + 12, y + 20);
        int slotH = hold ? 86 : 35;
        for (int i = 0; i < pieces.size(); i++) {
            Tetromino type = pieces.get(i);
            int minX=4, maxX=0, minY=4, maxY=0;
            for (int[] c : type.cells(0)) { minX=Math.min(minX,c[0]); maxX=Math.max(maxX,c[0]); minY=Math.min(minY,c[1]); maxY=Math.max(maxY,c[1]); }
            int unit = hold ? 18 : 14;
            int startX = x + (156 - (maxX-minX+1)*unit)/2;
            int startY = y + 27 + i*slotH + (slotH - (maxY-minY+1)*unit)/2;
            for (int[] c : type.cells(0)) drawMiniCell(g, startX + (c[0]-minX)*unit, startY + (c[1]-minY)*unit, unit, type);
        }
    }

    // 4개까지 NEXT 미노를 보여줌
    private void drawPreviewPiece(Graphics2D g, Tetromino type, int x, int y, int width, int height, int unit) {
        int minX = 4, maxX = 0, minY = 4, maxY = 0;
        int[][] cells = type.cells(0);
        for (int[] cell : cells) {
            minX = Math.min(minX, cell[0]); maxX = Math.max(maxX, cell[0]);
            minY = Math.min(minY, cell[1]); maxY = Math.max(maxY, cell[1]);
        }
        int pieceWidth = (maxX - minX + 1) * unit;
        int pieceHeight = (maxY - minY + 1) * unit;
        int startX = x + (width - pieceWidth) / 2 - minX * unit;
        int startY = y + (height - pieceHeight) / 2 - minY * unit;
        for (int[] cell : cells) {
            drawMiniCell(g, startX + cell[0] * unit, startY + cell[1] * unit, unit, type);
        }
    }

    // 게임 오버 시
    private void drawGameOver(Graphics2D g) {
        int x = BOARD_X - 7, y = BOARD_Y + 230;
        g.setColor(new Color(12, 16, 25, 220)); g.fillRoundRect(x + 15, y, 250, 100, 12, 12);
        g.setColor(Color.WHITE); g.setFont(interBlack.deriveFont(24f));
        FontMetrics fm = g.getFontMetrics(); String text = "GAME OVER";
        g.drawString(text, x + (280 - fm.stringWidth(text))/2, y + 39);
        g.setFont(interBlack.deriveFont(14f));
        text = "Press R to restart"; fm = g.getFontMetrics();
        g.drawString(text, x + (280 - fm.stringWidth(text))/2, y + 68);
    }

    /// 폰트를 가져옵니다. Black(가장 굵은)
    private Font loadBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter_18pt-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter_18pt-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    /// 폰트를 가져옵니다. Medium(보통)
    private Font loadMedium() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter_18pt-Medium.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter_18pt-Medium 폰트를 불러올 수 없습니다.", e);
        }
    }

    /// 폰트를 가져옵니다. 한글 전용
    private Font loadKRBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/NotoSansKR-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("NotoSansKR-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    private Font loadOrbitBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Orbitron-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Orbitron-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    private Font loadOrbitBold() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Orbitron-Bold.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Orbitron-Bold 폰트를 불러올 수 없습니다.", e);
        }
    }
}
