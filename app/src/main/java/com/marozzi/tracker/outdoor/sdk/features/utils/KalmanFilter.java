package com.marozzi.tracker.outdoor.sdk.features.utils;

import android.location.Location;
import android.os.SystemClock;

import java.util.ArrayList;

/**
 * Created by amarozzi on 2020-07-19
 */
public class KalmanFilter  {

    // Static constant
    /**
     * Provider string assigned to predicted Location objects.
     */

    private static final double DEG_TO_METER = 111225.0;
    private static final double METER_TO_DEG = 1.0 / DEG_TO_METER;

    private static final double TIME_STEP = 1.0;
    private static final double COORDINATE_NOISE = 4.0 * METER_TO_DEG;
    private static final double ALTITUDE_NOISE = 2.0;

    private Location mLastLocation;

    private ArrayList<Double> mAltitudeList;

    /**
     * Three 1-dimension trackers, since the dimensions are independent and can avoid using matrices.
     */
    private Tracker1D mLatitudeTracker, mLongitudeTracker, mAltitudeTracker;

    public KalmanFilter () {
        mAltitudeList = new ArrayList<Double>();
    }

    public boolean isFirstPoint(){
        return mLastLocation == null;
    }

    public Location filterData(Location location) {

        // Reusable
        final double accuracy = location.getAccuracy();
        double position, noise;
        Location noFilteredLocation = new Location(location);

        // Latitude
        position = location.getLatitude();
        noise = accuracy * METER_TO_DEG;

        if (mLatitudeTracker == null) {

            mLatitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
            mLatitudeTracker.setState(position, 0.0, noise);
        }

        mLatitudeTracker.update(position, noise);

        // Longitude
        position = location.getLongitude();
        noise = accuracy * Math.cos(Math.toRadians(location.getLatitude())) * METER_TO_DEG ;

        if (mLongitudeTracker == null) {

            mLongitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
            mLongitudeTracker.setState(position, 0.0, noise);
        }

        mLongitudeTracker.update(position, noise);

        // Altitude
        if (location.hasAltitude()) {

            position = location.getAltitude();
            noise = accuracy;
            if (mAltitudeTracker == null) {

                mAltitudeTracker = new Tracker1D(TIME_STEP, ALTITUDE_NOISE);
                mAltitudeTracker.setState(position, 0.0, noise);
            }

            mAltitudeTracker.update(position, noise);
        }

        // Update last location
        if (mLastLocation == null) {
            mLastLocation = new Location(location);

        }

        try {
            // Calculate prediction
            mLongitudeTracker.predict(0.0);

            if (mLastLocation.hasAltitude())
                mAltitudeTracker.predict(0.0);

            // Latitude
            mLatitudeTracker.predict(0.0);
            location.setLatitude(mLatitudeTracker.getPosition());

            // Longitude
            mLongitudeTracker.predict(0.0);
            location.setLongitude(mLongitudeTracker.getPosition());

            // Altitude
            if (mLastLocation.hasAltitude()) {

                mAltitudeTracker.predict(0.0);
                Double avAltitude = 0.0;
                mAltitudeList.add(mAltitudeTracker.getPosition());
                for(Double altitude : mAltitudeList) {
                    avAltitude += altitude;
                }
                avAltitude /= mAltitudeList.size();

                if(mAltitudeList.size() >= 4) {
                    mAltitudeList.remove(0);
                }

                location.setAltitude(avAltitude);
            }

            // Speed
            if (mLastLocation.hasSpeed())
                location.setSpeed(mLastLocation.getSpeed());

            // Bearing
            if (mLastLocation.hasBearing())
                location.setBearing(mLastLocation.getBearing());

            // Accuracy (always has)
            location.setAccuracy((float) (mLatitudeTracker.getAccuracy() * DEG_TO_METER));

            // Set times
            location.setTime(System.currentTimeMillis());

            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            return location;

        } catch (Exception e) {
            e.printStackTrace();
            return noFilteredLocation;
        }

    }

    public void cleanData(){
        mLatitudeTracker = null;
        mLongitudeTracker = null;
        mAltitudeTracker = null;
    }
}
