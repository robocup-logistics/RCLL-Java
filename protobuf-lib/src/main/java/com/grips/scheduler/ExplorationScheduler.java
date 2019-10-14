package com.grips.scheduler;

import com.google.common.collect.Lists;
import com.grips.model.scheduler.ExplorationZone;
import com.grips.model.scheduler.ProductTask;
import com.grips.model.scheduler.SubProductionTask;
import com.grips.model.teamserver.Robot;
import com.grips.model.teamserver.dao.*;
import com.grips.protobuf_lib.RobotConnectionManager;
import com.grips.protobuf_lib.RobotConnections;
import com.grips.protobuf_lib.SubTaskGenerator;
import com.grips.protobuf_lib.llsf_msgs.PrsTaskProtos;
import com.grips.protobuf_lib.llsf_msgs.TeamProtos;

import javax.annotation.PostConstruct;
import java.awt.geom.Point2D;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static com.grips.model.scheduler.SubProductionTask.TaskState.ASSIGNED;
import static com.grips.model.scheduler.SubProductionTask.TaskState.TBD;

public class ExplorationScheduler {

    private GameField _gameField;
    private GameStateDao gameStateDao;
    private RobotConnections _robotConnections;
    private RobotObservationDao _robotObservationDao;
    private ExplorationTaskDao _explorationTaskDao;
    private SubTaskGenerator _subTaskGenerator;
    private SubProductionTaskDao _subProductionTaskDao;
    private ProductTaskDao _productTaskDao;
    private ProductOrderDao _productOrderDao;
    private RobotConnectionManager _robotConnectionManager;
    private MachineReportDao _machineReportDao;

    private long _explorationId = 0;
    private List<RobotObservation> _notVerifiedObservations = new ArrayList<>();
    private List<String> _alreadyReportedMachines = new ArrayList<>();

    private HashMap<Robot, PrsTaskProtos.PrsTask> _activeExplorationTasks = new HashMap<>();
    private HashMap<Robot, PrsTaskProtos.PrsTask> _previousExplorationTask = new HashMap<>();

    private HashMap<String, Long> _bufferedCs = new HashMap<>();

    private Semaphore _mutex = new Semaphore(1);

    private static String DUMMY_MACHINE = "XXX";

    private boolean _earlyCapBuffering;

    private BigInteger _startEarlyCapBuffering;

    private int _sentToBs = -1;

    // constructor without parameters as required by spring framework
    public ExplorationScheduler() {

    }

    @PostConstruct
    private void afterConstructor() {
        try {
            _mutex.acquire();

            _notVerifiedObservations.clear();
            _notVerifiedObservations.addAll(Lists.newArrayList(_robotObservationDao.findAll()));

            _alreadyReportedMachines.clear();
            Iterable<MachineReport> machineReports = _machineReportDao.findAll();
            for (MachineReport mReport : machineReports) {
                _alreadyReportedMachines.add(mReport.getName());
            }

            // active and previous tasks cannot be loaded, so each robot will get
            // a task that is probably not optimal regarding the distance

        } catch (InterruptedException ie) {
            System.err.println("Mutex interrupted in Constructor of ExplorationScheduler");
        } finally {
            _mutex.release();
        }
    }

    public GameField getGameField() {
        return _gameField;
    }



    public synchronized void addRobotObservation(RobotObservation observation) {
        try {
            _mutex.acquire();

           /* if (observation.isFromLidar()) {
                // new mode, robot can directly force teamserver to report machine

                MachineReport machineReport = new MachineReport();
                machineReport.setName(observation.getMachineName());
                machineReport.setRotation(observation.getOrientation());
                machineReport.setZone(ZoneProtos.Zone.valueOf(observation.getMachineZone()));
                _machineReportDao.save(machineReport);

                _mutex.release();
                explorationTaskResult(observation.getRobotId(), observation.getMachineName(), observation.getMachineZone());
            } else { */
            if (!_alreadyReportedMachines.contains(observation.getMachineName())) {
                // check if machine in zone is already reported by this robot
                if (_notVerifiedObservations.stream().filter(o -> o.getMachineName().equalsIgnoreCase(observation.getMachineName()) &&
                        o.getMachineZone().equalsIgnoreCase(observation.getMachineZone()) &&
                        o.getRobotId() == observation.getRobotId()).count() == 0) {

                    // DS may only be approached from input side
                    if (observation.getMachineName().contains("DS")) {
                        observation.setSide("input");
                    }

                    _notVerifiedObservations.add(observation);
                    _robotObservationDao.save(observation);
                }
            }

            _mutex.release();
        } catch (InterruptedException ie) {
            System.err.println("Mutex interrupted in addRobotObservation");
        }
    }

