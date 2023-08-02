package com.mechalikh.pureedgesim.simulationvisualizer;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;
import utils.CustomCircle;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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

    private ArrayList<String> previousSeriesIds = new ArrayList<>();

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

        ClusterToRender(int id, Color color) {
            this.id = id;
            this.color = color;

            this.nodesX = new ArrayList<>();
            this.nodesY = new ArrayList<>();
        }

        public void addNode(double x, double y) {
            this.nodesX.add(x);
            this.nodesY.add(y);
        }
    }

    /**
     * Updates the map with the current edge devices and their status.
     */
    protected void updateEdgeDevices() {
        HashMap<Integer, ClusterToRender> clustersById = new HashMap<>();

        ArrayList<Double> orphansX = new ArrayList<>();
        ArrayList<Double> orphansY = new ArrayList<>();

        for (ComputingNode node : computingNodesGenerator.getMistOnlyList()) {
            examples.TesisClusteringDevice device = (examples.TesisClusteringDevice) node;

            if (device.isOrchestrator() && device.getCluster().size() > 1) {
                Random random = new Random();
                Color randomColor = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));

                clustersById.put(device.getId(), new ClusterToRender(device.getId(), randomColor));
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

            cluster.addNode(xPos, yPos);
        }

        for (ClusterToRender cluster : clustersById.values()) {
            String seriesId = Integer.toString(cluster.id);

            this.previousSeriesIds.add(seriesId);

            updateSeries(
                getChart(),
                seriesId,
                toArray(cluster.nodesX),
                toArray(cluster.nodesY),
                SeriesMarkers.CIRCLE,
                cluster.color
            );
        }

        updateSeries(
            getChart(),
            "Orphans",
            toArray(orphansX),
            toArray(orphansY),
            SeriesMarkers.CIRCLE,
            Color.GRAY
        );
    }

    /**
     *
     * Updates the map with information about edge devices, edge data centers and
     * cloud CPU utilization.
     */
    public void update() {
        for (String seriesId : this.previousSeriesIds) {
            chart.removeSeries(seriesId);
        }

        this.previousSeriesIds = new ArrayList<>();

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

