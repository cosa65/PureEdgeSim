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
package examples;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import utils.Formulas;

/** You must read this to understand 
 * This is a simple example showing how to launch simulation using a custom
 * network model. The CustomNetworkModel.java is located under the examples/f
 * older. As you can see, this class extends the MainApplication class provided
 * by PureEdgeSim, which is required for this example to work.
 * 
 * In this example, we will implement the cooperative caching algorithm
 * presented in the following paper: Mechalikh, C., Taktak, H., Moussa, F.:
 * Towards a Scalable and QoS-Aware Load Balancing Platform for Edge Computing
 * Environments. The 2019 International Conference on High Performance Computing
 * & Simulation (2019) 684-691
 *
 * Before running this example you need to
 * 
 * 1/ enable the registry in the simulation parameters file by setting
 * enable_registry=true registry_mode=CACHE
 * 
 * 2/ enable orchestrators in the simulation parameters file by setting
 * enable_orchestrators=true deploy_orchestrator=CLUSTER
 * 
 * you can then compare between registry_mode=CLOUD in which the containers are
 * downloaded from the cloud everytime and registry_mode=CACHE in which the
 * frequently needed containers are cached in edge devices. Same for
 * deploy_orchestrator=CLUSTER and deploy_orchestrator=CLOUD. where the
 * orchestrators are deployed on the cluster heads or on the cloud.
 * 
 * Try to use the MIST_ONLY architecture, in order to see clearly the difference
 * in WAN usage (no tasks offloading to the cloud, so the wan will only be used
 * by containers). To see the effect, try with 60 minutes simulation time.
 * 
 * You will see that the cooperative caching algorithm decreases the WAN usage
 * remarkably.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 2.3
 */
public class TesisClusteringDevice extends DefaultComputingNode {
	private double weight = 0;
	private TesisClusteringDevice parent = this;

	public List<TesisClusteringDevice> cluster;
	private static final int UPDATE_CLUSTERS = 11000;
	private static final double weightDrop = 0.5;

	private static final int updateClusterPollingSlot = 1;

	private int time = -30;
	private List<ComputingNode> edgeDevices;
	private List<ComputingNode> orchestratorsList;

	private static int nextFreeId = 0;

	private int id;

	public TesisClusteringDevice(SimulationManager simulationManager, double mipsCapacity, int numberOfPes,
								 double storage, double ram, String deviceTypeName) {
		super(simulationManager, mipsCapacity, numberOfPes, storage, ram, deviceTypeName);
		cluster = new ArrayList<TesisClusteringDevice>();
		edgeDevices = simulationManager.getDataCentersManager().getComputingNodesGenerator().getMistOnlyList();
		orchestratorsList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
			.getOrchestratorsList();

		this.setAsOrchestrator(true);

		this.id = TesisClusteringDevice.nextFreeId;
		TesisClusteringDevice.nextFreeId += 1;
	}

	public double getCurrentWeight() {
		if (this.isOrchestrator()) {
			return this.getOriginalWeight();
		} else {
			double parentWeight = this.getParent().getCurrentWeight();

			return parentWeight * (1 - weightDrop);
		}
	}

	/**
	 * The clusters update will be done by scheduling events, the first event has to
	 * be scheduled within the startInternal() method:
	 */

	@Override
	public void startInternal() {
		super.startInternal();
		schedule(this, 1, UPDATE_CLUSTERS);
	}

	/**
	 * The scheduled event will be processed in processEvent(). To update the
	 * clusters continuously (a loop) another event has to be scheduled right after
	 * processing the previous one:
	 */
	@Override
	public void processEvent(Event ev) {
		switch (ev.getTag()) {
			case UPDATE_CLUSTERS:
				if ("CLUSTER".equals(SimulationParameters.deployOrchestrators)) {
					time = (int) getSimulation().clock();

					// Update clusters.
					for (int i = 0; i < edgeDevices.size(); i++)
						((TesisClusteringDevice) edgeDevices.get(i)).updateCluster();


					// Schedule the next update if still within simulation duration
					if (SimulationParameters.simulationDuration > time + SimulationParameters.updateInterval) {
						schedule(this, SimulationParameters.updateInterval, UPDATE_CLUSTERS);
					}
				}

				break;
			default:
				super.processEvent(ev);
				break;
		}
	}

	public void updateCluster() {
		if ((!isOrchestrator() && getOrchestratorWeight() < getOriginalWeight()) || ((parent != this)
			&& (this.getMobilityModel().distanceTo(parent) > SimulationParameters.edgeDevicesRange))) {
			setParent(this);
			weight = this.getOrchestratorWeight();
		}

		compareWeightWithNeighbors();
	}

