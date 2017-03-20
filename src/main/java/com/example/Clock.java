package com.example;

/**
 * Using a wrapper clock that simply returns
 * the current time in milliseconds such that
 * mocking the time will be possible and using
 * sleep mechanisms will be avoided. This way
 * tests run time will remain reasonable.
 *
 * I did this in order to avoid importing
 * another library that allows me to mock
 * private members.
 */
public interface Clock {

    long currentTimeMillis();
}
