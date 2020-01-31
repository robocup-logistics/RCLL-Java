package com.grips.utils;

import com.grips.model.teamserver.Order;
import com.grips.model.teamserver.ProductionPointsConfig;
import com.grips.model.teamserver.Ring;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

@Log
@AllArgsConstructor
public class PointEstimator {

    private ProductionPointsConfig productionPointsConfig;

    public int estimatePoints(Order order, boolean inTime, boolean deliveredByOtherTeam) {
        int totalPoints = 0;
        int complexity = 0;
        // correct points for mounting the first ring
        if (order.getRing1() != null) {
            totalPoints += getMountRingPoints(order.getRing1().getRawMaterial());
            complexity++;
        }
        if (order.getRing2() != null) {
            totalPoints += getMountRingPoints(order.getRing2().getRawMaterial());
            complexity++;
        }
        if (order.getRing3() != null) {
            totalPoints += getMountRingPoints(order.getRing3().getRawMaterial());
            complexity++;
        }

        // points for mounting the last ring of a product of complexity higher than C0

        switch (complexity) {
            case 1: totalPoints += productionPointsConfig.getLastRingC1(); break;
            case 2: totalPoints += productionPointsConfig.getLastRingC2(); break;
            case 3: totalPoints += productionPointsConfig.getLastRingC3(); break;
        }

        // we always get points for mounting the cap
        totalPoints += productionPointsConfig.getMountCap();
        totalPoints += productionPointsConfig.getBufferCapInCS();
        // we always get points for delivering the product
        if (inTime) {
            totalPoints += productionPointsConfig.getInTimeDelivery();
        } else {
            totalPoints += productionPointsConfig.getLateDelivery();
        }

        if (order.isCompetitive()) {
            if (deliveredByOtherTeam) {
                totalPoints -= productionPointsConfig.getCompetitiveDeduction();
            } else {
                totalPoints -= productionPointsConfig.getCompetitivePoints();
            }
        }

        return totalPoints;
    }


    private int getMountRingPoints(int rawMaterial) {
        switch (rawMaterial) {
            case 0: return productionPointsConfig.getFinishCC0();
            case 1: return productionPointsConfig.getFinishCC1();
            case 2: return productionPointsConfig.getFinishCC2();

            default:
                log.warning("Ring requires num: " + rawMaterial + " additional bases which we did not expect");
                return 0;
        }
    }


}
