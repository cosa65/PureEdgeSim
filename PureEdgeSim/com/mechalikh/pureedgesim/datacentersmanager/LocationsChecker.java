package com.mechalikh.pureedgesim.datacentersmanager;

import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import org.jgrapht.alg.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static java.lang.System.exit;

public class LocationsChecker {
    private class LocationsStore {
        ArrayList<Integer> xs = new ArrayList<>();
        ArrayList<Integer> ys = new ArrayList<>();

        ArrayList<Movement> customMovements = new ArrayList<>();

        LocationsStore() {}

        public void addCustomPosition(int x, int y) {
            this.xs.add(x);
            this.ys.add(y);
        }

        public void addCustomMovement(Movement movement) {
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
                try {
                    Random random = SecureRandom.getInstanceStrong();

                    return new Pair<>(
                        new Location(
                            random.nextInt(SimulationParameters.simulationMapLength),
                            random.nextInt(SimulationParameters.simulationMapLength)
                        ),
                        movementToReturn
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    exit(1);
                }

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

    private static LocationsChecker instance;

    public static LocationsChecker getInstance() {
        if (instance == null) {
            try {
                instance = new LocationsChecker();
            } catch (Exception e) {
                e.printStackTrace();
                exit(1);
            }

        }

        return instance;
    }

    public LocationsChecker() {
        try (InputStream startingPositionsFile = new FileInputStream(SimulationParameters.startingPositionsFile)) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(startingPositionsFile);
            NodeList nodeList = doc.getElementsByTagName("devicePosition");

            for (int i = 0; i < nodeList.getLength(); i++) {

                Element startingPosition = (Element) nodeList.item(i);

                String deviceTypeName = startingPosition.getElementsByTagName("deviceTypeName").item(0).getTextContent();
                int startingX = Integer.parseInt(startingPosition.getElementsByTagName("x").item(0).getTextContent());
                int startingY = Integer.parseInt(startingPosition.getElementsByTagName("y").item(0).getTextContent());

                LocationsStore store = this.locationsByDeviceType.getOrDefault(deviceTypeName, new LocationsStore());

                store.addCustomPosition(startingX, startingY);

                Element customMovementNode = (Element) startingPosition.getElementsByTagName("customMovement").item(0);

                if (customMovementNode == null) continue;

                ArrayList<Integer> xs = new ArrayList<>();
                ArrayList<Integer> ys = new ArrayList<>();
                NodeList positions = customMovementNode.getElementsByTagName("position");
                for (int j = 0; j < positions.getLength(); j++) {
                    Element position = (Element) positions.item(j);
                    Integer x = Integer.parseInt(position.getElementsByTagName("x").item(0).getTextContent());
                    Integer y = Integer.parseInt(position.getElementsByTagName("y").item(0).getTextContent());

                    xs.add(x);
                    ys.add(y);
                }

                store.addCustomMovement(new Movement(xs, ys));

                this.locationsByDeviceType.put(deviceTypeName, store);
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
