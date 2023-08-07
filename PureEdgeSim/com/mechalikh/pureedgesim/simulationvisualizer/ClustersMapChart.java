package com.mechalikh.pureedgesim.simulationvisualizer;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import org.jgrapht.alg.util.Pair;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import utils.CustomCircle;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

/**
 *
 * This class represents a Map Chart that displays the current state of the
 * simulation in a scatter plot.
 *
 * It extends the Chart class and implements methods to update the chart with
 * information about edge devices,
 *
 * edge data centers and cloud CPU utilization.
 */
public class ClustersMapChart extends MapChart {

    private HashSet<String> previousSeriesIds = new HashSet<>();

    /**
     *
     * Constructor for MapChart. Initializes the chart with the given title, x and y
     * axis titles and the SimulationManager. Sets the default series render style
     * to Scatter and the marker size to 4. Calls the updateSize method to set the
     * chart size based on the simulation map dimensions.
     *
     * @param title             the title of the chart
     * @param xAxisTitle        the title of the x axis
     * @param yAxisTitle        the title of the y axis
     * @param simulationManager the SimulationManager instance
     */
    public ClustersMapChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
        super(title, xAxisTitle, yAxisTitle, simulationManager);
        getChart().getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        getChart().getStyler().setMarkerSize(4);
        updateSize(0.0, (double) SimulationParameters.simulationMapWidth, 0.0,
            (double) SimulationParameters.simulationMapLength);
    }

    protected class ClusterToRender {
        public int id;

        public Color color;

        public ArrayList<Double> nodesX;

        public ArrayList<Double> nodesY;

        public ArrayList<examples.TesisClusteringDevice> nodes;

        ClusterToRender(int id, Color color) {
            this.id = id;
            this.color = color;

            this.nodesX = new ArrayList<>();
            this.nodesY = new ArrayList<>();
            this.nodes = new ArrayList<>();
        }

        public void addNode(examples.TesisClusteringDevice node, double x, double y) {
            this.nodesX.add(x);
            this.nodesY.add(y);
            this.nodes.add(node);
        }
    }

    protected ArrayList<examples.TesisClusteringDevice> getPathToOrchestrator(examples.TesisClusteringDevice device) {
        ArrayList<examples.TesisClusteringDevice> pathTo = new ArrayList<>();

        examples.TesisClusteringDevice currentDevice = device;

        while (!currentDevice.isOrchestrator()) {
            pathTo.add(currentDevice);
            currentDevice = currentDevice.getParent();
        }

//        Add the orchestrator to complete the path
        pathTo.add(currentDevice);

        return pathTo;
    }

    protected Pair<ArrayList<Double>, ArrayList<Double>> toXYPaths(ArrayList<examples.TesisClusteringDevice> devices) {
        ArrayList<Double> pathX = new ArrayList<>();
        ArrayList<Double> pathY = new ArrayList<>();

        for (examples.TesisClusteringDevice device : devices) {
            double posX = device.getMobilityModel().getCurrentLocation().getXPos();
            double posY = device.getMobilityModel().getCurrentLocation().getYPos();

            pathX.add(posX);
            pathY.add(posY);
        }

        ArrayList<Double> fullPathX = (ArrayList<Double>) pathX.clone();
        ArrayList<Double> fullPathY = (ArrayList<Double>) pathY.clone();

        Collections.reverse(fullPathX);
        Collections.reverse(fullPathY);

        fullPathX.addAll(pathX);
        fullPathY.addAll(pathY);

//        Need a path that begins from orch and ends in orch so that the line doesn't jump to places
//        where there is not a cluster link in between nodes in the cluster
        return new Pair<>(fullPathX, fullPathY);
    }

    /**
     * Updates the map with the current edge devices and their status.
     */
    protected void updateEdgeDevices() {
        HashMap<Integer, ClusterToRender> clustersById = new HashMap<>();

        ArrayList<Double> orphansX = new ArrayList<>();
        ArrayList<Double> orphansY = new ArrayList<>();

        HashSet<String> newClusterSeriesIds = new HashSet<>();

        for (ComputingNode node : computingNodesGenerator.getMistOnlyList()) {
            examples.TesisClusteringDevice device = (examples.TesisClusteringDevice) node;

            if (device.isOrchestrator() && device.getCluster().size() > 1) {
                Random random = new Random();
                Color randomColor = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));

                clustersById.put(device.getId(), new ClusterToRender(device.getId(), randomColor));

                newClusterSeriesIds.add(Integer.toString(device.getId()));
            }
        }

        for (ComputingNode node : computingNodesGenerator.getMistOnlyList()) {
            examples.TesisClusteringDevice device = (examples.TesisClusteringDevice) node;
            double xPos = device.getMobilityModel().getCurrentLocation().getXPos();
            double yPos = device.getMobilityModel().getCurrentLocation().getYPos();

            ClusterToRender cluster = clustersById.getOrDefault(device.getOrchestrator().getId(), null);

            if (cluster == null) {
                orphansX.add(xPos);
                orphansY.add(yPos);

                continue;
            }

            cluster.addNode(device, xPos, yPos);
        }

        for (ClusterToRender cluster : clustersById.values()) {
            String seriesId = Integer.toString(cluster.id);

            ArrayList<Double> fullPathX = new ArrayList<>();
            ArrayList<Double> fullPathY = new ArrayList<>();

//            Connect all paths between nodes and the orchestrator
            for (examples.TesisClusteringDevice node : cluster.nodes) {
                ArrayList<examples.TesisClusteringDevice> pathTo = getPathToOrchestrator(node);
                Pair<ArrayList<Double>, ArrayList<Double>> pair = toXYPaths(getPathToOrchestrator(node));

                fullPathX.addAll(pair.getFirst());
                fullPathY.addAll(pair.getSecond());
            }


            updateLineSeries(
                getChart(),
                seriesId,
                toArray(fullPathX),
                toArray(fullPathY),
                SeriesMarkers.CIRCLE,
                cluster.color,
                new Color(100, 100, 100, 30)
            );
        }

        updateSeries(
            getChart(),
            "Orphans",
            toArray(orphansX),
            toArray(orphansY),
            SeriesMarkers.CIRCLE,
            Color.LIGHT_GRAY
        );

