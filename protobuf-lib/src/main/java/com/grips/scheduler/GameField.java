package com.grips.scheduler;

import com.grips.model.scheduler.ExplorationZone;
import com.grips.model.teamserver.MachineInfoRefBox;
import com.grips.team_server.Config;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GameField {

    private List<ExplorationZone> _zones = new ArrayList<>();
    private List<ExplorationZone> _waitingPositions = new ArrayList<>();
    private boolean _occupancyInitialized = false;

    public GameField() {
        for (int width_it = 1; width_it <= (Config.NUM_ZONES_WIDTH / 2); ++width_it) {
            for (int height_it = 1; height_it <= Config.NUM_ZONES_HEIGHT; ++height_it) {
                ExplorationZone currZoneM = new ExplorationZone("M", height_it, width_it);
                ExplorationZone currZoneC = new ExplorationZone("C", height_it, width_it);

                currZoneM.setMirroredZone(currZoneC);
                currZoneC.setMirroredZone(currZoneM);

                currZoneM.setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.FREE);
                currZoneC.setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.FREE);

                if (Config.INVALID_ZONES.stream().filter(i -> i == currZoneC.getZoneNumber()).count() == 0) {
                    // generated zone must qnot be in invalid zones list
                    _zones.add(currZoneM);
                    _zones.add(currZoneC);
                    System.out.println("generated zone: " + currZoneC.getZoneNumber());
                }
                else {
                    System.out.println("did not add zone with number " + currZoneC.getZoneNumber());
                }
            }
        }
    }

    private void setBlockedIfExists(long zoneNumber) {
        Optional<ExplorationZone> any = getAllZones().stream().filter(ez -> ez.getZoneNumber() == zoneNumber).findAny();
        if (any.isPresent()) {
            any.get().setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.BLOCKED);
            any.get().getMirroredZone().setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.BLOCKED);
        }
    }

    public void updateHalfFieldOccupancy(List<MachineInfoRefBox> mInfos, String teamPrefix) {
        if (_occupancyInitialized || mInfos == null || mInfos.size() < 14) {
            return;
        }

        for (MachineInfoRefBox machine : mInfos) {
            if (machine.getZone().startsWith(teamPrefix)) {
                // machine is in our halffield
                ExplorationZone z = getZoneByName(machine.getZone());

                z.setMachine(machine.getName());
                z.setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.MACHINE);
                z.getMirroredZone().setMachine(machine.getName());
                z.getMirroredZone().setZoneOccupiedState(ExplorationZone.ZoneOccupiedState.MACHINE);

                long zoneNumber = z.getZoneNumber();

                if (machine.getRotation() == 0 || machine.getRotation() == 180) {
                    long leftZoneNumber = zoneNumber - 10;
                    long rightZoneNumber = zoneNumber + 10;

                    if (leftZoneNumber > 0) {
                        setBlockedIfExists(leftZoneNumber);
                    }
                    if (rightZoneNumber < ((Config.NUM_ZONES_WIDTH/2)+1)*10) {
                        setBlockedIfExists(rightZoneNumber);
                    }
                } else if (machine.getRotation() == 90 || machine.getRotation() == 270) {
                    long upperZoneNumber = zoneNumber + 1;
                    long lowerZoneNumber = zoneNumber - 1;

                    if (upperZoneNumber%10 <= Config.NUM_ZONES_HEIGHT) {
                        setBlockedIfExists(upperZoneNumber);
                    }
                    if (lowerZoneNumber%10 > 0) {
                        setBlockedIfExists(lowerZoneNumber);
                    }
                } else if (machine.getRotation() == 45 || machine.getRotation() == 225 || machine.getRotation() == 135 || machine.getRotation() == 315) {
                    // these zones are allways blocked
                    long upperZoneNumber = zoneNumber+1;
                    long lowerZoneNumber = zoneNumber-1;
                    long leftZoneNumber = zoneNumber-10;
                    long rightZoneNumber = zoneNumber+10;

                    if (leftZoneNumber > 0) {
                        setBlockedIfExists(leftZoneNumber);
                    }
                    if (rightZoneNumber < ((Config.NUM_ZONES_WIDTH/2)+1)*10) {
                        setBlockedIfExists(rightZoneNumber);
                    }
                    if (upperZoneNumber%10 <= Config.NUM_ZONES_HEIGHT) {
                        setBlockedIfExists(upperZoneNumber);
                    }
                    if (lowerZoneNumber%10 > 0) {
                        setBlockedIfExists(lowerZoneNumber);
                    }

                    if (machine.getRotation() == 135 || machine.getRotation() == 315) {
                        long upperrightZone = zoneNumber+11;
                        long lowerleftZone =  zoneNumber-11;

                        if (upperrightZone%10 <= Config.NUM_ZONES_HEIGHT && upperrightZone < ((Config.NUM_ZONES_WIDTH/2)+1)*10) {
                            setBlockedIfExists(upperrightZone);
                        }
                        if (lowerleftZone%10 > 0 && lowerleftZone > 0) {
                            setBlockedIfExists(lowerleftZone);
                        }
                    } else if (machine.getRotation() == 45 || machine.getRotation() == 225) {
                        long upperleftZone = zoneNumber-10+1;
                        long lowerrightZone =  zoneNumber+10-1;

                        if (upperleftZone%10 <= Config.NUM_ZONES_HEIGHT && upperleftZone > 0) {
                            setBlockedIfExists(upperleftZone);
                        }
                        if (lowerrightZone%10 > 0 && lowerrightZone < ((Config.NUM_ZONES_WIDTH/2)+1)*10) {
                            setBlockedIfExists(lowerrightZone);
                        }
                    }
                }
            }
        }

        generateWaitingPositions(teamPrefix);
        _occupancyInitialized = true;
    }

    private void generateWaitingPositions(String teamPrefix) {
        for (int x = 1; x <= (Config.NUM_ZONES_WIDTH/2); ++x) {
            for (int y = 1; y <= (Config.NUM_ZONES_HEIGHT); ++y) {
                String zoneName = teamPrefix + "_Z" + (x * 10 + y);
                ExplorationZone z = getZoneByName(zoneName);
                if (z != null && z.getZoneOccupiedState() == ExplorationZone.ZoneOccupiedState.FREE) {
                    // zone is free, check if surrounding can be used for path

                    List<ExplorationZone> surroundingZones = new ArrayList<>();

                    String zName;
                    ExplorationZone z1 = null;
                    ExplorationZone z2 = null;
                    ExplorationZone z3 = null;
                    if (x > 1) {
                        // we are mirrored, so "x=0" has the same state as x
                        zName = teamPrefix + "_Z" + ((x - 1) * 10 + (y - 1));
                        z1 = getZoneByName(zName);
                        if (z1 != null) {
                            surroundingZones.add(z1);
                        }
                        zName = teamPrefix + "_Z" + ((x - 1) * 10 + (y));
                        z2 = getZoneByName(zName);
                        if (z2 != null) {
                            surroundingZones.add(z2);
                        }
                        zName = teamPrefix + "_Z" + ((x - 1) * 10 + (y + 1));
                        z3 = getZoneByName(zName);
                        if (z3 != null) {
                            surroundingZones.add(z3);
                        }
                    }

                    zName = teamPrefix + "_Z" + ((x) * 10 + (y-1));
                    ExplorationZone z4 = getZoneByName(zName);
                    if (z4 != null) {
                        surroundingZones.add(z4);
                    }
                    zName = teamPrefix + "_Z" + ((x) * 10 + (y+1));
                    ExplorationZone z5 = getZoneByName(zName);
                    if (z5 != null) {
                        surroundingZones.add(z5);
                    }

                    zName = teamPrefix + "_Z" + ((x+1) * 10 + (y-1));
                    ExplorationZone z6 = getZoneByName(zName);
                    if (z6 != null) {
                        surroundingZones.add(z6);
                    }
                    zName = teamPrefix + "_Z" + ((x+1) * 10 + (y));
                    ExplorationZone z7 = getZoneByName(zName);
                    if (z7 != null) {
                        surroundingZones.add(z7);
                    }
                    zName = teamPrefix + "_Z" + ((x+1) * 10 + (y+1));
                    ExplorationZone z8 = getZoneByName(zName);
                    if (z8 != null) {
                        surroundingZones.add(z8);
                    }

                    if (surroundingZones.stream().allMatch(n -> n.getZoneOccupiedState() == ExplorationZone.ZoneOccupiedState.FREE || n.getZoneOccupiedState() == ExplorationZone.ZoneOccupiedState.BLOCKED)) {
                        // all surroundings are free, use the zone for waiting
                        _waitingPositions.add(z);
                    }

                    if (surroundingZones.size() > 5 && surroundingZones.stream().filter(n -> n.getZoneOccupiedState() == ExplorationZone.ZoneOccupiedState.MACHINE).count() == 1) {
                        // only one machine in our surroundings (if we are not at a wall), we use this for waiting
                        _waitingPositions.add(z);
                    }
                }
            }
        }

        if (_waitingPositions.size() < 3) {
            List<ExplorationZone> mirroredWaiting = new ArrayList<>();
            for (ExplorationZone e : _waitingPositions) {
                mirroredWaiting.add(e.getMirroredZone());
            }
            _waitingPositions.addAll(mirroredWaiting);
        }

        System.out.println("----------Waiting positions--------");
        for (ExplorationZone z : _waitingPositions) {
            System.out.println(z.getZoneName());
        }

        Collections.shuffle(_waitingPositions);
    }

    public ExplorationZone getWaitingZone(int robotId) {
        if (_occupancyInitialized == false || robotId > _waitingPositions.size()) {
            // we do not have enough waiting positions
            return null;
        }

        return _waitingPositions.get(robotId - 1);
    }

    public List<ExplorationZone> getAllZones() {
        return _zones;
    }

    public ExplorationZone getZoneByName(String zoneName) {
        Optional<ExplorationZone> zone = _zones.stream().filter(z -> z.getZoneName().equalsIgnoreCase(zoneName)).findAny();
        if (zone.isPresent()) {
            return zone.get();
        }

        return null;
    }

    public int rotateMachineIfNecessary(int xPos, int yPos, String name, int rotation) {
        boolean nextToWall = false;
        if (yPos == 1 || yPos == 8 || xPos == 7 || (yPos == 2 && xPos == 6)) {
            nextToWall = true;
        }

        rotation = (180 - rotation);
        rotation = (rotation % 360 + 360) % 360;
        if (nextToWall &&  (name.contains("CS") || name.contains("RS"))) {
            rotation = (rotation + 180) % 360;
        }

        return rotation;
    }
}
