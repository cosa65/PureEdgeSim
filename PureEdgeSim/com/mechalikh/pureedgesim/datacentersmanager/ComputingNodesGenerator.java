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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import org.jgrapht.alg.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * This class is responsible for generating the computing resources from the
 * input files ( @see
 * com.mechalikh.pureedgesim.simulationcore.SimulationAbstract#setCustomSettingsFolder(String))
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 1.0
 */
public class ComputingNodesGenerator {

	/**
	 * The list that contains all orchestrators. It is used by the computing node.
	 * In this case, the tasks are sent over the network to one of the orchestrators
	 * to make decisions.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager#sendTaskToOrchestrator(Task)
	 */
	protected List<ComputingNode> orchestratorsList;

	/**
	 * The simulation manager.
	 */
	protected SimulationManager simulationManager;

	/**
	 * The Mobility Model to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends MobilityModel> mobilityModelClass;

	/**
	 * The Computing Node Class to be used in this scenario
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#loadModels(DefaultSimulationManager)
	 */
	protected Class<? extends ComputingNode> computingNodeClass;

	/**
	 * A list that contains all edge devices including sensors (i.e., devices
	 * without computing capacities).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> mistOnlyList;

	/**
	 * A list that contains all edge devices except sensors (i.e., devices without
	 * computing capacities).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> mistOnlyListSensorsExcluded;

	/**
	 * A list that contains only edge data centers and servers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#edgeOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> edgeOnlyList = new ArrayList<>(SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains only cloud data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#cloudOnly(Task
	 *      task)
	 */
	protected List<ComputingNode> cloudOnlyList = new ArrayList<>(SimulationParameters.numberOfCloudDataCenters);

	/**
	 * A list that contains cloud data centers and edge devices (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistAndCloud(Task
	 *      task)
	 */
	protected List<ComputingNode> mistAndCloudListSensorsExcluded;

	/**
	 * A list that contains cloud and edge data centers.
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#edgeAndCloud(Task
	 *      task)
	 */
	protected List<ComputingNode> edgeAndCloudList = new ArrayList<>(
			SimulationParameters.numberOfCloudDataCenters + SimulationParameters.numberOfEdgeDataCenters);

	/**
	 * A list that contains edge data centers and edge devices (except sensors).
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#mistAndEdge(Task
	 *      task)
	 */
	protected List<ComputingNode> mistAndEdgeListSensorsExcluded;

	/**
	 * A list that contains all generated nodes including sensors
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#all(Task task)
	 */
	protected List<ComputingNode> allNodesList;

	/**
	 * A list that contains all generated nodes (sensors excluded)
	 * 
	 * @see com.mechalikh.pureedgesim.taskorchestrator.Orchestrator#all(Task task)
	 */
	protected List<ComputingNode> allNodesListSensorsExcluded;

	/**
	 * Constructs a new instance of the computing nodes generator.
	 * 
	 * @param simulationManager  The simulation manager to use.
	 * @param mobilityModelClass The mobility model to use.
	 * @param computingNodeClass The computing node class to use.
	 */
	public ComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		this.simulationManager = simulationManager;
		this.mobilityModelClass = mobilityModelClass;
		this.computingNodeClass = computingNodeClass;
		this.orchestratorsList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistOnlyList = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistOnlyListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount());
		this.mistAndCloudListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfCloudDataCenters);
		this.mistAndEdgeListSensorsExcluded = new ArrayList<>(
				simulationManager.getScenario().getDevicesCount() + SimulationParameters.numberOfEdgeDataCenters);
		this.allNodesList = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
		this.allNodesListSensorsExcluded = new ArrayList<>(simulationManager.getScenario().getDevicesCount()
				+ SimulationParameters.numberOfEdgeDataCenters + SimulationParameters.numberOfCloudDataCenters);
	}

	/**
	 * Generates all computing nodes, including the Cloud data centers, the edge
	 * ones, and the edge devices.
	 */
	public void generateDatacentersAndDevices() {

		// Generate Edge and Cloud data centers.
		generateDataCenters(SimulationParameters.cloudDataCentersFile, SimulationParameters.TYPES.CLOUD);

		generateDataCenters(SimulationParameters.edgeDataCentersFile, SimulationParameters.TYPES.EDGE_DATACENTER);

		// Generate edge devices.
		generateEdgeDevices();

		getSimulationManager().getSimulationLogger().print("%s - Datacenters and devices were generated",
				getClass().getSimpleName());

	}

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

