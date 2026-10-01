import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Set;

/* Swing 화면, 키 입력, 주기적인 게임 업데이트를 연결한다.
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
    private static final String[] PAUSE_OPTIONS = { "RESUME", "RESTART", "QUIT" };

    private final Game game;
    private InputFilter inputFilter;
    private Consumer<Graphics2D> overlayRenderer;
    private long lastTick = System.nanoTime();

    /// 이미지 불러오기
    private final Image matrixImage;
    private final Image guideImage;
    private final Image pauseGuideImage;
    private final Image gameNamePanelImage;

    private final LabelUI labelUI;
    private final BufferedImage[] minoTiles;

    /// 폰트 불러오기
    public final Font interBlack = MenuFonts.loadBlack();
    public final Font interBold = MenuFonts.loadBold();
    public final Font interMedium = MenuFonts.loadMedium();
    public final Font sansKRBlack = MenuFonts.loadKRBlack();
    public final Font orbitBlack = MenuFonts.loadOrbitBlack();
    public final Font orbitBold = MenuFonts.loadOrbitBold();

    private final Set<String> heldKeys = new HashSet<>();
    private final Set<String> pressedMoveKeys = new HashSet<>();
    private final Set<String> repeatingKeys = new HashSet<>();
    private final Map<String, Integer> heldKeyElapsed = new HashMap<>();
    private final Map<String, Runnable> heldKeyActions = new HashMap<>();
    private final Set<Settings.Action> initialKeys = new HashSet<>();
    private int lastInitialRotation = 1;

    private final Timer timer;
    private boolean paused;
    private int pauseSelection;
    private boolean ignoreSpaceUntilRelease;
    private Runnable homeCallback = () -> { };
    // 일시정지 메뉴의 재시작 콜백. null이면 game.restart()를 수행한다.
    private Runnable pauseRestartCallback;

    // 하드 드롭 직후 락 효과음 중복 재생을 방지한다.
    private boolean justHardDropped;

    private final JButton gameOverRestartButton = new JButton("다시 시작");
    private final JButton gameOverMenuButton = new JButton("메인화면");

    // 현재 진행 중 모드
    private String gameName = "테스트 플레이";

    // 키를 게임 동작에 연결하고 16ms 간격으로 게임 상태를 갱신한다.
    GamePanel() { this(new Game()); }

    GamePanel(Game game) {
        this.game = game;
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setFocusable(true);

        matrixImage = loadImage("Images/Board/Matrix.png");
        guideImage = loadImage("Images/Board/Guide.png");
        pauseGuideImage = loadImage("Images/PauseGuide.png");
        gameNamePanelImage = loadImage("Images/Board/GameNamePanel.png");
        labelUI = new LabelUI(game, interBlack, interMedium, sansKRBlack, orbitBlack, orbitBold, gameNamePanelImage, gameName);
        minoTiles = loadMinoTiles("Images/Mino.png");
        bindHeld(Settings.Action.MOVE_LEFT,  () -> game.move(-1, 0));
        bindHeld(Settings.Action.MOVE_RIGHT, () -> game.move(1, 0));
        bindHeld(Settings.Action.SOFT_DROP,  game::softDrop);
        bindInitial(Settings.Action.ROTATE_CW,  "rotateCW",  () -> game.rotate(1));
        bindInitial(Settings.Action.ROTATE_CCW, "rotateCCW", () -> game.rotate(-1));
        bind(Settings.Action.HARD_DROP,  "hardDrop",  game::hardDrop);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(Settings.Action.HARD_DROP), "pauseSpaceReleased");
        getActionMap().put("pauseSpaceReleased", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ignoreSpaceUntilRelease = false; }
        });
        bindInitial(Settings.Action.HOLD, "hold", game::hold);
        bind(Settings.Action.RESTART, "restart", () -> {
            game.restart();
            AudioManager.get().startGameBgm();
        });
        bindPauseNavigation("UP", -1);
        bindPauseNavigation("DOWN", 1);

        bindUnfiltered("ESCAPE", "pauseToggle", () -> { if (!paused) setPaused(true); });
        bindUnfiltered("ENTER", "pauseConfirm", () -> { if (paused) confirmPauseSelection(); });
        bindUnfiltered("M", "menu", () -> { if (paused || game.gameOver) homeCallback.run(); });
        configureOverlayButtons();

        // 게임 동작에 맞춰 효과음을 재생한다.
        game.addListener(new GameListener() {
            @Override public void onMove(int dx, int dy) {
                AudioManager.get().playMove();
            }
            @Override public void onRotate(int direction) {
                AudioManager.get().playRotate();
            }
            @Override public void onSoftDrop() {
                AudioManager.get().playSoftDrop();
            }
            @Override public void onHardDrop() {
                justHardDropped = true;
                AudioManager.get().playHardDrop();
            }
            @Override public void onHold() {
                AudioManager.get().playHold();
            }
            @Override public void onLock(Tetromino type) {
                // 하드 드롭 직후에는 락 효과음을 생략한다.
                if (justHardDropped) {
                    justHardDropped = false;
                } else {
                    AudioManager.get().playLock();
                }
            }
            @Override public void onLinesCleared(int lines) {
                AudioManager.get().playLineClear(lines);
            }
            @Override public void onLevelUp(int newLevel) {
                AudioManager.get().playLevelUpDelayed();
            }
            @Override public void onGameOver() {
                AudioManager.get().playGameOver();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                clearHeldKeys();
                ignoreSpaceUntilRelease = false;
                AudioManager.get().setWindowActive(false);
            }
            @Override public void focusGained(FocusEvent e) {
                AudioManager.get().setWindowActive(true);
            }
        });

        timer = new Timer(16, e -> {
            long now = System.nanoTime();
            int elapsed = (int) Math.min(100, (now - lastTick) / 1_000_000L);
            lastTick = now;
            game.tick(elapsed);
            tickHeldKeys(elapsed);
            game.scoring.tick(elapsed);
            updateOverlayButtons();
            repaint();
        });
        timer.start();
    }

    void setInputFilter(InputFilter inputFilter) {
        this.inputFilter = inputFilter;
        updateInitialInputs();
    }

    void setOverlayRenderer(Consumer<Graphics2D> overlayRenderer) { this.overlayRenderer = overlayRenderer; }

    /** 일시정지 메뉴에서 RESTART를 골랐을 때 실행할 동작을 지정한다. null이면 기본 game.restart(). */
    void setPauseRestartCallback(Runnable callback) { this.pauseRestartCallback = callback; }

    void setHomeCallback(Runnable homeCallback) {
        this.homeCallback = homeCallback == null ? () -> { } : homeCallback;
    }

    private void setPaused(boolean paused) {
        if (this.paused == paused) return;
        this.paused = paused;
        clearHeldKeys();
        if (paused) {
            pauseSelection = 0;
            timer.stop();
            AudioManager.get().pauseBgm(); // BGM을 일시정지한다.
        }
        else {
            lastTick = System.nanoTime();
            timer.restart();
            AudioManager.get().resumeBgm(); // BGM을 재개한다.
        }
        updateOverlayButtons();
        repaint();
    }

    private void configureOverlayButtons() {
        gameOverRestartButton.setBounds(548, 414, 108, 30);
        gameOverMenuButton.setBounds(662, 414, 96, 30);
        gameOverRestartButton.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            game.restart();
            AudioManager.get().startGameBgm(); // 인게임 BGM을 무작위로 재생한다.
            updateOverlayButtons();
            repaint();
            requestFocusInWindow();
        });

        gameOverMenuButton.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            homeCallback.run();
        });
        add(gameOverRestartButton);
        add(gameOverMenuButton);
        updateOverlayButtons();
    }

    private void updateOverlayButtons() {
        gameOverRestartButton.setVisible(game.gameOver && !paused);
        gameOverMenuButton.setVisible(game.gameOver && !paused);
    }


    /** 화면에서 제거될 때 주기적인 게임 업데이트를 멈춘다. */
    void stop() {
        timer.stop();
        clearHeldKeys();
    }

    /// 설정된 키 입력을 게임 동작에 연결한다.
    private void bind(Settings.Action action, String name, Runnable run) {
        // 동작별 키 설정을 액션에 등록한다.
        String bindingName = name + "_" + action.name();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), bindingName);
        getActionMap().put(bindingName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (action == Settings.Action.HARD_DROP) {
                    if (ignoreSpaceUntilRelease) return;
                    ignoreSpaceUntilRelease = true;
                }
                if (paused) handlePauseInput(action);
                else if (inputFilter == null || inputFilter.allow(name)) run.run();
                repaint();
            }
        });
    }

    /** 즉시 조작은 한 번만 실행하고, 키 유지 상태는 다음 스폰에 전달한다. */
    private void bindInitial(Settings.Action action, String name, Runnable run) {
        String pressedName = name + "_" + action.name();
        String releasedName = pressedName + "Released";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressedName);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), releasedName);
        getActionMap().put(pressedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused) handlePauseInput(action);
                else if ((inputFilter == null || inputFilter.allow(name)) && initialKeys.add(action)) {
                    if (action == Settings.Action.ROTATE_CW) lastInitialRotation = 1;
                    else if (action == Settings.Action.ROTATE_CCW) lastInitialRotation = -1;
                    updateInitialInputs();
                    run.run();
                }
                repaint();
            }
        });
        getActionMap().put(releasedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                initialKeys.remove(action);
                updateInitialInputs();
            }
        });
    }

    private void updateInitialInputs() {
        boolean cw = initialKeyAllowed(Settings.Action.ROTATE_CW, "rotateCW");
        boolean ccw = initialKeyAllowed(Settings.Action.ROTATE_CCW, "rotateCCW");
        int direction = cw && ccw ? lastInitialRotation : cw ? 1 : ccw ? -1 : 0;
        game.setInitialInputs(initialKeyAllowed(Settings.Action.HOLD, "hold"), direction);
    }

    private boolean initialKeyAllowed(Settings.Action action, String name) {
        return initialKeys.contains(action) && (inputFilter == null || inputFilter.allow(name));
    }

    private void bindHeld(Settings.Action action, Runnable run) {
        String key = action.name();                       // heldKeys 등 기존 Set/Map의 키로 그대로 사용
        String pressedName = key + "HeldPressed";
        String releasedName = key + "HeldReleased";
        heldKeyActions.put(key, run);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressedName);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), releasedName);
        getActionMap().put(pressedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused) {
                    handlePauseInput(action); repaint();
                }
                else {
                    if (action == Settings.Action.MOVE_LEFT || action == Settings.Action.MOVE_RIGHT)
                        pressedMoveKeys.add(key);
                    if (!oppositeMoveHeld(action)) activateHeldKey(key);
                }
            }
        });
        getActionMap().put(releasedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { releaseHeldKey(key); }
        });
    }



    private void activateHeldKey(String key) {
        if (!heldKeys.add(key)) return;
        heldKeyElapsed.put(key, 0);
        repeatingKeys.remove(key);
        heldKeyActions.get(key).run();
        repaint();
    }

    private boolean oppositeMoveHeld(Settings.Action action) {
        return (action == Settings.Action.MOVE_LEFT && heldKeys.contains(Settings.Action.MOVE_RIGHT.name()))
                || (action == Settings.Action.MOVE_RIGHT && heldKeys.contains(Settings.Action.MOVE_LEFT.name()));
    }

    /// 키 입력 후에 다시 재입력
    private void tickHeldKeys(int elapsedMs) {
        for (String key : heldKeys) {
            int elapsed = heldKeyElapsed.getOrDefault(key, 0) + elapsedMs;
            int delay = repeatingKeys.contains(key) ? Math.max(1, Settings.get().arrMs())
                    : Settings.get().dasMs();
            if (elapsed >= delay) {
                elapsed -= delay;
                repeatingKeys.add(key);
                heldKeyActions.get(key).run();
            }
            heldKeyElapsed.put(key, elapsed);
        }
    }

    private void releaseHeldKey(String key) {
        boolean wasHeld = heldKeys.remove(key);
        repeatingKeys.remove(key);
        heldKeyElapsed.remove(key);
        pressedMoveKeys.remove(key);
        // 반대 키가 이미 눌려 있으면 OS 반복 입력을 기다리지 않고 즉시 전환한다.
        String opposite = key.equals(Settings.Action.MOVE_LEFT.name()) ? Settings.Action.MOVE_RIGHT.name()
                : key.equals(Settings.Action.MOVE_RIGHT.name()) ? Settings.Action.MOVE_LEFT.name() : null;
        if (wasHeld && opposite != null && pressedMoveKeys.contains(opposite)) activateHeldKey(opposite);
    }

    private void clearHeldKeys() {
        heldKeys.clear();
        pressedMoveKeys.clear();
        repeatingKeys.clear();
        heldKeyElapsed.clear();
        initialKeys.clear();
        game.setInitialInputs(false, 0);
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
        if (paused) drawPauseOverlay(g);
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

            // 색맹 모드: 설정에 따라 미노 타일(0~6번)의 색만 바꾼다. 고스트(7번)는 그대로.
            if (Settings.get().colorMode() != Settings.ColorMode.OFF) {
                for (Tetromino t : Tetromino.values()) {
                    int idx = minoTileIndex(t);
                    tiles[idx] = tint(tiles[idx], Settings.get().minoColor(t));
                }
            }

            return tiles;

        } catch (IOException e) {
            throw new IllegalStateException("미노 이미지에 문제 발생. " + path, e);
        }
    }

    /// 원본 타일의 밝기(음영)는 유지하고 색상만 target 색으로 교체
    private static BufferedImage tint(BufferedImage src, Color target) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float base = (target.getRed() * 0.299f + target.getGreen() * 0.587f + target.getBlue() * 0.114f) / 255f;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                float lum = (((p >> 16) & 255) * 0.299f + ((p >> 8) & 255) * 0.587f + (p & 255) * 0.114f) / 255f;
                float k = base == 0 ? 1f : lum / base;
                int r = Math.min(255, Math.round(target.getRed() * k));
                int gr = Math.min(255, Math.round(target.getGreen() * k));
                int b = Math.min(255, Math.round(target.getBlue() * k));
                out.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return out;
    }


    /// 이 아래의 코드는 디자인 변화 시 수정될 수 있는 코드입니다.
    /// 보드 및 미노(블록)을 png 파일로 대체하여 아래보다 코드가 간단해질 수 있습니다.


    // 보드와 쌓은 블록, 현재블록과 고스트를 그림
    private void drawBoard(Graphics2D g) {
        for (int row = Board.MIN_ROW; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                Tetromino locked = game.board.get(col, row);
                if (locked != null) drawCell(g, sx, sy, locked, false);
            }
        }

        if (!game.gameOver) {
            if (Settings.get().ghostPiece()) {
                // 현재 블록을 아래로 복사 이동해 예상 착지 위치(고스트)를 먼저 그린다.
                int ghostY = game.y;
                while (game.board.canPlace(game.active, game.x, ghostY + 1, game.rotation)) ghostY++;
                drawPiece(g, game.active, game.x, ghostY, game.rotation, true);
            }
            drawPiece(g, game.active, game.x, game.y, game.rotation, false);
        }
    }


    // 미노 만들기
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            if (by < Board.MIN_ROW || by >= Board.HEIGHT) continue;
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
        g.setColor(new Color(12, 16, 25, 220)); g.fillRoundRect(x + 15, y, 250, 112, 12, 12);
        g.setColor(Color.WHITE); g.setFont(interBlack.deriveFont(24f));
        FontMetrics fm = g.getFontMetrics(); String text = "GAME OVER";
        g.drawString(text, x + (280 - fm.stringWidth(text))/2, y + 39);
        g.setFont(interBlack.deriveFont(14f));
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        Map<TextAttribute, Float> fontAttributes = Map.of(TextAttribute.TRACKING, 0.3f);
        g.setColor(Color.WHITE);
        g.setFont(orbitBlack.deriveFont(30f).deriveFont(fontAttributes));
        String title = "PAUSE";
        g.drawString(title, 40, 70);

        int spacing = 80;
        int firstCenterY = getHeight() / 2 - (PAUSE_OPTIONS.length - 1) * spacing / 2;
        for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
            g.setColor(i == pauseSelection ? Color.WHITE : new Color(70, 70, 70));
            g.setFont((i == pauseSelection ? interBlack : interMedium).deriveFont(32f).deriveFont(fontAttributes));
            FontMetrics metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(PAUSE_OPTIONS[i])) / 2;
            int y = firstCenterY + i * spacing + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.drawString(PAUSE_OPTIONS[i], x, y);
        }
        int guideX = (getWidth() - pauseGuideImage.getWidth(this)) / 2;
        int guideY = getHeight() - pauseGuideImage.getHeight(this) - 50;
        g.drawImage(pauseGuideImage, guideX, guideY, this);
    }

    /// 일시정지 화면에서 위/아래 키로 옵션을 변경한다.
    private void bindPauseNavigation(String key, int direction) {
        KeyStroke stroke = KeyStroke.getKeyStroke("pressed " + key);
        Object gameplayBinding = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(stroke);
        Action gameplayAction = gameplayBinding == null ? null : getActionMap().get(gameplayBinding);
        String bindingName = "pause" + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, bindingName);
        getActionMap().put(bindingName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused) {
                    pauseSelection = Math.floorMod(pauseSelection + direction, PAUSE_OPTIONS.length);
                    AudioManager.get().playMenuSelect(); // 옵션 이동 효과음
                    repaint();
                } else if (gameplayAction != null) {
                    gameplayAction.actionPerformed(e);
                }
            }
        });
    }

    /// 일시정지 메뉴에서 선택한 항목을 실행한다.
    private void confirmPauseSelection() {
        AudioManager.get().playMenuSelect(); // 옵션 선택 효과음
        switch (pauseSelection) {
            case 0 -> setPaused(false); // 계속하기
            case 1 -> { // 다시 시작
                if (pauseRestartCallback != null) pauseRestartCallback.run();
                else {
                    game.restart();
                    AudioManager.get().startGameBgm();
                }
                setPaused(false);
            }
            case 2 -> homeCallback.run(); // 메인 화면으로 이동
        }
    }

    private void handlePauseInput(Settings.Action a) {
        if (a == Settings.Action.HARD_DROP) {
            ignoreSpaceUntilRelease = true;
            confirmPauseSelection();
        }
    }


    private void bindUnfiltered(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
                repaint();
            }
        });
    }

}
