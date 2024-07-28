/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.locationmanager;

import java.util.Random;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator.Movement;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import org.jgrapht.alg.util.Pair;

public class DefaultMobilityModel extends MobilityModel {
	/**
	 * Used to generate random values.
	 * 
	 * @see #pause
	 * @see #reoriontate(double, double)
	 */
	protected Random random;
	protected boolean pause = false;
	protected double pauseDuration = -1;
	protected double mobilityDuration;
	protected int orientationAngle;

	public DefaultMobilityModel(SimulationManager simulationManager, Pair<Location, Movement> startingLocation) {
		super(simulationManager, startingLocation);
//		try {
//			random = SecureRandom.getInstanceStrong();
			random = simulationManager.getRandom();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
		orientationAngle = random.nextInt(359);
	}

	@Override
	protected Location getNextLocation(Location currentLocation) {
		double xPosition = currentLocation.getXPos(); // Get the initial X coordinate assigned to this device
		double yPosition = currentLocation.getYPos(); // Get the initial y coordinate assigned to this device

		if (pause && pauseDuration > 0) {
			// The device mobility is paused until that random delay finishes
			pauseDuration -= SimulationParameters.updateInterval;
			return currentLocation;
		}

		// Make sure that the device stay in the simulation area
		reoriontate(xPosition, yPosition);

		if (mobilityDuration <= 0) {
			pause();
		}

		if (pauseDuration <= 0) {
			resume();
		}

		// Update the currentLocation of this device
		return updateLocation(xPosition, yPosition);
	}

	protected Location updateLocation(double xPosition, double yPosition) {
		double distance = getSpeed() * SimulationParameters.updateInterval;
		double X_distance = Math.cos(Math.toRadians(orientationAngle)) * distance;
		double Y_distance = Math.sin(Math.toRadians(orientationAngle)) * distance;
		// Update the xPosition
		double X_pos = xPosition + X_distance;
		double Y_pos = yPosition + Y_distance;
		return new Location(X_pos, Y_pos);
	}

	protected void resume() {
		// Resume mobility in the next iteration
		pause = false;
		// Increment time and then calculate the next coordinates in the next iteration
		// (the device is moving)
		mobilityDuration -= SimulationParameters.updateInterval;
	}

	protected void pause() {
		// Pickup random duration from 50 to 200 seconds
		pauseDuration = getMinPauseDuration()
				+ random.nextInt((int) ((getMaxPauseDuration() + 1) - getMinPauseDuration()));
		// Pause mobility (the device will stay in its location for the randomly
		// generated duration
		pause = true;
		// Reorientate the device to a new direction
		orientationAngle = random.nextInt(359);
		// The mobility will be resumed for the following period of time
		mobilityDuration = random.nextInt((int) (getMaxMobilityDuration() + 1 - getMinMobilityDuration()))
				+ getMinMobilityDuration();
	}

	protected void reoriontate(double xPosition, double yPosition) {
		this.orientationAngle = this.movement.getAngleToReachNextLocation(new Location(xPosition, yPosition), this.random);
	}

}
