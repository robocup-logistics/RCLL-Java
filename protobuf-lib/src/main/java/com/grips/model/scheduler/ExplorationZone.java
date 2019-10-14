/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.grips.model.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grips.team_server.Config;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

import static com.grips.model.scheduler.ExplorationZone.ZoneExplorationState.UNEXPLORED;

public class ExplorationZone extends Zone {

    protected Point2D zoneCenter;

    protected String zonePrefix;

    protected int heightIDX;

    protected int widthIDX;

    protected ArrayList<ExplorationObservation> observations;

    protected ZoneExplorationState explorationState = UNEXPLORED;

    @JsonIgnore
    protected ExplorationZone mirroredZone;

    protected ZoneState zoneState;

    private String machine;

    private ZoneOccupiedState zoneOccupiedState;

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public ZoneOccupiedState getZoneOccupiedState() {
        return zoneOccupiedState;
    }

    public void setZoneOccupiedState(ZoneOccupiedState zoneOccupiedState) {
        this.zoneOccupiedState = zoneOccupiedState;
    }

    public enum ZoneOccupiedState { MACHINE, BLOCKED, FREE }

    public ExplorationZone(String zonePrefix, int zoneHeightIdx, int zoneWidthIdx) {
        if(zoneHeightIdx > Config.NUM_ZONES_HEIGHT) { System.err.println("Zone height out of bounds!"); }
        if(zoneWidthIdx > (Config.NUM_ZONES_WIDTH/2)) { System.err.println("Zone width out of bounds!"); }

        this.zonePrefix = zonePrefix;
        this.zoneNumber = zoneHeightIdx + 10*zoneWidthIdx;
        this.heightIDX = zoneHeightIdx;
        this.widthIDX = zoneWidthIdx;
        this.zoneName = zonePrefix + "_Z" + String.valueOf(this.zoneNumber);
        this.observations = new ArrayList<>();
        if(Config.FREE_ZONE_NUMBERS.contains(new Long(zoneNumber).intValue())) {
            this.setZoneState(ExplorationZone.ZoneState.FREE);
        } else if(Config.INSERTION_ZONE_NUMBERS.contains(new Long(zoneNumber).intValue())) {
            this.setZoneState(ExplorationZone.ZoneState.INSERTION);
        } else {
            this.setZoneState(ExplorationZone.ZoneState.VALID);
        }
        double yPos = heightIDX - 0.5;
        double xPos =  widthIDX - 0.5;
        if(0 == zonePrefix.compareToIgnoreCase("M")) {
            xPos *= -1;
        }
        this.zoneCenter = new Point2D.Double(xPos, yPos);
    }

    public boolean checkRobotObservation(long robotId) {
        if (getObservations().stream().filter(p -> p.getRobotId() == robotId).count() > 0) {
            return true;
        }

        return false;
    }

    @Nullable
    public AbstractMap.SimpleEntry<String, Integer> getMostSeenMachineWithCount() {

        ArrayList<AbstractMap.SimpleEntry<String, Integer> > machinesWithCount = new ArrayList<>();
        for(String machineName : Config.MPS_NAMES) {
            List<ExplorationObservation> obs;

            obs = observations.stream()
                    .filter(p -> p.getMachine().getName().compareToIgnoreCase(machineName) == 0)
                    .collect(Collectors.toList());

            AbstractMap.SimpleEntry<String, Integer> machineWithCount;
            if(null != obs && obs.size() > 1) {
                machineWithCount = new AbstractMap.SimpleEntry<>(machineName, obs.size());
            } else {
                machineWithCount = new AbstractMap.SimpleEntry<>(Config.UNKNOWN_MACHINE_IDENTIFIER, 0);
            }
            machinesWithCount.add(machineWithCount);
        }
        Collections.sort(machinesWithCount, Comparator.comparing(p -> -p.getValue()));
        return machinesWithCount.get(0);
    }

    public int getObservationCountOfMachineWithName(String machine) {
        List<ExplorationObservation> obs = observations.stream()
                .filter(p -> p.getMachine().getName().compareToIgnoreCase(machine) == 0).collect(Collectors.toList());
        if(null == obs) {
            return 0;
        }
        return obs.size();
    }

    public String getZonePrefix() {
        return zonePrefix;
    }
    public void setZonePrefix(String zonePrefix) {
        this.zonePrefix = zonePrefix;
    }

    public ExplorationZone getMirroredZone() {
        return mirroredZone;
    }
    public void setMirroredZone(ExplorationZone mirroredZone) {
        this.mirroredZone = mirroredZone;
    }

    public Point2D getZoneCenter() {
        return zoneCenter;
    }
    private void setZoneCenter(Point2D zoneCenter) {
        this.zoneCenter = zoneCenter;
    }

    public ArrayList<ExplorationObservation> getObservations() {
        return observations;
    }
    public void setObservations(ArrayList<ExplorationObservation> observations) {
        this.observations = observations;
    }

    public ZoneExplorationState getExplorationState() {
        return explorationState;
    }
    public void setExplorationState(ZoneExplorationState explorationState) {
        this.explorationState = explorationState;
    }

    public ZoneState getZoneState() {
        return zoneState;
    }
    public void setZoneState(ZoneState zoneState) {
        this.zoneState = zoneState;
    }

    public enum ZoneExplorationState {UNEXPLORED, EXPLORED, SCHEDULED, ORIENTATION_FOUND, REPORTED}

    public enum ZoneState {INSERTION, FREE, VALID}

}
