package utils;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;

import java.util.ArrayList;
import java.util.List;

public class Formulas {
    public static ArrayList<examples.TesisClusteringDevice> getPredictedNeighbours(
        List<ComputingNode> edgeDevices,
        double timeSlot,
        MobilityModel mobilityModel
    ) {
        ArrayList<examples.TesisClusteringDevice> neighbours = new ArrayList();

        for (int i = 0; i < edgeDevices.size(); i++) {
//			TODO Not skipping myself (is this a problem? shouldn't I think?)

            double distance = mobilityModel.distanceBetween(
                mobilityModel.getLocationForTime(timeSlot),
                edgeDevices.get(i).getMobilityModel().getLocationForTime(timeSlot)
            );

            if (distance <= SimulationParameters.edgeDevicesRange) {
                neighbours.add((examples.TesisClusteringDevice) edgeDevices.get(i));
            }
        }

        return neighbours;
    }
}