    // if a robot is active, assign a new task, otherwise return 0
    public PrsTaskProtos.PrsTask getNextExplorationTask(int robotId) {
        PrsTaskProtos.PrsTask newTask = null;

        try {
            _mutex.acquire();

            Robot robot = _robotConnections.getRobot(robotId);

            if (robot == null) {
                System.out.println("Robot that wants a task is not known by system!");
                return null;
            }

            /*
            if (_activeExplorationTasks.containsKey(robot)) {
                //System.out.println("Robot already has active task! Cannot assign new one");
            }*/


            // new in 2019: before letting this robot do further exploration tasks, see if there is a preparecap for him
            // (but only do this in the last minute of exploration)
            BigInteger latestGameTimeExplorationPhase = gameStateDao.getLatestGameTimeExplorationPhase();
            if (latestGameTimeExplorationPhase.compareTo(_startEarlyCapBuffering) >= 0) {
                List<SubProductionTask> prepareCapTasks = _subProductionTaskDao.findByRobotIdAndState(-1, SubProductionTask.TaskState.ASSIGNED_EXPLORATION);
                if (prepareCapTasks != null && prepareCapTasks.size() > 0) {
                    // there is a task that can be done by this robot
                    // only the cap loading should be assigned, discarding
                    // the trash cannot be done since prepare machine doesnt work

                    //TODO: set start time and stuff that is done in production scheduler when task is actually assigned
                    Optional<SubProductionTask> bufferCap = prepareCapTasks.stream().filter(t -> t.getPreConditionTasks() == null || t.getPreConditionTasks().size() == 0).findAny();
                    if (bufferCap.isPresent() && robotId != _sentToBs) {
                        SubProductionTask bufferCapTask = bufferCap.get();
                        bufferCapTask.setState(SubProductionTask.TaskState.INWORK, "inwork started in exploration");
                        bufferCapTask.setRobotId(robotId);
                        List<SubProductionTask> sameRobotTasks = prepareCapTasks.stream().filter(p -> bufferCapTask.equals(p.getSameRobotSubTask())).collect(Collectors.toList());
                        sameRobotTasks.forEach(s -> s.setRobotId(robotId));
                        sameRobotTasks.forEach(s -> s.setState(SubProductionTask.TaskState.ASSIGNED, "assigned started in exploration"));

                        Optional<SubProductionTask> next = prepareCapTasks.stream().filter(p -> p.getPreConditionTasks().contains(bufferCapTask)).findAny();
                        while (next.isPresent()) {
                            if (next.get().getState() != ASSIGNED)
                            next.get().setState(TBD, "dispose waste set to TBD in exploration");
                            _subProductionTaskDao.save(next.get());
                            final SubProductionTask fNext = next.get();
                            next = prepareCapTasks.stream().filter(p -> p.getPreConditionTasks().contains(fNext)).findAny();
                        }

                        _subProductionTaskDao.save(bufferCapTask);
                        _subProductionTaskDao.save(sameRobotTasks);
                        _robotConnectionManager.sendProductionTaskToRobot(bufferCapTask);
                        _robotConnections.getRobot(robotId).setTimeLastTaskAssignment(System.currentTimeMillis());

                        _mutex.release();
                        return null;
                    }
                }
            }

            if (_notVerifiedObservations.size() > 0) {
                // if we do have unchecked observations, we send
                // the robot to the exploration that makes the most sense
                // for him to verify
                newTask = getBestExplorationOption(robot);
                if (newTask == null) {
                    newTask = getRandomExplorationZone(robot);
                }
            } else {
                // if we do not have any observation to check
                // we send to robot to any random zone that is
                // still unexplored
                if (_alreadyReportedMachines.size() < 16) {
                    newTask = getRandomExplorationZone(robot);
                } else if (_sentToBs == -1 && latestGameTimeExplorationPhase.compareTo(_startEarlyCapBuffering) >= 0) {
                    String teamPrefix = null;

                    if (gameStateDao.getTeamColor().startsWith("C")) {
                        teamPrefix = "C-";
                    } else if (gameStateDao.getTeamColor().startsWith("M")) {
                        teamPrefix = "M-";
                    } else {
                        System.err.println("Unknown teamcolor in explorationscheduler observed!");
                    }

                    /*
                    ProductOrder pOrder = new ProductOrder();
                    pOrder.setId(gameStateDao.getLatestGameTimeExplorationPhase().longValue());

                    ProductTask pTask = new ProductTask();
                    pTask.setState(ProductTask.ProductState.INWORK);
                    pTask.setProductOrder(pOrder);

                    pOrder.setId(System.currentTimeMillis());
                    pOrder.setComplexity(ProductOrder.Complexity.D0);

                    _productOrderDao.save(pOrder);
                    _productTaskDao.save(pTask);

                    SubProductionTask subTaskToAdd = SubProductionTaskBuilder.newBuilder()
                            .setMachine(teamPrefix + "BS")
                            .setName("[D0] VisitRandomPlace")
                            .setState(SubProductionTask.TaskState.INWORK)
                            .setSide(SubProductionTask.MachineSide.OUTPUT)
                            .setType(SubProductionTask.TaskType.DUMMY)
                            .setFatal(true) // should not be reasigned
                            .build();
                    subTaskToAdd.setRobotId(robotId);

                    subTaskToAdd.setProductTask(pTask);
                    pTask.setSubProductionTasks(Lists.newArrayList(subTaskToAdd));
                    _subProductionTaskDao.save(subTaskToAdd);

                    PrsTaskProtos.MoveToWayPointTask moveToWayPointTask = PrsTaskProtos.MoveToWayPointTask.newBuilder()
                            .setWaypoint(subTaskToAdd.getMachine() + subTaskToAdd.getSide().toString().toLowerCase() + "_exploration")
                            .build();

                    TeamProtos.Team teamColorProto = TeamProtos.Team.valueOf(gameStateDao.getTeamColor());

                    PrsTaskProtos.PrsTask prsTask = PrsTaskProtos.PrsTask.newBuilder()
                            .setTaskId(new Long(subTaskToAdd.getId()).intValue())
                            .setTeamColor(teamColorProto)
                            .setRobotId(subTaskToAdd.getRobotId())
                            .setMoveToWayPointTask(moveToWayPointTask)
                            .setExecutionResult(PrsTaskProtos.PrsTask.ExecutionResult.FAIL).build();

                    _robotConnectionManager.send_to_robot(robotId, prsTask);
                    _robotConnections.getRobot(robotId).setTimeLastTaskAssignment(System.currentTimeMillis());
*/
                    _sentToBs = robotId;

                } else {
                    // do not do anything here
                    //newTask = getRandomExplorationZone(robot);
                }
            }

            if (newTask != null) {
                _activeExplorationTasks.put(robot, newTask);

                ExplorationTask eTask = new ExplorationTask();
                eTask.setRobotId(robotId);
                eTask.setMachine(newTask.getExploreMachineAtWaypointTask().getMachine());
                eTask.setZone(newTask.getExploreMachineAtWaypointTask().getZoneId());
                eTask.setTimeStamp(System.currentTimeMillis());
                _explorationTaskDao.save(eTask);
            }
        } catch (InterruptedException ie) {
            System.err.println("Mutex interrupted in getNextExplorationTask");
        } finally {
            _mutex.release();
        }

        return newTask;
    }

