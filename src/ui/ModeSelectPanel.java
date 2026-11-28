package ui;

import app.ScreenManager;
import app.ScreenManager.BgmMode;
import audio.AudioManager;
import mode.GameMode;
import mode.GameModes;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import ui.TetrisBlockButton.BlockShape;
import ui.TetrisBlockButton.Position;

/** 홈 화면에서 Play를 누르면 나오는 게임 모드 선택 화면. 모드 버튼을 고르면 해당 모드를 시작하고, 아래에 설명을 보여준다. */
public final class ModeSelectPanel extends JPanel {
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private final ScreenManager screens;
    private final List<TetrisBlockButton> buttons = new ArrayList<>();
    private final MenuNavigation navigation;
    private final Font menuFont = MenuFonts.loadPressStart2P();
    private final Font koreanFont = MenuFonts.loadKRBlack();
    private TetrisBlockButton hovered, pressed;
    private String description = "";
    private final List<GameMode> modes = GameModes.all();

    /** 모드 버튼과 BACK 버튼을 배치하고, 키보드 내비게이션과 마우스 입력을 등록한다. */
    public ModeSelectPanel(ScreenManager screens) {
        this.screens = screens;
        setPreferredSize(new Dimension(1280,720)); setBackground(BACKGROUND); setOpaque(true);
        String[][] shapes={{"###",".#."},{"####"},{"###","..#"},{"###","#.."},{"##","##"}};
        Color[] colors={new Color(170,85,205),new Color(75,205,235),new Color(240,145,55),new Color(70, 95, 255),new Color(245,210,55)};
        layoutModes(shapes, colors);
        buttons.add(TetrisBlockButton.back(() -> screens.showMenuScreen(BgmMode.HOME)));
        navigation = new MenuNavigation(this, buttons, this::updateDescription, () -> screens.showMenuScreen(BgmMode.HOME));
        updateDescription();
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mouseMoved(MouseEvent e){updateHover(e.getX(),e.getY());}
            @Override public void mouseDragged(MouseEvent e){updateHover(e.getX(),e.getY());}
            @Override public void mouseExited(MouseEvent e){hovered=null;updateDescription();repaint();}
            @Override public void mousePressed(MouseEvent e){pressed=buttonAt(e.getX(),e.getY());hovered=pressed;navigation.useMouse(pressed);repaint();}
            @Override public void mouseReleased(MouseEvent e){TetrisBlockButton b=buttonAt(e.getX(),e.getY());
                                                                if(pressed!=null&&pressed==b&&b.enabled){AudioManager.get().playMenuSelect();b.action.run();}
                                                                pressed=null;updateHover(e.getX(),e.getY());}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);
    }
    /** 모드 개수에 맞춰 한 줄에 최대 3개씩 버튼을 가운데 정렬로 배치한다. 버튼 크기는 줄 수에 따라 자동으로 조절된다. */
    private void layoutModes(String[][] shapes, Color[] colors) {
        if (modes.isEmpty()) return;
        int columns = Math.min(3, modes.size());
        int rows = (modes.size() + columns - 1) / columns;
        int cellSize = Math.min(44, Math.max(12, (430 / rows - 52) / 2));
        int columnGap = 46;
        int rowGap = 22;
        int rowPitch = cellSize * 2 + rowGap + 30;
        int firstY = 190 + Math.max(0, (430 - rows * rowPitch) / 2);
        for (int row = 0; row < rows; row++) {
            int start = row * columns;
            int count = Math.min(columns, modes.size() - start);
            int pitch = cellSize * 4 + columnGap;
            int rowWidth = (count - 1) * pitch + cellSize * 4;
            int firstX = (1280 - rowWidth) / 2;
            for (int col = 0; col < count; col++) {
                int index = start + col;
                GameMode mode = modes.get(index);
                String[] shape = shapes[index % shapes.length];
                int shapeWidth = 0;
                for (String shapeRow : shape) shapeWidth = Math.max(shapeWidth, shapeRow.length());
                int x = firstX + col * pitch + (cellSize * 4 - shapeWidth * cellSize) / 2;
                int y = firstY + row * rowPitch;
                buttons.add(new TetrisBlockButton(mode.getName(), new BlockShape(colors[index % colors.length], shape),
                        new Position(x, y, cellSize), () -> { if (mode.isAvailable()) mode.start(screens); }, mode.isAvailable()));
            }
        }
    }
    /** 주어진 좌표에 있는 버튼을 반환한다. 없으면 null. */
    private TetrisBlockButton buttonAt(int x,int y){for(TetrisBlockButton b:buttons)if(b.contains(x,y))return b;return null;}
    /** 마우스 위치 아래의 버튼을 hover 상태로 바꾸고 내비게이션에 알린다. */
    private void updateHover(int x,int y){hovered=buttonAt(x,y);navigation.useMouse(hovered);}
    /** 현재 강조된 버튼에 해당하는 모드의 설명 문구로 갱신한다. */
    private void updateDescription() {
        TetrisBlockButton button = navigation.highlightedButton(hovered);
        description = "";
        if (button != null) for (GameMode mode : modes) {
            if (mode.getName().equals(button.label)) {
                description = mode.getDescription();
                break;
            }
        }
    }
    /** 제목, 모드 버튼(사용 불가 모드는 COMING SOON 표시), 모드 설명을 그린다. */
    @Override
    protected void paintComponent(Graphics graphics) {
        //1. 기본 패널 그리기
        super.paintComponent(graphics);

        //2. Graphics2D 생성 및 렌더링 설정
        Graphics2D g = (Graphics2D) graphics.create();

        // 3. 제목 그리기
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        );

        g.setColor(Color.WHITE);
        g.setFont(menuFont.deriveFont(34f));

        String title = "SELECT MODE";

        g.drawString(
                title,
                (getWidth() - g.getFontMetrics().stringWidth(title)) / 2,
                145
        );
        // 4. 모드 버튼 그리기
        for (TetrisBlockButton b : buttons) {
            int size = b.label.equals("BACK")
                    ? 14
                    : Math.min(
                    11,
                    Math.max(8, 440 / b.label.length())
            );

            b.draw(
                    g,
                    navigation.highlightedButton(hovered) == b,
                    pressed == b,
                    menuFont,
                    size
            );

            if (!b.enabled && !b.label.equals("BACK")) {
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(menuFont.deriveFont(8f));

                int width =
                        g.getFontMetrics().stringWidth("COMING SOON");

                int centerX =
                        b.x + buttonWidth(b) / 2;

                g.drawString(
                        "COMING SOON",
                        centerX - width / 2,
                        b.y + 2 * b.cellSize + 22
                );
            }
        }
        // 5. 설명 문구 그리기
        if (!description.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(koreanFont.deriveFont(24f));

            int w = g.getFontMetrics().stringWidth(description);

            g.drawString(
                    description,
                    (getWidth() - w) / 2,
                    660
            );
        }
        //6. Graphics 자원 정리
        g.dispose();
    }
    /** 버튼 모양의 가로 폭(픽셀)을 계산한다. */
    private int buttonWidth(TetrisBlockButton button){int widest=0;for(String row:button.rows)widest=Math.max(widest,row.length());return widest*button.cellSize;}
}
