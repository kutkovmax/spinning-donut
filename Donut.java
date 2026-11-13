import java.util.Arrays;

public class Donut {

    private static final double TUBE_RADIUS = 0.9;
    private static final double RING_RADIUS = 2.5;
    private static final int TUBE_SEGMENTS = 90;
    private static final int RING_SEGMENTS = 180;

    private static final int SCREEN_WIDTH_PX = 64;
    private static final int SCREEN_HEIGHT_PX = 32;
    private static final int FRAME_DURATION_MS = 30;
    private static final double ASPECT_RATIO = (double) SCREEN_WIDTH_PX / SCREEN_HEIGHT_PX * 0.5;

    private static final double CAMERA_X = 0.0;
    private static final double CAMERA_Y = 0.0;
    private static final double CAMERA_Z = 5.0;
    private static final double HALF_FOV = Math.toRadians(45.0);
    private static final double NEAR_PLANE = 0.1;
    private static final double FAR_PLANE = 100.0;

    private static final String LUMINANCE = ".,-~:;=!*#$@";
    private static final double[] LIGHT_DIR = normalize(new double[]{0.3, 0.5, 0.8});

    private final char[][] screen = new char[SCREEN_HEIGHT_PX][SCREEN_WIDTH_PX];
    private final double[][] zBuffer = new double[SCREEN_HEIGHT_PX][SCREEN_WIDTH_PX];
    private final StringBuilder frameBuffer = new StringBuilder(SCREEN_HEIGHT_PX * (SCREEN_WIDTH_PX + 1) + 4);

    private final double[][] viewProjMatrix = multiply(getProjectionMatrix(), getViewMatrix());

    private double angleX = 0;
    private double angleY = 0; 
    private double angleZ = 0;
    private double time = 0.0;



    public static void main(String[] args) {
        Donut myDonut = new Donut();
        myDonut.drawDonut();
    }

    private void drawDonut(){
        System.out.print("\033[H\033[2J");
        System.out.flush();

        double stepA = 2 * Math.PI / TUBE_SEGMENTS;
        double stepB = 2 * Math.PI / RING_SEGMENTS;

        while (true){
            clearBuffers();
            updateAngles();
            
            double[][] modelMatrix = getModelMatrix(angleX, angleY, angleZ);
            double[][] mvpMatrix = multiply(viewProjMatrix, modelMatrix);

            for (int i = 0; i < TUBE_SEGMENTS; i++) {
                double a = i * stepA;
                for (int j = 0; j < RING_SEGMENTS; j++) {
                    double b = j * stepB;

                    double[] point = getTorusPoint(a,b);
                    double[] transformed = multiply(mvpMatrix, point);

                    double w = transformed[3];
                    if (w <= 0) {
                        continue;
                    }

                    double x = transformed[0] / w;
                    double y = transformed[1] / w;
                    double z = transformed[2] / w;

                    int screenX = (int) ((x + 1) * 0.5 * SCREEN_WIDTH_PX);
                    int screenY = (int) ((1 - y) * 0.5 * SCREEN_HEIGHT_PX);

                    if (screenX >= 0 && screenX < SCREEN_WIDTH_PX &&
                        screenY >= 0 && screenY < SCREEN_HEIGHT_PX) {
                        if (z < zBuffer[screenY][screenX]) {
                            zBuffer[screenY][screenX] = z;

                            double[] localNormal = getTorusNormal(a, b);
                            double[] worldNormal = multiply(modelMatrix, localNormal);

                            double lum = Math.max(0, dot(worldNormal, LIGHT_DIR));
                            int idx = (int)(lum * (LUMINANCE.length() - 1));
                            screen[screenY][screenX] = LUMINANCE.charAt(idx);
                        }
                    }

                }
            }

            drawScreen();

            try {
                Thread.sleep(FRAME_DURATION_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private double[] getTorusPoint(double a, double b) {
        return new double[]{
            (RING_RADIUS + TUBE_RADIUS * Math.cos(a)) * Math.cos(b),
            (RING_RADIUS + TUBE_RADIUS * Math.cos(a)) * Math.sin(b),
            TUBE_RADIUS  * Math.sin(a),
            1
        };
    }

    private void drawScreen() {
        frameBuffer.setLength(0);
        frameBuffer.append("\033[H");
        for (char[] row : screen) {
            frameBuffer.append(row).append('\n');
        }
        System.out.print(frameBuffer);
    }

    private void updateAngles() {
        time += 0.04;
        angleY += 0.06;                        // полноценно крутится
        angleX = 0.45 * Math.sin(time * 0.7);  // плавно кивает вперед-назад
    }

    private double[][] getModelMatrix(double angleX, double angleY, double angleZ) {
        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);
        double cosZ = Math.cos(angleZ), sinZ = Math.sin(angleZ);

        double[][] rotX = {
            {1,     0,      0, 0},
            {0,  cosX,  -sinX, 0},
            {0,  sinX,   cosX, 0},
            {0,     0,      0, 1}
        };

        double[][] rotY = {
            { cosY, 0, sinY, 0},
            {    0, 1,    0, 0},
            {-sinY, 0, cosY, 0},
            {    0, 0,    0, 1}
        };

        double[][] rotZ = {
            {cosZ, -sinZ, 0, 0},
            {sinZ,  cosZ, 0, 0},
            {   0,     0, 1, 0},
            {   0,     0, 0, 1}
        };

        double[][] rotXY = multiply(rotY, rotX);
        return multiply(rotZ, rotXY);
    }

    private double[][] getViewMatrix() {
        return new double[][]{
            {1, 0, 0, -CAMERA_X},
            {0, 1, 0, -CAMERA_Y},
            {0, 0, 1, -CAMERA_Z},
            {0, 0, 0,  1}
        };
    }

    // матрица перспективной проекции
    private double[][] getProjectionMatrix(){
        double scale = 1.0 / Math.tan(HALF_FOV);

        return new double[][]{
            {scale / ASPECT_RATIO, 0, 0, 0},
            {0, scale, 0, 0},
            {0, 0, -(FAR_PLANE+NEAR_PLANE)/(FAR_PLANE-NEAR_PLANE), -2*(FAR_PLANE*NEAR_PLANE)/(FAR_PLANE-NEAR_PLANE)},
            {0, 0, -1, 0},
        };
    }

    private double[] multiply(double[][] matrix, double[] vector){
        double[] result = new double[4];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j < 4; j++){
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    private double[][] multiply(double[][] a, double[][] b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    private static double dot(double[] u, double[] v) {
        return u[0]*v[0] + u[1]*v[1] + u[2]*v[2];
    }

    private double[] getTorusNormal(double a, double b) {
        return new double[]{
            Math.cos(a) * Math.cos(b),
            Math.cos(a) * Math.sin(b),
            Math.sin(a),
            0
        };
    }

    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }

    private void clearBuffers() {
        for (char[] row : screen) {
            Arrays.fill(row, ' ');
        }
        for (double[] row : zBuffer) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
    }
    
}