    public void explorationTaskResult(long robotId, final String machineName, String zoneName) {
        try {
            _mutex.acquire();
            if (machineName != null) {
                _alreadyReportedMachines.add(machineName);

                String mirroredMachineName = "";
                if (machineName.startsWith("C")) {
                    mirroredMachineName = "M" + machineName.substring(1);
                } else if (machineName.startsWith("M")) {
                    mirroredMachineName = "C" + machineName.substring(1);
                }
                _alreadyReportedMachines.add(mirroredMachineName);

                final String mMachineName = mirroredMachineName;
                List<RobotObservation> toDelete = _notVerifiedObservations.stream().filter(o -> o.getMachineName().equalsIgnoreCase(machineName) || o.getMachineName().equalsIgnoreCase(mMachineName)).collect(Collectors.toList());
                _notVerifiedObservations.removeAll(toDelete);
                _robotObservationDao.delete(toDelete);
            }

            _gameField.getZoneByName(zoneName).setExplorationState(ExplorationZone.ZoneExplorationState.EXPLORED);
            _gameField.getZoneByName(zoneName).getMirroredZone().setExplorationState(ExplorationZone.ZoneExplorationState.EXPLORED);

            Robot robot = _robotConnections.getRobot(robotId);
            PrsTaskProtos.PrsTask activeTask = _activeExplorationTasks.get(robot);
            _previousExplorationTask.put(robot, activeTask);
            _activeExplorationTasks.remove(robot);

            // new 2019: if a CS is reported, start to buffer the cap (but only in the last minute of exploration phase,
            // this is checked when assigning the task)
            if (_earlyCapBuffering) {
                String teamPrefix = "";
                String otherTeamPrefix = "";

                if (gameStateDao.getTeamColor().startsWith("C")) {
                    teamPrefix = "C-";
                    otherTeamPrefix = "M-";
                } else if (gameStateDao.getTeamColor().startsWith("M")) {
                    teamPrefix = "M-";
                    otherTeamPrefix = "C-";
                } else {
                    System.err.println("Unknown teamcolor in explorationscheduler observed!");
                }

                String useForBuffering = machineName;
                if (machineName.startsWith(otherTeamPrefix)) {
                    useForBuffering = machineName.replace(otherTeamPrefix, teamPrefix);
                }

                if (useForBuffering.contains("CS") && !_bufferedCs.containsKey(useForBuffering)) {

                    _bufferedCs.put(useForBuffering, -1L); // task is not assigned to a specific robot
                    System.out.println("Robot " + robot.getId() + " reported " + useForBuffering + " that is not buffered, so starting to buffer that CS");

                    ProductOrder pOrder = new ProductOrder();
                    pOrder.setId(System.currentTimeMillis());
                    pOrder.setComplexity(ProductOrder.Complexity.E0);
                    _productOrderDao.save(pOrder);

                    ProductTask pTask = new ProductTask();
                    pTask.setState(ProductTask.ProductState.INWORK, "cap buffer productTask inwork");
                    pTask.setProductOrder(pOrder);
                    _productTaskDao.save(pTask);

                    // we do not know the dispose station yet, so it should be set on task assignment
                    List<SubProductionTask> prepareCapTasks = _subTaskGenerator.prepareCapAssignDisposeLater(pOrder, useForBuffering);

                    prepareCapTasks.forEach(t -> t.setFatal(true)); // do not reassign
                    prepareCapTasks.forEach(t -> t.setRobotId(new Long(-1).intValue()));
                    prepareCapTasks.forEach(t -> t.setState(SubProductionTask.TaskState.ASSIGNED_EXPLORATION, "set to ASSIGNED_EXPLORATION when machine was found"));
                    prepareCapTasks.forEach(t -> t.setProductTask(pTask));

                    prepareCapTasks.get(1).setDemandTaskWithOutDemand(true);

                    pTask.setSubProductionTasks(prepareCapTasks);
                    _subProductionTaskDao.save(prepareCapTasks);
                }
            }
        } catch (InterruptedException ie) {
            System.err.println("Mutex interrupted in explorationTaskResult");
        } finally {
            _mutex.release();
        }
    }

