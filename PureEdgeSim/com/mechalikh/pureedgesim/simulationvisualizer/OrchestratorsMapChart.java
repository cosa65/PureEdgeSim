package com.mechalikh.pureedgesim.simulationvisualizer;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.util.ArrayList;
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
public class OrchestratorsMapChart extends MapChart {

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
    public OrchestratorsMapChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
        super(title, xAxisTitle, yAxisTitle, simulationManager);
        getChart().getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        getChart().getStyler().setMarkerSize(4);
        updateSize(0.0, (double) SimulationParameters.simulationMapWidth, 0.0,
            (double) SimulationParameters.simulationMapLength);
    }

    /**
     * Updates the map with the current edge devices and their status.
     */
    protected void updateEdgeDevices() {
        List<Double> xChildDevices = new ArrayList<>();
        List<Double> yChildDevices = new ArrayList<>();
        List<Double> xOrchestrators = new ArrayList<>();
        List<Double> yOrchestrators = new ArrayList<>();

        for (ComputingNode node : computingNodesGenerator.getMistOnlyList()) {
            ComputingNode device = node;
            double xPos = device.getMobilityModel().getCurrentLocation().getXPos();
            double yPos = device.getMobilityModel().getCurrentLocation().getYPos();

            if (device.isOrchestrator()) {
                xOrchestrators.add(xPos);
                yOrchestrators.add(yPos);
            } else {
                xChildDevices.add(xPos);
                yChildDevices.add(yPos);
            }
        }

        updateSeries(getChart(), "Orchestrators", toArray(xOrchestrators), toArray(yOrchestrators),
            SeriesMarkers.CIRCLE, Color.red);
        updateSeries(getChart(), "Child devices", toArray(xChildDevices), toArray(yChildDevices), SeriesMarkers.CIRCLE,
            Color.blue);
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

