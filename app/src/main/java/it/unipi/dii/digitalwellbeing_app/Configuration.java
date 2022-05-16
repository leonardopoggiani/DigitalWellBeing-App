package it.unipi.dii.digitalwellbeing_app;

public class Configuration {

    // TODO Range values for accelerometer in pocket

    static final int HIGH_SAMPLING_RATE = 33330;
    static final int LOW_SAMPLING_RATE = 10000;

    static final double X_LOWER_BOUND_POCKET = -5.0;
    static final double X_UPPER_BOUND_POCKET = 5.0;
    static final double Y_LOWER_BOUND_POCKET = -5.0;
    static final double Y_UPPER_BOUND_POCKET = 5.0;
    static final double Z_LOWER_BOUND_POCKET = 0.0;
    static final double Z_UPPER_BOUND_POCKET = 12.0;

    //Telefono in tasca sottosopra display verso la gamba
    //Forse sbagliati
    static final double X_LOWER_BOUND_POCKET_0 = 0.4;
    static final double X_UPPER_BOUND_POCKET_0 = 0.7;
    static final double Y_LOWER_BOUND_POCKET_0 = -9.6;
    static final double Y_UPPER_BOUND_POCKET_0 = -9.5;
    static final double Z_LOWER_BOUND_POCKET_0 = -1.1;
    static final double Z_UPPER_BOUND_POCKET_0 = -1.1;

    //Telefono in tasca sottosopra display verso l'esterno
    //Forse sbagliati
    static final double X_LOWER_BOUND_POCKET_1 = -0.08;
    static final double X_UPPER_BOUND_POCKET_1 = -0.05;
    static final double Y_LOWER_BOUND_POCKET_1 = -11;
    static final double Y_UPPER_BOUND_POCKET_1 = -8;
    static final double Z_LOWER_BOUND_POCKET_1 = 0.5;
    static final double Z_UPPER_BOUND_POCKET_1 = 1.5;

}