    public void explorationTaskFailed(long robotId, String machineName, String zoneName) {
        try {
            _mutex.acquire();
            // remove either own observation or other observation
            if (!machineName.equalsIgnoreCase(DUMMY_MACHINE)) {
                // we where observing a machine
                Optional<RobotObservation> ownObs = _notVerifiedObservations.stream().filter(o -> o.getRobotId() == robotId && o.getMachineName().equalsIgnoreCase(machineName) && o.getMachineZone().equalsIgnoreCase(zoneName)).findAny();
                if (ownObs.isPresent()) {
                    System.out.println("Removing robot's own observation for machine " + machineName + " at zone " + zoneName);
                    _notVerifiedObservations.remove(ownObs.get());
                }

                Optional<RobotObservation> anyObs = _notVerifiedObservations.stream().filter(o -> o.getMachineName().equalsIgnoreCase(machineName) && o.getMachineZone().equalsIgnoreCase(zoneName)).findAny();
                if (anyObs.isPresent()) {
                    System.out.println("Removing random own observation for machine " + machineName + " at zone " + zoneName);
                    _notVerifiedObservations.remove(anyObs.get());
                }
            } else {
                _gameField.getZoneByName(zoneName).setExplorationState(ExplorationZone.ZoneExplorationState.EXPLORED);
            }
        } catch (InterruptedException ie) {
            System.err.println("Mutex interrupted in explorationTaskFailed");
        } finally {
            _mutex.release();
        }
    }

