package dev.vepo.youtube.creator.project;

public class ColorGrade {
    private double lift;
    private double gamma = 1.0;
    private double gain = 1.0;
    private double saturation = 1.0;
    private double hue;
    private String lutPath;

    public double getLift() {
        return lift;
    }

    public void setLift(double lift) {
        this.lift = lift;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public double getSaturation() {
        return saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getHue() {
        return hue;
    }

    public void setHue(double hue) {
        this.hue = hue;
    }

    public String getLutPath() {
        return lutPath;
    }

    public void setLutPath(String lutPath) {
        this.lutPath = lutPath;
    }
}
