import java.util.Arrays;

public class Donut {


    private static final double R1 = 0.4; // радиус образующей окружности
    private static final  double R2 = 2.5; // расстояние до оси вращения 
    private static final int WINDOW_WIDTH_PX = 64;
    private static final int WINDOW_HEIGHT_PX = 32;
    private static final double AR = (double) WINDOW_WIDTH_PX / WINDOW_HEIGHT_PX * 0.5;
    private static final double HALF_FOV = Math.toRadians(90.0 / 2.0);
    private static final String LUMINANCE = ".,-~:;=!*#$@";   
    private static final double[] LIGHT_DIR = normalize(new double[]{0.3, 0.5, -0.8});



    public static void main(String[] args) {
        Donut myDonut = new Donut();
        myDonut.drawDonut();
    }

    private void drawDonut(){
        System.out.print("\033[H\033[2J");
        System.out.flush();

        double angleX = 1;
        double angleY = 0; 
        double angleZ = 0;

        double[][] viewProjMatrix = multiply(getProjectionMatrix(), getViewMatrix());

        while (true){
       
            char[][] screen = new char[WINDOW_HEIGHT_PX][WINDOW_WIDTH_PX];
            double[][] zBuffer = new double[WINDOW_HEIGHT_PX][WINDOW_WIDTH_PX];
            for (char[] row : screen){
                Arrays.fill(row, ' ');
            }
            for (double[] row : zBuffer){
                Arrays.fill(row, Double.POSITIVE_INFINITY);
            }
            
            double[][] modelMatrix = getModelMatrix(angleX, angleY, angleZ);
            double[][] mvpMatrix = multiply(viewProjMatrix, modelMatrix);

            for (double a = 0; a < 2 * Math.PI ; a+= 0.1){
                for (double b = 0; b < 2 * Math.PI; b += 0.1){
                    double[] point = getTorusPoint(a,b);
                    double[] transformed = multiply(mvpMatrix, point);

                    double w = transformed[3];
                    double x = transformed[0] /= w;
                    double y = transformed[1] /= w;
                    double z = transformed[2] /= w;

                    int screenX = (int) ((x + 1) * 0.5 * WINDOW_WIDTH_PX);
                    int screenY = (int) ((1 - y) * 0.5 * WINDOW_HEIGHT_PX);

                    if (screenX >= 0 && screenX < WINDOW_WIDTH_PX &&
                        screenY >= 0 && screenY < WINDOW_HEIGHT_PX) {
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

            System.out.print("\033[H");

            for (char[] row : screen) {
                System.out.println(row);
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // angleX += 0.01;
            angleY += 0.09;
            angleZ += 0.00;
        }
    }

    private double[] getTorusPoint(double a, double b) {
        return new double[]{
            (R2 + R1 * Math.cos(a)) * Math.cos(b),
            (R2 + R1 * Math.cos(a)) * Math.sin(b),
            R1 * Math.sin(a),
            1
        };
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

        double[][] translate = {
            {1, 0, 0,  0},
            {0, 1, 0,  0},
            {0, 0, 1, -5},
            {0, 0, 0,  1}
        };

        double[][] rotXY  = multiply(rotY, rotX);
        double[][] rotXYZ = multiply(rotZ, rotXY);

        return multiply(translate, rotXYZ);
    }

    private double[][] getViewMatrix(){
        return new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
        };
    }

    // матрица перспективной проекции
    private double[][] getProjectionMatrix(){
        double scale = 1.0 / Math.tan(HALF_FOV);
        double n = 0.1;
        double f = 100.0;

        return new double[][]{
            {scale / AR, 0, 0, 0},
            {0, scale, 0, 0},
            {0, 0, -(f+n)/(f-n), -2*(f*n)/(f-n)},
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

    private static double dot(double[] u, double[] v) {
        return u[0]*v[0] + u[1]*v[1] + u[2]*v[2];
    }
    
}