    private PrsTaskProtos.PrsTask getBestExplorationOption(Robot robot) {

        //System.out.println("In getBestExplorationOption for Robot " + robot.getId());

        // first see, if there are observations by this robot
        List<RobotObservation> usedObservations = new LinkedList<>();
        List<RobotObservation> ownObservations = _notVerifiedObservations.stream().filter(o -> o.getRobotId() == robot.getId()).collect(Collectors.toList());
        if (ownObservations.size() > 0) {
            usedObservations.addAll(ownObservations);
        } else {
            // this case should never happen
            usedObservations.addAll(_notVerifiedObservations);
        }

        // now we try to find the nearest observation
        // first we check, if the robot has a previous task
        Point2D prevZoneCenter = null;
        PrsTaskProtos.PrsTask prevTask = _previousExplorationTask.get(robot);
        if (prevTask != null) {
            //System.out.println("Previous task found for position estimation");
            String zoneId = prevTask.getExploreMachineAtWaypointTask().getZoneId();
            ExplorationZone prevZone = _gameField.getZoneByName(zoneId);
            prevZoneCenter = prevZone.getZoneCenter();

            if (prevZoneCenter == null) {
                System.out.println("Error that should not happen, check!!!");
                prevZoneCenter = new Point2D.Double(0, 0);
            }
        } else {
            prevZoneCenter = new Point2D.Double(0, 0);
        }

        RobotObservation obs = findNearestObservationNotScheduled(usedObservations, prevZoneCenter);

        PrsTaskProtos.PrsTask prsTask = null;
        if (obs != null) {
            prsTask = buildExplorationTask(robot, obs.getMachineName(), obs.getMachineZone(), obs.getSide());
            System.out.println("Sending Robot " + robot.getId() + " to explore Machine " + prsTask.getExploreMachineAtWaypointTask().getMachine() + " at Zone " + prsTask.getExploreMachineAtWaypointTask().getZoneId() + " side: " + prsTask.getExploreMachineAtWaypointTask().getSide());
        }

        return prsTask;
    }

