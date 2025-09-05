package com.example.aircraftmarshalling.core;

public class TransformUtils {
    public static float[] makeRotationZColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                c,  s,  0,  0,
                -s,  c,  0,  0,
                0,  0,  1,  0,
                0,  0,  0,  1
        };
    }
    public static float[] makeRotationYColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                c,  0, -s, 0,
                0,  1,  0, 0,
                s,  0,  c, 0,
                0,  0,  0, 1
        };
    }
    public static float[] makeRotationXColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                1,  0,  0, 0,
                0,  c,  s, 0,
                0, -s,  c, 0,
                0,  0,  0, 1
        };
    }
    public static float[] mulCM(float[] a, float[] b) {
        float[] o = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                o[col*4 + row] =
                        a[0*4 + row] * b[col*4 + 0] +
                        a[1*4 + row] * b[col*4 + 1] +
                        a[2*4 + row] * b[col*4 + 2] +
                        a[3*4 + row] * b[col*4 + 3];
            }
        }
        return o;
    }
    public static float[] createTransform(float scale, float angleX, float angleY, float angleZ) {
        double radX = Math.toRadians(angleX);
        double radY = Math.toRadians(angleY);
        double radZ = Math.toRadians(angleZ);
        float cx = (float) Math.cos(radX);
        float sx = (float) Math.sin(radX);
        float cy = (float) Math.cos(radY);
        float sy = (float) Math.sin(radY);
        float cz = (float) Math.cos(radZ);
        float sz = (float) Math.sin(radZ);
        float[] m = new float[16];
        m[0] = cy * cz * scale;
        m[1] = (sx * sy * cz - cx * sz) * scale;
        m[2] = (cx * sy * cz + sx * sz) * scale;
        m[3] = 0;
        m[4] = cy * sz * scale;
        m[5] = (sx * sy * sz + cx * cz) * scale;
        m[6] = (cx * sy * sz - sx * cz) * scale;
        m[7] = 0;
        m[8] = -sy * scale;
        m[9] = sx * cy * scale;
        m[10] = cx * cy * scale;
        m[11] = 0;
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = 1;
        return m;
    }
}