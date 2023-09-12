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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationvisualizer.ClustersMapChart;
import utils.Formulas;

import static com.mechalikh.pureedgesim.simulationengine.EventType.UPDATE_CLUSTERS;

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

	private HashSet<TesisClusteringDevice> cluster;
	private static final double weightDrop = 0.5;
	private static final int updateClusterPollingSlot = 1;

	private int time = -30;
	private List<ComputingNode> edgeDevices;
	public static HashSet<ComputingNode> orchestratorsList = new HashSet<>();

	private static int nextFreeId = 0;

	private int id;

	public TesisClusteringDevice(SimulationManager simulationManager, double mipsCapacity, int numberOfPes,
								 double storage, double ram, String deviceTypeName) {
		super(simulationManager, mipsCapacity, numberOfPes, storage, ram, deviceTypeName);
		this.cluster = new HashSet<>();
		this.getCluster().add(this);

		this.edgeDevices = simulationManager.getDataCentersManager().getComputingNodesGenerator().getMistOnlyList();

		this.setAsOrchestrator(true);

		this.id = TesisClusteringDevice.nextFreeId;
		TesisClusteringDevice.nextFreeId += 1;

		TesisClusteringDevice.orchestratorsList.add(this);
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
		oncePerSecond = getSimulation().clock();
	}

	public static double oncePerSecond;

	/**
	 * The scheduled event will be processed in processEvent(). To update the
	 * clusters continuously (a loop) another event has to be scheduled right after
	 * processing the previous one:
	 */
	@Override
	public void processEvent(Event ev) {
		switch (ev.getType()) {
			case UPDATE_CLUSTERS:
				if ("CLUSTER".equals(SimulationParameters.deployOrchestrators)) {
					time = (int) getSimulation().clock();

					if (oncePerSecond != time) {
						oncePerSecond = time;
					}

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

//		SimLog.println(String.format("computingWeight: %s", Double.toString(computingWeight)));
//		SimLog.println(String.format("currentNeighborsCount: %s", Integer.toString(currentNeighborsCount)));
//		SimLog.println(String.format("nextNeighborsCount: %s", Integer.toString(nextNeighborsCount)));
//		SimLog.println(String.format("averageDistanceFromNeighbours / transmissionRange: %s", Double.toString(averageDistanceFromNeighbours / transmissionRange)));
//		SimLog.println(String.format("battery * 2: %s", Double.toString(battery * 2)));

		// capacity/#neighbours + #neighbours + #futureNeighbours + averageDistanceFromNeighbours / myTransmissionRange + remainingEnergy
		double weightToReturn = 0.4 * computingWeight
			+ 0.1 * currentNeighborsCount
			+ 0.1 * nextNeighborsCount
			+ 0.2 * (averageDistanceFromNeighbours / transmissionRange)
			+ 0.2 * (battery * 2);

//		SimLog.println(String.format("weightToReturn: %s", Double.toString(weightToReturn)));

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
//		SimLog.println(String.format("Distances from %d", this.getId()));
		return Formulas.getPredictedNeighbours(
			this.edgeDevices,
			simulationManager.getSimulation().clock(),
			this.mobilityModel
		);
	}

	public boolean isOrchestrator() {
		return this.parent.getId() == this.getId();
	}

	public HashSet<TesisClusteringDevice> getCluster() {
		if (edgeDevices == null) { return new HashSet<>(); }

		if (this.isOrchestrator()) {
			HashSet<TesisClusteringDevice> cluster = new HashSet<>();
			cluster.add(this);
			for (ComputingNode device : edgeDevices) {
				TesisClusteringDevice clusteringDevice = (TesisClusteringDevice) device;

				if (clusteringDevice.getOrchestrator().id == this.id) {
					cluster.add(clusteringDevice);
				}
			}

			return cluster;
		} else {
			return this.getOrchestrator().getCluster();
		}
	}

	private double getOrchestratorWeight() {
		if (this.isOrchestrator())
			return getOriginalWeight();
		return this.getOrchestrator().getOrchestratorWeight();
	}

	public void setAsOrchestrator(boolean isOrchestrator) {
//		If it was already an orchestrator, ignore (we don't want to reset the cluster)
		if (this.isOrchestrator()) {
			return;
		}

		this.isOrchestrator = isOrchestrator;

		super.setAsOrchestrator(isOrchestrator);
		this.parent = this;

		this.cluster = new HashSet<>();
		this.cluster.add(this);

		TesisClusteringDevice.orchestratorsList.add(this);
	}

	public void setParent(TesisClusteringDevice newParent) {
//		If breaking away from its current cluster, just setting itself as orchestrator should be enough
		if (newParent.id == this.id && !this.isOrchestrator()) {
			this.setAsOrchestrator(true);

			return;
		}

		TesisClusteringDevice newOrchestrator = newParent.getOrchestrator();

		newOrchestrator.setAsOrchestrator(true);

		// If the new orchestrator is another device (not this one)
		if (this.isOrchestrator() && this != newOrchestrator) {
//			SimLog.println(String.format("%d is setting external orchestrator: %d", this.getId(), newOrchestrator.getId()));

			// this device is no more an orchestrator;
			this.orchestrator = newOrchestrator;
			this.isOrchestrator = false;
		}

//		If I'm no longer an orchestrator, remove from list
		if (this != newOrchestrator) {
			TesisClusteringDevice.orchestratorsList.remove(this);
		}

		this.parent = newParent;
		this.orchestrator = newOrchestrator;
	}

	public class NodeRelevant {
		public int id;
		public int orchestratorId;

		NodeRelevant(int id, int orchestratorId) {
			this.id = id;
			this.orchestratorId = orchestratorId;
		}
	}

	public ArrayList<List<NodeRelevant>> debugGetClusterRelevantInfoList() {
		ArrayList<List<NodeRelevant>> clustersList = new ArrayList<>();
		for (ComputingNode orchestratorNode : TesisClusteringDevice.orchestratorsList) {
			TesisClusteringDevice orchestrator = (TesisClusteringDevice) orchestratorNode;

			clustersList.add(
				orchestrator.getCluster().stream()
					.map(i -> new NodeRelevant(i.id, i.getOrchestrator().id))
					.collect(Collectors.toList())
			);
		}

		return clustersList;
	}

	public ArrayList<HashSet<TesisClusteringDevice>> debugGetClustersList() {
		ArrayList<HashSet<TesisClusteringDevice>> clustersList = new ArrayList<>();
		for (ComputingNode orchestratorNode : TesisClusteringDevice.orchestratorsList) {
			TesisClusteringDevice orchestrator = (TesisClusteringDevice) orchestratorNode;
			clustersList.add(orchestrator.getCluster());
		}

		return clustersList;
	}

	public boolean debugIsClustersListBroken() {
		for (HashSet<TesisClusteringDevice> cluster : debugGetClustersList()) {
			int orchestratorId = cluster.iterator().next().getOrchestrator().id;

			for (TesisClusteringDevice node : cluster) {
				if (node.getOrchestrator().id != orchestratorId) {
					return true;
				}
			}
		}

		return false;
	}

	public TesisClusteringDevice getParent() {
		return this.parent;
	}

	private void compareWeightWithNeighbors() {
//		My weight might have changed, so I need to compare again from scratch, I can't rely on the weight I held before
//		So use my own weight as the initial best option (because it's always going to exist)
		TesisClusteringDevice bestParent = this;
		double bestWeight = this.getOriginalWeight();

		for (TesisClusteringDevice neighbor : this.getNeighbors()) {
			double weightIfParent = neighbor.getCurrentWeight() * (1 - weightDrop);

			if (bestWeight < weightIfParent) {
				bestParent = neighbor;
				bestWeight = weightIfParent;
			}
		}

		this.setParent(bestParent);
		this.weight = bestWeight;
	}

	public TesisClusteringDevice getOrchestrator() {
		if (this.isOrchestrator()) return this;

		return this.parent.getOrchestrator();
	}

}
