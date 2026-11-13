import java.util.Arrays;

public class Donut {


    private final double R1 = 1; // радиус образующей окружности
    private final  double R2 = 2; // расстояние до оси вращения 

    private final int WINDOW_WIDTH_PX = 64;
    private final int WINDOW_HEIGHT_PX = 32;
    private final double AR = (double) WINDOW_WIDTH_PX / WINDOW_HEIGHT_PX;
    private final double HALF_FOV = Math.toRadians(90.0 / 2.0);
    


    public static void main(String[] args) {
        Donut myDonut = new Donut();
        myDonut.drawDonut();
    }

    private void drawDonut(){
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        char[][] screen = new char[WINDOW_HEIGHT_PX][WINDOW_WIDTH_PX];
        for (char[] row : screen) {
            Arrays.fill(row, ' ');
        }

        for (double a = 0; a < 2 * Math.PI ; a+= 0.1){
            for (double b = 0; b < 2 * Math.PI; b += 0.1){
                double[] point = getTorusPoint(a,b);
                double[] transformed = multiply(getModelMatrix(), point);
                transformed = multiply(getViewMatrix(), transformed);
                transformed = multiply(getProjectionMatrix(), transformed);

                double w = transformed[3];
                double x = transformed[0] /= w;
                double y = transformed[1] /= w;
                double z = transformed[2] /= w;

                int screenX = (int) ((x + 1) * 0.5 * WINDOW_WIDTH_PX);
                int screenY = (int) ((1 - y) * 0.5 * WINDOW_HEIGHT_PX);

                if (screenX >= 0 && screenX < WINDOW_WIDTH_PX &&
                    screenY >= 0 && screenY < WINDOW_HEIGHT_PX) {
                    screen[screenY][screenX] = '#';
                }

            }
        }

        System.out.print("\033[H\033[2J");

        for (char[] row : screen) {
            System.out.println(row);
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

    private double[][] getModelMatrix(){
        return new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, -5},
            {0, 0, 0, 1},
        };
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
    
}
