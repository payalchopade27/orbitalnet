
package com.orbitalnet.core;

public class Orbit {

    private final double altitudeKm;
    private final double inclinationDeg;
    private final double periodSeconds;

    public Orbit(double altitudeKm, double inclinationDeg, double periodSeconds) {
        this.altitudeKm = altitudeKm;
        this.inclinationDeg = inclinationDeg;
        this.periodSeconds = periodSeconds;
    }

    public double getAltitudeKm() {
        return altitudeKm;
    }

    public double getInclinationDeg() {
        return inclinationDeg;
    }

    public double getPeriodSeconds() {
        return periodSeconds;
    }
}