//			If no more locations
			return null;
		}
	}

	private class LocationsChecker {
		private class LocationsStore {
			ArrayList<Integer> xs = new ArrayList<>();
			ArrayList<Integer> ys = new ArrayList<>();

			ArrayList<Movement> customMovements = new ArrayList<>();

			LocationsStore() {}

			public void addCustomPosition(int x, int y) {
				this.xs.add(x);
				this.ys.add(y);
			}

			public void addCustomMovement(Movement movement, ArrayList<Integer> x, ArrayList<Integer> y) {
				this.customMovements.add(movement);
			}

			public Pair<Location, Movement> popLocation() {
				Movement movementToReturn;
				if (this.customMovements.size() > 0) {
					movementToReturn = this.customMovements.remove(0);
				} else {
					movementToReturn = new Movement();
				}

				if (this.xs.size() == 0) {
					//		Random random = SecureRandom.getInstanceStrong();
					Random random = simulationManager.getRandom();

					return new Pair<>(
						new Location(
							random.nextInt(SimulationParameters.simulationMapLength),
							random.nextInt(SimulationParameters.simulationMapLength)
						),
						movementToReturn
					);
				}



				Location locationToReturn = new Location(
					this.xs.remove(0),
					this.ys.remove(0)
				);

				return new Pair<>(
					locationToReturn,
					movementToReturn
				);
			}
		}

		HashMap<String, LocationsStore> locationsByDeviceType = new HashMap<>();

		LocationsChecker() {
			try (InputStream startingPositionsFile = new FileInputStream(SimulationParameters.startingPositionsFile)) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(startingPositionsFile);
				NodeList nodeList = doc.getElementsByTagName("devicePosition");

				for (int i = 0; i < nodeList.getLength(); i++) {
					Element position = (Element) nodeList.item(i);

					String deviceTypeName = position.getElementsByTagName("deviceTypeName").item(0).getTextContent();
					int x = Integer.parseInt(position.getElementsByTagName("x").item(0).getTextContent());
					int y = Integer.parseInt(position.getElementsByTagName("y").item(0).getTextContent());

					LocationsStore store = this.locationsByDeviceType.getOrDefault(deviceTypeName, new LocationsStore());

					store.addCustomPosition(x, y);

					this.locationsByDeviceType.put(deviceTypeName, store);

					Node customMovement;
					try {
						customMovement = position.getElementsByTagName("customMovement").item(0);
					} catch (Exception e) {
//						If no customMovement was found, return. Default mobility is random
						return;
					}


				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Pair<Location, Movement> getLocation(String deviceTypeName) {
			LocationsStore store = this.locationsByDeviceType.getOrDefault(deviceTypeName, new LocationsStore());

			return store.popLocation();
		}
	}

	/**
	 * Generates edge devices
	 */
	public void generateEdgeDevices() {

		// Generate edge devices instances from edge devices types in xml file.
		try (InputStream devicesFile = new FileInputStream(SimulationParameters.edgeDevicesFile)) {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(devicesFile);
			NodeList nodeList = doc.getElementsByTagName("device");
			Element edgeElement = null;

			LocationsChecker locationsChecker = new LocationsChecker();

			// Load all devices types in edge_devices.xml file.
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node edgeNode = nodeList.item(i);
				edgeElement = (Element) edgeNode;

				generateDevicesInstances(edgeElement);
			}

			// if percentage of generated devices is < 100%.
			if (mistOnlyList.size() < getSimulationManager().getScenario().getDevicesCount())
				getSimulationManager().getSimulationLogger().print(
						"%s - Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !",
						getClass().getSimpleName());
			// Add more devices.
			if (edgeElement != null) {
				int missingInstances = getSimulationManager().getScenario().getDevicesCount() - mistOnlyList.size();
				for (int k = 0; k < missingInstances; k++) {
					ComputingNode newDevice = createComputingNode(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE, locationsChecker);
					insertEdgeDevice(newDevice);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Puts the newly generated edge device in corresponding lists.
	 */
	protected void insertEdgeDevice(ComputingNode newDevice) {
		mistOnlyList.add(newDevice);
		allNodesList.add(newDevice);
		if (!newDevice.isSensor()) {
			mistOnlyListSensorsExcluded.add(newDevice);
			mistAndCloudListSensorsExcluded.add(newDevice);
			mistAndEdgeListSensorsExcluded.add(newDevice);
			allNodesListSensorsExcluded.add(newDevice);
		}
	}

	/**
	 * Generates the required number of instances for each type of edge devices.
	 * 
	 * @param type The type of edge devices.
	 */
	protected void generateDevicesInstances(Element type) {
		generateDevicesInstances(type, new LocationsChecker());
	}

	protected void generateDevicesInstances(Element type, LocationsChecker locationsChecker) {

		int instancesPercentage = Integer.parseInt(type.getElementsByTagName("percentage").item(0).getTextContent());

		// Find the number of instances of this type of devices
		int devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

		for (int j = 0; j < devicesInstances; j++) {
			if (mistOnlyList.size() > getSimulationManager().getScenario().getDevicesCount()) {
				getSimulationManager().getSimulationLogger().print(
						"%s - Wrong percentages values (the sum is superior than 100%%), check edge_devices.xml file !",
						getClass().getSimpleName());
				break;
			}

			try {
				insertEdgeDevice(createComputingNode(type, SimulationParameters.TYPES.EDGE_DEVICE, locationsChecker));
			} catch (Exception e ) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Generates the Cloud and Edge data centers.
	 * 
	 * @param file The configuration file.
	 * @param type The type, whether a CLOUD data center or an EDGE one.
	 */
	protected void generateDataCenters(String file, TYPES type) {

		// Fill list with edge data centers
		try (InputStream serversFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);
			NodeList datacenterList = doc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				Element datacenterElement = (Element) datacenterList.item(i);
				ComputingNode computingNode = createComputingNode(datacenterElement, type);
				if (computingNode.getType() == TYPES.CLOUD) {
					cloudOnlyList.add(computingNode);
					mistAndCloudListSensorsExcluded.add(computingNode);
					if (SimulationParameters.enableOrchestrators
							&& SimulationParameters.deployOrchestrators == "CLOUD") {
						orchestratorsList.add(computingNode);
					}
				} else {
					edgeOnlyList.add(computingNode);
					mistAndEdgeListSensorsExcluded.add(computingNode);
					if (SimulationParameters.enableOrchestrators
							&& SimulationParameters.deployOrchestrators == "EDGE") {
						orchestratorsList.add(computingNode);
					}
				}
				allNodesList.add(computingNode);
				allNodesListSensorsExcluded.add(computingNode);
				edgeAndCloudList.add(computingNode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the computing nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type) throws Exception {
		return this.createComputingNode(datacenterElement, type, new LocationsChecker());
	}

	protected ComputingNode createComputingNode(Element datacenterElement, SimulationParameters.TYPES type, LocationsChecker locationsChecker)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// SecureRandom is preferred to generate random values.

		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());

		Pair<Location, Movement> datacenterLocationSetup = new Pair<>(new Location(xPosition, yPosition), new Movement());
		int numOfCores = Integer.parseInt(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
		double mips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());
		double storage = Double.parseDouble(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
		double ram = Double.parseDouble(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());

		String deviceTypeName = datacenterElement.getElementsByTagName("deviceTypeName").item(0).getTextContent();

		Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
				int.class, double.class, double.class, String.class);
		ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
				numOfCores, storage, ram, deviceTypeName);

//		computingNode.setAsOrchestrator(Boolean
//				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

		if (computingNode.isOrchestrator())
			orchestratorsList.add(computingNode);

		computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

		if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			String name = datacenterElement.getAttribute("name");
			computingNode.setName(name);
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			xPosition = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
			yPosition = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
			datacenterLocationSetup = new Pair<>(new Location(xPosition, yPosition), new Movement());

			for (int i = 0; i < edgeOnlyList.size(); i++)
				if (datacenterLocationSetup.equals(edgeOnlyList.get(i).getMobilityModel().getCurrentLocation()))
					throw new IllegalArgumentException(
							" Each Edge Data Center must have a different location, check the \"edge_datacenters.xml\" file!");

			computingNode.setPeriphery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));

		} else if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			mobile = Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(datacenterElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(datacenterElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
					datacenterElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			computingNode.getEnergyModel().setIntialBatteryPercentage(Double.parseDouble(
					datacenterElement.getElementsByTagName("initialBatteryLevel").item(0).getTextContent()));
			computingNode.getEnergyModel().setConnectivityType(
					datacenterElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			datacenterLocationSetup = locationsChecker.getLocation(deviceTypeName);

			Location location = datacenterLocationSetup.getFirst();

			getSimulationManager().getSimulationLogger()
					.deepLog("ComputingNodesGenerator- Edge device:" + mistOnlyList.size() + "    location: ( "
							+ location.getXPos() + "," + location.getYPos() + " )");
			SimLog.println("ComputingNodesGenerator- Edge deviceDebug:" + mistOnlyList.size() + "    location: ( "
				+ location.getXPos() + "," + location.getYPos() + " )");
		}
		computingNode.setType(type);
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocationSetup)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		computingNode.setMobilityModel(mobilityModel);

		return computingNode;
	}

	/**
	 * Returns the list containing computing nodes that have been selected as
	 * orchestrators (i.e. to make offloading decisions).
	 * 
	 * @return The list of orchestrators
	 */
	public List<ComputingNode> getOrchestratorsList() {
		return orchestratorsList;
	}

	/**
	 * Returns the simulation Manager.
	 * 
	 * @return The simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Gets the list containing all generated computing nodes.
	 * 
	 * @see #generateDatacentersAndDevices()
	 * 
	 * @return the list containing all generated computing nodes.
	 */
	public List<ComputingNode> getAllNodesList() {
		return this.allNodesList;
	}

	/**
	 * Gets the list containing all generated edge devices including sensors (i.e.,
	 * devices with no computing resources).
	 * 
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices including sensors.
	 */
	public List<ComputingNode> getMistOnlyList() {
		return this.mistOnlyList;
	}

	/**
	 * Gets the list containing all generated edge data centers / servers.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all edge data centers and servers.
	 */
	public List<ComputingNode> getEdgeOnlyList() {
		return this.edgeOnlyList;
	}

	/**
	 * Gets the list containing only cloud data centers.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing all generated cloud data centers.
	 */
	public List<ComputingNode> getCloudOnlyList() {
		return this.cloudOnlyList;
	}

	/**
	 * Gets the list containing cloud data centers and edge devices (except
	 * sensors).
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return the list containing cloud data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndCloudListSensorsExcluded() {
		return this.mistAndCloudListSensorsExcluded;
	}

	/**
	 * Gets the list containing cloud and edge data centers.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * 
	 * @return the list containing cloud and edge data centers.
	 */
	public List<ComputingNode> getEdgeAndCloudList() {
		return this.edgeAndCloudList;
	}

	/**
	 * Gets the list containing edge data centers and edge devices (except sensors).
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return the list containing edge data centers and edge devices.
	 */
	public List<ComputingNode> getMistAndEdgeListSensorsExcluded() {
		return this.mistAndEdgeListSensorsExcluded;
	}

	/**
	 * Gets the list containing all generated edge devices except sensors (i.e.,
	 * devices with no computing resources).
	 * 
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return the list containing all edge devices except sensors.
	 */
	public List<ComputingNode> getMistOnlyListSensorsExcluded() {
		return this.mistOnlyListSensorsExcluded;
	}

	/**
	 * Gets the list containing all computing nodes (except sensors).
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @return the list containing all data centers and devices except sensors.
	 */
	public List<ComputingNode> getAllNodesListSensorsExcluded() {
		return this.allNodesListSensorsExcluded;
	}

}
