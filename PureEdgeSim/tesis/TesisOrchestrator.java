package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;
import net.sourceforge.jFuzzyLogic.FIS;
import utils.Formulas;

import java.util.ArrayList;

import static utils.Formulas.getPredictedNeighbours;

public class TesisOrchestrator extends DefaultOrchestrator {

    public TesisOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
    }

    protected int findComputingNode(String[] architecture, Task task) {
        if ("RANKING".equals(algorithmName)) {
            int selectedNode = ranking(architecture, task);
            SimLog.println("selectedNodeDebug");
            SimLog.println(Integer.toString(selectedNode));
            return selectedNode;
        }

        int selectedNode = super.findComputingNode(architecture, task);
        SimLog.println("selectedNodeDebug");
        SimLog.println(Integer.toString(selectedNode));
        return selectedNode;
    }

    private int ranking(String[] architecture, Task task) {
        int highestScoreIndex = 0;

        double highestScore = 0;

//        ArrayList<examples.TesisClusteringDevice> neighbors = Formulas.getPredictedNeighbours(nodeList, simulationManager.getSimulation().clock(), this.mobilityModel);
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode nodeIt = nodeList.get(i);

//            Skip nodes that don't belong to the current cluster
            if (!offloadingIsPossible(task, nodeIt, architecture)) { continue; }

            if (getScore(nodeIt) > highestScore) {
                highestScore = getScore(nodeIt);
                highestScoreIndex = i;
            }
        }

        return highestScoreIndex;
    }

    // Variables in ranking: mobility, battery, computing cap
    private double getScore(ComputingNode node) {
        double mobility = 1;

        if (node.getMobilityModel().isMobile())
            mobility = 0;

        EnergyModelComputingNode energy = node.getEnergyModel();

        double batteryLeftPercentage = energy.isBatteryPowered() ? energy.getBatteryLevelPercentage() : 1;

        // mips is divided by 200000 to normalize it, it is out of the parenthesis so
        // the weight becomes 0 when mips = 0
        double computingScore = node.getMipsPerCore() / 200000;

        return computingScore + batteryLeftPercentage + mobility;
    }

//    private int fuzzyLogic(Task task) {
//        String fileName = "PureEdgeSim/examples/Example8_settings/stage1.fcl";
//        FIS fis = FIS.load(fileName, true);
//        // Error while loading?
//        if (fis == null) {
//            System.err.println("Can't load file: '" + fileName + "'");
//            return -1;
//        }
//        double cpuUsage = 0;
//        int count = 0;
//        for (int i = 0; i < nodeList.size(); i++) {
//            if (nodeList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
//                count++;
//                cpuUsage += nodeList.get(i).getAvgCpuUtilization();
//
//            }
//        }
//
//        // Set fuzzy inputs
//        fis.setVariable("wan",
//                (SimulationParameters.wanBandwidthBitsPerSecond
//                        - simulationManager.getNetworkModel().getWanUpUtilization())
//                        / SimulationParameters.wanBandwidthBitsPerSecond);
//        fis.setVariable("taskLength", task.getLength());
//        fis.setVariable("delay", task.getMaxLatency());
//        fis.setVariable("cpuUsage", count > 0 ? cpuUsage / count : 1);
//
//        // Evaluate
//        fis.evaluate();
//
//        if (fis.getVariable("offload").defuzzify() > 50) {
//            String[] architecture2 = { "Cloud" };
//            return tradeOff(architecture2, task);
//        } else {
//            String[] architecture2 = { "Edge", "Mist" };
//            return stage2(architecture2, task);
//        }
//
//    }
//
//    private int stage2(String[] architecture2, Task task) {
//        double min = -1;
//        int selected = -1;
//        String fileName = "PureEdgeSim/examples/Example8_settings/stage2.fcl";
//        FIS fis = FIS.load(fileName, true);
//        // Error while loading?
//        if (fis == null) {
//            System.err.println("Can't load file: '" + fileName + "'");
//            return -1;
//        }
//        for (int i = 0; i < nodeList.size(); i++) {
//            if (offloadingIsPossible(task, nodeList.get(i), architecture2) && nodeList.get(i).getTotalStorage() > 0) {
//
//                fis.setVariable("vm_local", 1 - task.getEdgeDevice().getAvgCpuUtilization()
//                        * task.getEdgeDevice().getTotalMipsCapacity() / 1000);
//                fis.setVariable("vm",
//                        (1 - nodeList.get(i).getAvgCpuUtilization()) * nodeList.get(i).getTotalMipsCapacity() / 1000);
//                fis.evaluate();
//
//                if (min == -1 || min > fis.getVariable("offload").defuzzify()) {
//                    min = fis.getVariable("offload").defuzzify();
//                    selected = i;
//                }
//            }
//        }
//        return selected;
//    }

    @Override
    public void resultsReturned(Task task) {
    }

}
