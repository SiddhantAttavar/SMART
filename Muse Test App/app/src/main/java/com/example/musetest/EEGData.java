package com.example.musetest;

public class EEGData {
    public int id;
    public long time;
    public double EEG, alpha, beta, theta, delta, DAR, DTR;

    EEGData(int id,
            long time,
            double EEG,
            double alpha,
            double beta,
            double theta,
            double delta,
            double DAR,
            double DTR) {
        this.id = id;
        this.time = time;
        this.EEG = EEG;
        this.alpha = alpha;
        this.beta = beta;
        this.theta = theta;
        this.delta = delta;
        this.DAR = DAR;
        this.DTR = DTR;
    }
}
