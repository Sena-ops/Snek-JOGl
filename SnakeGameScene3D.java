import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.gl2.GLUT;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SnakeGameScene3D implements GLEventListener {
    private GLU glu;
    private GLUT glut;
    private List<float[]> snake; // Lista para armazenar as posições dos segmentos da cobra
    private float[] apple; // Posição da maçã
    private float snakeDirection[] = {1.0f, 0.0f}; // Direção inicial da cobra
    private float snakeSpeed = 0.1f; // Velocidade da cobra
    private float snakeX = 0.0f, snakeY = 0.0f; // Posição inicial da cobra
    private boolean gameOver = false;
    private float appleSize = 0.2f;
    private final float MAX_X = 3.5f, MAX_Y = 3.5f, MIN_X = -3.5f, MIN_Y = -3.5f;
    private Texture backFaceTexture; // Textura para a parte traseira do cubo
    private int score = 0; // Contador de pontos
    private int highScore = 0; // Recorde
    private float appleYPosition = 0.0f;  // Posição inicial da maçã no eixo Y
    private float floatSpeed = 0.1f;  // Velocidade da flutuação
    private float floatAmplitude = 0.05f;  // Amplitude da flutuação
    private float time = 0.0f;  // Controla o tempo da animação de flutuação


    public SnakeGameScene3D() {
        glu = new GLU();
        glut = new GLUT();
        snake = new ArrayList<>();
        snake.add(new float[]{0.0f, 0.0f}); // Posição inicial da cobra
        spawnApple(); // Gera uma posição inicial para a maçã
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f); // Cor do fundo
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

        setupLighting(gl);
        loadTexture(gl);
    }

    private void setupLighting(GL2 gl) {
        float[] lightPosition = {0.0f, 0.0f, 8.0f, 1.0f};
        float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] lightSpecular = {0.8f, 0.8f, 0.8f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);
    }

    private void loadTexture(GL2 gl) {
        try {
            // Altere o caminho para o seu arquivo
            InputStream stream = getClass().getResourceAsStream("piso.jpg");
            if (stream == null) {
                throw new IOException("Texture file not found");
            }


            TextureData data = TextureIO.newTextureData(gl.getGLProfile(), stream, false, "jpg");
            backFaceTexture = TextureIO.newTexture(data);
        } catch (IOException e) {
            System.err.println("Error loading texture: " + e.getMessage());
            backFaceTexture = null; // Garante que não tente usar textura inválida
        }
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Limpa a tela e o buffer de profundidade
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // Define a câmera para uma visão mais de lado e melhor perspectiva
        glu.gluLookAt(0.0, 0.0, 10.0,   // Posição da câmera
                0.0, 0.0, 0.0,    // Onde a câmera está olhando
                0.0, 1.0, 0.0);   // Orientação da câmera

        // Desenha a caixa
        drawBoundaryBox(gl);

        // Atualiza a posição da maçã
        updateApplePosition();

        // Renderização 3D
        drawApple(gl, appleYPosition);  // A maçã é desenhada usando a posição Y atualizada

        // Desenha a cobra
        drawSnake(gl);

        // Desenha o contador de pontos e recorde
        drawScore(gl);

        // Atualiza a posição da cobra
        if (!gameOver) {
            updateSnake();
        }

        // Verifica se a cobra comeu a maçã
        checkAppleCollision();

        // Verifica se a cobra bateu na parede
        checkBoundaryCollision();

        gl.glFlush();
    }



    private void drawBoundaryBox(GL2 gl) {
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.0f, -4.0f);
        gl.glColor3f(0.6f, 0.8f, 0.2f); // Verde claro

        if (backFaceTexture != null) {
            backFaceTexture.enable(gl);
            backFaceTexture.bind(gl);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL);

            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-7.5f, -7.5f, -7.5f);
            gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f(7.5f, -7.5f, -7.5f);
            gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f(7.5f, 7.5f, -7.5f);
            gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-7.5f, 7.5f, -7.5f);
            gl.glEnd();

            backFaceTexture.disable(gl);
        } else {

            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-4.5f, -4.5f, -4.5f);
            gl.glVertex3f(4.5f, -4.5f, -4.5f);
            gl.glVertex3f(4.5f, 4.5f, -4.5f);
            gl.glVertex3f(-4.5f, 4.5f, -4.5f);
            gl.glEnd();
        }

        gl.glPopMatrix();
    }

    private void drawApple(GL2 gl, float appleYPosition) {
        // Atualiza a posição da maçã
        updateApplePosition();

        gl.glPushMatrix();
        gl.glTranslatef(apple[0], apple[1] + this.appleYPosition, 0.0f); // Aplica a flutuação no eixo Y

        // Desenha a maçã
        gl.glColor3f(1.0f, 0.0f, 0.0f); // Cor vermelha para a maçã
        glut.glutSolidSphere(appleSize, 20, 20); // Maçã representada por uma esfera

        // Desenha o caule da maçã
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.3f, appleSize * 0.5f); // Posiciona o caule no topo da maçã (ao longo do eixo Y positivo)
        gl.glRotatef(90, 1.0f, 0.0f, 0.0f); // Rotaciona para que o caule fique na direção correta
        gl.glColor3f(0.5f, 0.25f, 0.1f); // Cor marrom para o caule
        glut.glutSolidCylinder(0.05f, 0.2f, 10, 10); // Caule
        gl.glPopMatrix();

        // Desenha a folha da maçã
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.4f, appleSize * 0.8f); // Posiciona a folha acima do caule (ao longo do eixo Y positivo)
        gl.glRotatef(180, 0.0f, 1.0f, 0.0f);
        gl.glColor3f(0.0f, 1.0f, 0.0f); // Cor verde para a folha
        gl.glBegin(GL2.GL_QUADS); // Desenhando a folha como um pequeno quadrado

        // Definindo a forma da folha
        gl.glVertex3f(-0.1f, 0.0f, 0.0f);
        gl.glVertex3f(0.1f, 0.0f, 0.0f);
        gl.glVertex3f(0.0f, 0.2f, 0.0f);
        gl.glVertex3f(0.0f, -0.2f, 0.0f);

        gl.glEnd();
        gl.glPopMatrix();

        gl.glPopMatrix();
    }

    // Método para atualizar a posição Y da maçã
    private void updateApplePosition() {
        time += floatSpeed;  // Incrementa o tempo
        appleYPosition = (float) (Math.sin(time) * floatAmplitude);  // Calcula a posição Y usando a função seno
    }


    private void drawSnake(GL2 gl) {

        float[] materialAmbient = {0.1f, 0.5f, 0.1f, 1.0f}; // Reflexão ambiente
        float[] materialDiffuse = {0.2f, 0.8f, 0.2f, 1.0f}; // Reflexão difusa
        float[] materialSpecular = {0.5f, 0.9f, 0.5f, 1.0f}; // Reflexão especular
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, materialAmbient, 0);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, materialDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, materialSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 50.0f); // Brilho

        for (int i = 0; i < snake.size(); i++) {
            float[] segment = snake.get(i);

            gl.glPushMatrix();
            gl.glTranslatef(segment[0], segment[1], 0.0f);

            if (i == 0) {
                // Cabeça da cobra
                gl.glScalef(1.2f, 0.8f, 0.8f); // Ajuste de escala para deixar levemente oval
                gl.glColor3f(0.0f, 0.6f, 0.0f); // Cor diferenciada para a cabeça
                GLUquadric head = glu.gluNewQuadric();
                glu.gluSphere(head, 0.25f, 16, 16);
                glu.gluDeleteQuadric(head);


                drawEyes(gl);

            } else {

                gl.glColor3f(0.0f, 0.8f, 0.0f); // Cor do corpo
                GLUquadric body = glu.gluNewQuadric();
                glu.gluSphere(body, 0.2f, 16, 16); // Segmentos do corpo
                glu.gluDeleteQuadric(body);
            }
            gl.glPopMatrix();
        }
    }



    private void drawEyes(GL2 gl) {
        gl.glPushMatrix();


        gl.glTranslatef(-0.2f, 0.15f, 0.35f); // Ajustar posição do olho
        gl.glColor3f(0.0f, 0.0f, 0.0f); // Branco
        GLUquadric eye1 = glu.gluNewQuadric();
        glu.gluSphere(eye1, 0.05, 8, 8); // Olho pequeno
        glu.gluDeleteQuadric(eye1);

        gl.glPopMatrix();
        gl.glPushMatrix();


        gl.glTranslatef(0.2f, 0.15f, 0.35f); // Ajustar posição do olho
        gl.glColor3f(0.0f, 0.0f, 0.0f); // Branco
        GLUquadric eye2 = glu.gluNewQuadric();
        glu.gluSphere(eye2, 0.05, 8, 8); // Olho pequeno
        glu.gluDeleteQuadric(eye2);

        gl.glPopMatrix();
    }

    private void drawScore(GL2 gl) {
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -0.1f); // Posição no canto superior esquerdo


        gl.glRotatef(90, 1.0f, 0.0f, 0.0f); // Rotação no eixo X
        gl.glRotatef(90, 0.0f, 1.0f, 0.0f); // Rotação no eixo Y


        gl.glColor3f(1.0f, 1.0f, 1.0f); // Cor branca para o texto
        String scoreText = "Score: " + score;
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, scoreText);


        gl.glTranslatef(0.0f, -0.2f, 0.0f); // Desce um pouco para o recorde
        String highScoreText = "High Score: " + highScore;
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, highScoreText);

        gl.glPopMatrix();
    }


    private void updateSnake() {
        List<float[]> newSnake = new ArrayList<>();
        newSnake.add(new float[]{snakeX, snakeY});
        for (int i = 0; i < snake.size() - 1; i++) {
            newSnake.add(snake.get(i));
        }
        snakeX += snakeDirection[0] * snakeSpeed;
        snakeY += snakeDirection[1] * snakeSpeed;
        snake = newSnake;
    }

    private void spawnApple() {
        apple = new float[]{(float) (Math.random() * 6 - 3), (float) (Math.random() * 6 - 3)};
    }

    private void checkAppleCollision() {
        // Verifica se a cobra colidiu com a maçã
        if (Math.abs(snakeX - apple[0]) < appleSize && Math.abs(snakeY - apple[1]) < appleSize) {
            // Adiciona um novo segmento à cauda da cobra
            snake.add(new float[]{snakeX, snakeY});
            spawnApple(); // Gera uma nova maçã

            // Aumenta a pontuação
            score++;

            // Atualiza o recorde
            if (score > highScore) {
                highScore = score;
            }

            // Aumenta a velocidade da cobra
            snakeSpeed += 0.01f; // Aumenta a velocidade um pouco a cada maçã
        }
    }


    private void checkBoundaryCollision() {
        if (snakeX >= MAX_X || snakeX <= MIN_X || snakeY >= MAX_Y || snakeY <= MIN_Y) {
            resetGame();
        }
    }

    private void resetGame() {
        snake.clear();
        snake.add(new float[]{0.0f, 0.0f});
        snakeX = 0.0f;
        snakeY = 0.0f;
        snakeSpeed = 0.1f;
        spawnApple();
        gameOver = false;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0) height = 1;
        float aspect = (float) width / height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, aspect, 0.1, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {}

    public void moveSnake(float x, float y) {
        snakeDirection[0] = x;
        snakeDirection[1] = y;
    }

    public void startGame() {
        snake.clear();
        snake.add(new float[]{0.0f, 0.0f});
        snakeX = 0.0f;
        snakeY = 0.0f;
        spawnApple();
        gameOver = false;
    }
}