//        Find series that need to be removed (clusters that disappeared)
        this.previousSeriesIds.removeAll(newClusterSeriesIds);

//        Remove them
        for (String seriesId : this.previousSeriesIds) {
            this.getChart().removeSeries(seriesId);
        }

        this.previousSeriesIds = newClusterSeriesIds;
    }

    /**
     *
     * Updates the map with information about edge devices, edge data centers and
     * cloud CPU utilization.
     */
    public void update() {
        // Add edge devices to map and display their CPU utilization
        updateEdgeDevices();
        // Add edge data centers to the map and display their CPU utilization
        updateEdgeDataCenters();
    }

    /**
     * Updates the map with the current edge data centers and their status.
     */
    protected void updateEdgeDataCenters() {
        // Only if Edge computing is used
        if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE")
            || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
            // List of idle servers
            List<Double> x_idleEdgeDataCentersList = new ArrayList<>();
            List<Double> y_idleEdgeDataCentersList = new ArrayList<>();
            // List of active servers
            List<Double> x_activeEdgeDataCentersList = new ArrayList<>();
            List<Double> y_activeEdgeDataCentersList = new ArrayList<>();

            for (ComputingNode node : computingNodesGenerator.getEdgeOnlyList()) {
                ComputingNode edgeDataCenter = node;
                double Xpos = edgeDataCenter.getMobilityModel().getCurrentLocation().getXPos();
                double Ypos = edgeDataCenter.getMobilityModel().getCurrentLocation().getYPos();
                if (edgeDataCenter.isIdle()) {
                    x_idleEdgeDataCentersList.add(Xpos);
                    y_idleEdgeDataCentersList.add(Ypos);
                } else {
                    x_activeEdgeDataCentersList.add(Xpos);
                    y_activeEdgeDataCentersList.add(Ypos);

                }
            }

            updateSeries(getChart(), "Idle Edge data centers", toArray(x_idleEdgeDataCentersList),
                toArray(y_idleEdgeDataCentersList), SeriesMarkers.CROSS, Color.BLACK);

            updateSeries(getChart(), "Active Edge data centers", toArray(x_activeEdgeDataCentersList),
                toArray(y_activeEdgeDataCentersList), SeriesMarkers.CROSS, Color.red);

        }
    }
}

