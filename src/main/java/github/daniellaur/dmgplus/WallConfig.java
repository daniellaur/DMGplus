package github.daniellaur.dmgplus;

public class WallConfig {
    public boolean knockbackEnabled   = true;
    public double pushBack            = 1.2;
    public double verticalBoost       = 0.1;
    public int    cooldownTicks       = 8;

    public double inflateX            = 0.00;
    public double inflateY            = -0.2;
    public double inflateZ            = -0.1;

    public boolean lagCompensation    = true;
    public int     maxCompensationTicks = 20;
    public double  minPenetration     = 0.3;
    public double  epsilon            = 0.04;
    public double  resolveAheadFactor = 1.0;
    public double  maxDxPerTick       = 0.8;
    public boolean suppressRubberband  = true;
    public double  suppressRadius      = 20.0;
}
