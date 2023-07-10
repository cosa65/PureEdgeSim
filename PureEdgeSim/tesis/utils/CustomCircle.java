package utils;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import org.knowm.xchart.style.markers.Circle;

import java.awt.*;
import java.util.ArrayList;

public class CustomCircle extends Circle {
    public static ArrayList<Integer> customMarkerSizes;
    public static int sizesIt = 0;

    public CustomCircle(ArrayList<Integer> customMarkerSizes) {
        this.customMarkerSizes = customMarkerSizes;
    }

    @Override
    public void paint(Graphics2D g, double xOffset, double yOffset, int markerSize) {
// TODO customMarkerSizes.size() == 0 is when drawing for the reference, ideally we don't want to have the
//  reference painted for radius at all so we should be able to get rid of this case
        int size = customMarkerSizes.size() == 0 ? 1 : this.customMarkerSizes.get(sizesIt % customMarkerSizes.size());

        if (xOffset > 320) {size = 1;}
        super.paint(g, xOffset, yOffset, size);

        if (xOffset <= 200) { sizesIt += 1; }
    }
}
