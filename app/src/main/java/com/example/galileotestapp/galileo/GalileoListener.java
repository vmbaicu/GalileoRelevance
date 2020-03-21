package com.example.galileotestapp.galileo;

public interface GalileoListener {
    void onGalileoNotAvailable();

    void onGalileoAvailable(int frequency);
}
