package ui;

import audio.AudioManager;
import game.GameListener;
import game.Tetromino;

/** 게임 이벤트에 맞춰 효과음을 재생하는 리스너. 1인용 화면과 대전 화면이 함께 쓴다. */
public final class GameAudioListener implements GameListener {
    private boolean justHardDropped;

    /** 블록이 이동하면 이동 효과음을 재생한다. */
    @Override public void onMove(int dx, int dy) { AudioManager.get().playMove(); }
    /** 블록이 회전하면 회전 효과음을 재생한다. */
    @Override public void onRotate(int direction) { AudioManager.get().playRotate(); }
    /** 소프트 드롭 시 효과음을 재생한다. */
    @Override public void onSoftDrop() { AudioManager.get().playSoftDrop(); }
    /** 하드 드롭 효과음을 재생하고, 직후 고정 효과음이 겹치지 않도록 표시해 둔다. */
    @Override public void onHardDrop() {
        justHardDropped = true;
        AudioManager.get().playHardDrop();
    }
    /** 홀드 시 효과음을 재생한다. */
    @Override public void onHold() { AudioManager.get().playHold(); }
    /** 블록이 고정되면 고정 효과음을 재생한다. 하드 드롭 직후라면 생략한다. */
    @Override public void onLock(Tetromino type) {
        if (justHardDropped) justHardDropped = false;
        else AudioManager.get().playLock();
    }
    /** 줄을 지우면 지운 줄 수에 맞는 효과음을 재생한다. */
    @Override public void onLinesCleared(int lines) { AudioManager.get().playLineClear(lines); }
    /** 레벨이 오르면 레벨업 효과음을 약간 늦게 재생한다. */
    @Override public void onLevelUp(int newLevel) { AudioManager.get().playLevelUpDelayed(); }
    /** 목표(스프린트 줄 수 등)를 달성하면 클리어 효과음을 재생한다. */
    @Override public void onGoalReached() { AudioManager.get().playTutorialClear(); }
    /** 게임 오버 시 게임 오버 효과음을 재생한다. */
    @Override public void onGameOver() { AudioManager.get().playGameOver(); }
}