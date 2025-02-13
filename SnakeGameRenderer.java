import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

public class SnakeGameRenderer {
    private static GLWindow window = null;
    private static SnakeGameScene3D gameScene;

    public static void init() {
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        window = GLWindow.create(capabilities);
        gameScene = new SnakeGameScene3D();

        window.addGLEventListener(gameScene);
        window.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        gameScene.moveSnake(0, 1);
                        break;
                    case KeyEvent.VK_DOWN:
                        gameScene.moveSnake(0, -1);
                        break;
                    case KeyEvent.VK_LEFT:
                        gameScene.moveSnake(-1, 0);
                        break;
                    case KeyEvent.VK_RIGHT:
                        gameScene.moveSnake(1, 0);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        window.setSize(800, 600);
        window.setVisible(true);

        FPSAnimator animator = new FPSAnimator(window, 60);
        animator.start();
    }

    public static void main(String[] args) {
        init();
    }
}