	public double getOriginalWeight() {
		ArrayList<TesisClusteringDevice> currentNeighbors = getNeighbors();
		int currentNeighborsCount = currentNeighbors.size();

		double nextTimeSlot = simulationManager.getSimulation().clock() + 1;
		int nextNeighborsCount = Formulas.getPredictedNeighbours(this.edgeDevices, nextTimeSlot, this.mobilityModel).size();

		double battery;

		if (getEnergyModel().isBatteryPowered()) {
			battery = getEnergyModel().getBatteryLevelPercentage()/100;
		} else {
			battery = 2;
		}
		double mips = this.getMipsPerCore();

		double averageDistanceFromNeighbours = getAverageDistance(currentNeighbors);
		double transmissionRange = SimulationParameters.edgeDevicesRange;

//		double alpha0 = 0.2;
//		double alpha_1 = 0.2;
//		double alpha2 = 0.2;
//		double alpha_3 = 0.2;
//		double alpha_4 = 0.2;

		// mips is divided by 200000 to normalize it, it is out of the parenthesis so
		// the weight becomes 0 when mips = 0
		double computingWeight = (mips / 200000) / currentNeighborsCount;

		SimLog.println(String.format("computingWeight: %s", Double.toString(computingWeight)));
		SimLog.println(String.format("currentNeighborsCount: %s", Integer.toString(currentNeighborsCount)));
		SimLog.println(String.format("nextNeighborsCount: %s", Integer.toString(nextNeighborsCount)));
		SimLog.println(String.format("averageDistanceFromNeighbours / transmissionRange: %s", Double.toString(averageDistanceFromNeighbours / transmissionRange)));
		SimLog.println(String.format("battery * 2: %s", Double.toString(battery * 2)));

		// capacity/#neighbours + #neighbours + #futureNeighbours + averageDistanceFromNeighbours / myTransmissionRange + remainingEnergy
		double weightToReturn = computingWeight
			+ currentNeighborsCount
			+ nextNeighborsCount
			+ averageDistanceFromNeighbours / transmissionRange
			+ battery * 2;

		SimLog.println(String.format("weightToReturn: %s", Double.toString(weightToReturn)));

		return weightToReturn;
	}

	public int getId() {
		return this.id;
	}

	private double getAverageDistance(ArrayList<TesisClusteringDevice> nodes) {
		double sum = 0;
		for (TesisClusteringDevice node : nodes) {
			double distance = this.getMobilityModel().distanceBetween(
				this.getMobilityModel().getCurrentLocation(),
				node.getMobilityModel().getCurrentLocation()
			);

			sum += distance;
		}

		return sum / nodes.size();
	}

	private ArrayList<TesisClusteringDevice> getNeighbors() {
		SimLog.println(String.format("Distances from %d", this.getId()));
		return Formulas.getPredictedNeighbours(
			this.edgeDevices,
			simulationManager.getSimulation().clock(),
			this.mobilityModel
		);
	}

	public boolean isOrchestrator() {
		return this.parent.getId() == this.getId();
	}

	private double getOrchestratorWeight() {
		if (this.isOrchestrator())
			return getOriginalWeight();
//		TODO what is this case???
		if (this.parent == null)
			return 0/0;
		return this.getOrchestrator().getOrchestratorWeight();
	}

	public void setAsOrchestrator(boolean isOrchestrator) {
		this.isOrchestrator = isOrchestrator;

		if (isOrchestrator == false) {
			double a = 0 / 0;
		}

		super.setAsOrchestrator(isOrchestrator);
		this.parent = this;
	}

	public void setParent(TesisClusteringDevice newParent) {
//		If setting myself as the parent, then that means I am also setting myself as orchestrator
		if (newParent == this) {
			this.orchestrator = this;
		}

		// this device has changed its cluster, so it should be removed from the previous one
		if (this.getOrchestrator() != this)
			this.getOrchestrator().cluster.remove(this);

		TesisClusteringDevice newOrchestrator = newParent.getOrchestrator();

		// If the new orchestrator is another device (not this one)
		if (this != newOrchestrator) {
			SimLog.println(String.format("%d is setting external orchestrator: %d", this.getId(), newOrchestrator.getId()));
			// if this device is no more an orchestrator, its cluster will be joined with
			// the cluster of the new orchestrator
			if (isOrchestrator()) {
				newOrchestrator.cluster.addAll(this.cluster);
			}
			// now remove it cluster after
			cluster.clear();
			// remove this device from orchestrators list
			orchestratorsList.remove(this);

			// this device is no more an orchestrator;
			this.parent = newParent;
			this.orchestrator = newOrchestrator;
			this.isOrchestrator = false;

			// in case the cluster doesn't has this device as member
			if (!newOrchestrator.cluster.contains(this))
				newOrchestrator.cluster.add(this);
		}

		// configure the new orchestrator (it can be another device, or this device)
		newOrchestrator.setAsOrchestrator(true);

		newOrchestrator.parent = newOrchestrator;
		// in case the cluster doesn't has the orchestrator as member
		if (!newOrchestrator.cluster.contains(newOrchestrator))
			newOrchestrator.cluster.add(newOrchestrator);
		// add the new orchestrator to the list
		if (!orchestratorsList.contains(newOrchestrator))
			orchestratorsList.add(newOrchestrator);

		this.orchestrator = newOrchestrator;

		if ((this.parent == this && this.getOrchestrator() != this) || (this.getOrchestrator() == this && this.parent != this)) {
			double a = 0/0;
		}
	}

	public TesisClusteringDevice getParent() {
		return this.parent;
	}

	private void compareWeightWithNeighbors() {
		TesisClusteringDevice bestParent = this.getOrchestrator();

		double bestWeight = this.getCurrentWeight();

		for (TesisClusteringDevice neighbor : this.getNeighbors()) {
			if (bestWeight < neighbor.getCurrentWeight()) {
				bestParent = neighbor;
				bestWeight = neighbor.getCurrentWeight() * (1 - weightDrop);
			}
		}

		this.setParent(bestParent);
		weight = bestWeight;
	}

	public TesisClusteringDevice getOrchestrator() {
		if (this.isOrchestrator()) return this;

		return this.parent.getOrchestrator();
	}

}
