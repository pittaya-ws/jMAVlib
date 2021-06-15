package me.drton.jmavlib.conversion;

import javax.vecmath.Vector3d;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * User: ton Date: 02.06.13 Time: 20:20
 */
public class RotationConversion {
    public static double[] rotationMatrixByEulerAngles(double roll, double pitch, double yaw) {
        return new double[] {
                   cos(pitch) * cos(yaw),
                   sin(roll) * sin(pitch) * cos(yaw) - cos(roll) * sin(yaw),
                   cos(roll) * sin(pitch) * cos(yaw) + sin(roll) * sin(yaw),
                   cos(pitch) * sin(yaw),
                   sin(roll) * sin(pitch) * sin(yaw) + cos(roll) * cos(yaw),
                   cos(roll) * sin(pitch) * sin(yaw) - sin(roll) * cos(yaw),
                   -sin(pitch),
                   sin(roll) * cos(pitch),
                   cos(roll) * cos(pitch)
               };
    }

    public static double[] rotationMatrixByQuaternion(double[] q) {
        double a = q[0];
        double b = q[1];
        double c = q[2];
        double d = q[3];
        double aa = a * a;
        double ab = a * b;
        double ac = a * c;
        double ad = a * d;
        double bb = b * b;
        double bc = b * c;
        double bd = b * d;
        double cc = c * c;
        double cd = c * d;
        double dd = d * d;
        
        return new double[] {
                    aa + bb - cc - dd,
                    2 * (bc - ad),
                    2 * (ac + bd),
                    2 * (bc + ad),
                    aa - bb + cc - dd,
                    2 * (cd - ab),
                    2 * (bd - ac),
                    2 * (ab + cd),
                    aa - bb - cc + dd
        };
    }

    public static double[] rotationMatrixRotate(double[] R) {
        // Pitch up 90 degree for fixed-wing mode tailsitter
        return new double[] {
            -R[2], R[1], R[0],
            -R[5], R[4], R[3],
            -R[8], R[7], R[6],
            
        };
    }

    public static double[] eulerAnglesByQuaternion(double[] q) {
        return new double[] {
                   Math.atan2(2.0 * (q[0] * q[1] + q[2] * q[3]), 1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2])),
                   Math.asin(2 * (q[0] * q[2] - q[3] * q[1])),
                   Math.atan2(2.0 * (q[0] * q[3] + q[1] * q[2]), 1.0 - 2.0 * (q[2] * q[2] + q[3] * q[3])),
               };
    }

    public static double[] eulerAnglesByQuaternionRotate(double[] q) {
        double[] R = rotationMatrixByQuaternion(q);
        double[] R2 = rotationMatrixRotate(R);
        double[] R3 = eulerAnglesByrotationMatrix(R2);

        return R3;
    }

    public static double[] eulerAnglesByrotationMatrix(double[] R) {
        double phi = Math.atan2(R[7], R[8]);
        double theta = Math.asin(-R[6]);
        double psi = Math.atan2(R[3], R[0]);

        if ((Math.abs(theta - Math.PI / 2)) < 1e-3) {
            
            phi = 0;
            psi = Math.atan2(R[5], R[2]);
        }
        else if ((Math.abs(theta + Math.PI / 2)) < 1e-3) {
            
            phi = 0;
            psi = Math.atan2(-R[5], -R[2]);
        }

        return new double[] {phi, theta, psi};
    }

    public static Float[] quaternionByEulerAngles(Vector3d euler) {
        double cosPhi_2 = Math.cos(euler.x / 2.0);
        double cosTheta_2 = Math.cos(euler.y / 2.0);
        double cosPsi_2 = Math.cos(euler.z / 2.0);
        double sinPhi_2 = Math.sin(euler.x / 2.0);
        double sinTheta_2 = Math.sin(euler.y / 2.0);
        double sinPsi_2 = Math.sin(euler.z / 2.0);
        return new Float[] {
                   (float)(cosPhi_2 * cosTheta_2 * cosPsi_2 +
                           sinPhi_2 * sinTheta_2 * sinPsi_2),
                   (float)(sinPhi_2 * cosTheta_2 * cosPsi_2 -
                           cosPhi_2 * sinTheta_2 * sinPsi_2),
                   (float)(cosPhi_2 * sinTheta_2 * cosPsi_2 +
                           sinPhi_2 * cosTheta_2 * sinPsi_2),
                   (float)(cosPhi_2 * cosTheta_2 * sinPsi_2 -
                           sinPhi_2 * sinTheta_2 * cosPsi_2)
               };
    }

}