    private PrsTaskProtos.PrsTask buildExplorationTask(Robot robot, String machineId, String zoneId, String side) {
        long taskId = ++_explorationId;

        PrsTaskProtos.ExploreMachineAtWaypointTask exploreTask = PrsTaskProtos.ExploreMachineAtWaypointTask.newBuilder()
                .setMachine(machineId)
                .setSide(side)
                .setZoneId(zoneId).build();

        TeamProtos.Team teamColor = TeamProtos.Team.valueOf(gameStateDao.getTeamColor());

        PrsTaskProtos.PrsTask prsTask = PrsTaskProtos.PrsTask.newBuilder()
                .setTaskId((int) taskId)
                .setTeamColor(teamColor)
                .setRobotId(new Long(robot.getId()).intValue())
                .setExploreMachineAtWaypointTask(exploreTask)
                .setExecutionResult(PrsTaskProtos.PrsTask.ExecutionResult.FAIL).build();

        return prsTask;
    }

    private RobotObservation findNearestObservationNotScheduled(List<RobotObservation> usedObservations, Point2D prevZoneCenter) {
        double minDistance = Double.MAX_VALUE;
        RobotObservation minObs = null;

        //System.out.println("current tasks: ");
        //for (PrsTaskProtos.PrsTask prsTask : _activeExplorationTasks.values()) {
        //    System.out.println("Machine: " + prsTask.getExploreMachineAtWaypointTask().getMachine() + " at zone: " + prsTask.getExploreMachineAtWaypointTask().getZoneId());
        //}

        for (RobotObservation obs : usedObservations) {
            double distance = _gameField.getZoneByName(obs.getMachineZone()).getZoneCenter().distance(prevZoneCenter);
            boolean alreadyScheduled = false;
            alreadyScheduled |= _activeExplorationTasks.values().stream().filter(t -> t.getExploreMachineAtWaypointTask().getZoneId().equalsIgnoreCase(obs.getMachineZone())).count() > 0;
            String mirroredZone = _gameField.getZoneByName(obs.getMachineZone()).getMirroredZone().getZoneName();
            alreadyScheduled |= _activeExplorationTasks.values().stream().filter(t -> t.getExploreMachineAtWaypointTask().getZoneId().equalsIgnoreCase(mirroredZone)).count() > 0;

            alreadyScheduled |= _activeExplorationTasks.values().stream().filter(t -> t.getExploreMachineAtWaypointTask().getMachine().equalsIgnoreCase(obs.getMachineName())).count() > 0;
            String mirroredMachine = obs.getMachineName();
            if (mirroredMachine.startsWith("C")) {
                mirroredMachine = "M" + mirroredMachine.substring(1);
            } else if (mirroredMachine.startsWith("M")) {
                mirroredMachine = "C" + mirroredMachine.substring(1);
            }
            final String mMachine = mirroredMachine;
            alreadyScheduled |= _activeExplorationTasks.values().stream().filter(t -> t.getExploreMachineAtWaypointTask().getMachine().equalsIgnoreCase(mMachine)).count() > 0;

            if (distance < minDistance && !alreadyScheduled) {
                minDistance = distance;
                minObs = obs;
            }
        }

        return minObs;
    }


    public PrsTaskProtos.PrsTask getRandomExplorationZone(Robot robot) {
        //System.out.println("In getRandomExplorationZone for Robot " + robot.getId());

        String teamColorPrefix = gameStateDao.getTeamColor().substring(0,1);
        Collections.shuffle(_gameField.getAllZones());
        Optional<ExplorationZone> unexploredZone = _gameField.getAllZones().stream().filter(z -> z.getExplorationState() == ExplorationZone.ZoneExplorationState.UNEXPLORED
                && z.getZonePrefix().startsWith(teamColorPrefix)).findAny();

        if (unexploredZone.isPresent()) {
            unexploredZone.get().setExplorationState(ExplorationZone.ZoneExplorationState.SCHEDULED);

            PrsTaskProtos.PrsTask prsTask = buildExplorationTask(robot, DUMMY_MACHINE, unexploredZone.get().getZoneName(), "input");

            System.out.println("Sending Robot to random Zone " + prsTask.getExploreMachineAtWaypointTask().getZoneId());

            return prsTask;
        }

        return null;
    }

}
