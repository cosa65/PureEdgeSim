package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.System.exit;

public class Movement {
    ArrayList<Integer> xs = new ArrayList<>();
    ArrayList<Integer> ys = new ArrayList<>();

    Movement() {}

    Movement(ArrayList<Integer> xs, ArrayList<Integer> ys) {
        this.xs = xs;
        this.ys = ys;
    }

    public int getAngleToReachNextLocation(Location currentLocation, Random random) {
        Location nextLocation = this.getNextLocation(currentLocation);

//			If there is no next location
        if (nextLocation == null) {
            return this.getRandomAngle(currentLocation, random);
        }

        // Calculate the angle in radians
        double angleRadians = Math.atan2(nextLocation.getYPos() - currentLocation.getYPos(), nextLocation.getXPos() - currentLocation.getXPos());

        // Convert radians to degrees
        int angleDegrees = (int) Math.toDegrees(angleRadians);

        // Normalize the angle to [0, 360) range
        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        return angleDegrees;
    }

    int getRandomAngle(Location currentLocation, Random random) {
        int randomDegree = random.nextInt(180);

        int angle = 0;

        if (currentLocation.getXPos() >= SimulationParameters.simulationMapLength)
            angle = -90 - randomDegree;
        else if (currentLocation.getXPos() <= 0)
            angle = -90 + randomDegree;
        if (currentLocation.getYPos() >= SimulationParameters.simulationMapWidth)
            angle = - randomDegree;
        else if (currentLocation.getYPos() <= 0)
            angle = randomDegree;

        return angle;
    }

    Location getNextLocation(Location currentLocation) {
        if (this.xs.size() == 0) {
            return null;
        }

        Location nextLocation = new Location(this.xs.get(0), this.ys.get(0));

        // If not yet in nextLocation, then return
        if (currentLocation != nextLocation) {
            return nextLocation;
        }

        // If already reached nextLocation and still have more locations available, then pop and return the next one
        if (this.xs.size() > 1) {
            this.xs.remove(0);
            this.ys.remove(0);

            return new Location(
                this.xs.get(0),
                this.ys.get(0)
            );
        }

        exit(1);
        return null;
    }
}
