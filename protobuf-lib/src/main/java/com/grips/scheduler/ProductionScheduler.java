package com.grips.scheduler;

import com.google.common.collect.Lists;
import com.grips.model.scheduler.PartDemand;
import com.grips.model.scheduler.ProductTask;
import com.grips.model.scheduler.SubProductionTask;
import com.grips.model.teamserver.MachineInfoRefBox;
import com.grips.model.teamserver.ProductOrder;
import com.grips.model.teamserver.Ring;
import com.grips.model.teamserver.dao.*;
import com.grips.team_server.*;
import com.grips.tools.PathEstimator;
import org.jetbrains.annotations.NotNull;
import org.robocup_logistics.llsf_msgs.PrsTaskProtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.grips.model.scheduler.SubProductionTask.TaskState.*;

@Service
public class ProductionScheduler {

    public static final String READY_AT_OUTPUT = "READY-AT-OUTPUT";
    public static final String IDLE = "IDLE";
    public static final String DOWN = "DOWN";
    public static final String BROKEN = "BROKEN";
    public static final String PREPARED = "PREPARED";
    public static final String PROCESSING = "PROCESSING";
    public static final String PROCESSED = "PROCESSED";
    // all times in minutes
    @Value("${gameconfig.productiontimes.estimate_C0}")
    private int estimateC0;
    @Value("${gameconfig.productiontimes.estimate_C1}")
    private int estimateC1;
    @Value("${gameconfig.productiontimes.estimate_C2}")
    private int estimateC2;
    @Value("${gameconfig.productiontimes.estimate_C3}")
    private int estimateC3;
    @Value("${gameconfig.productiontimes.max_wait}")
    private int maxWait;

    @Autowired
    private RobotConnectionManager rcm;

    @Autowired
    private RobotConnections robotConnections;

    @Autowired
    ResourceManager resourceManager;

    @Autowired
    SubProductionTaskDao subProductionTaskDao;

    @Autowired
    ProductTaskDao productTaskDao;

    @Autowired
    ProductOrderDao productOrderDao;

    @Autowired
    GameStateDao gameStateDao;

    @Autowired
    SubTaskGenerator subTaskGenerator;

    @Autowired
    RingDao ringDao;

    @Autowired
    MachineInfoRefBoxDao machineInfoRefBoxDao;

    @Autowired
    MPSHandler mpsHandler;

    @Autowired
    ResetMPSSender resetMPSSender;

    @Autowired
    PathEstimator pathEstimator;

    @Autowired
    PartDemandDao partDemandDao;

    //private Semaphore _mutex = new Semaphore(1);
    private SubProductionTask.MachineSide bsSide = SubProductionTask.MachineSide.INPUT;


    private SubProductionTask assignTask(SubProductionTask task, int robotId) {

        task.setRobotId(robotId);
        task.setState(SubProductionTask.TaskState.INWORK, "set to INWORK due to assigning task to robot");
        subProductionTaskDao.save(task);

        SubProductionTask sameRobotTask = subProductionTaskDao.findBySameRobotSubTask(task);
        while (sameRobotTask != null) {
            sameRobotTask.setRobotId(robotId);
            sameRobotTask.setState(SubProductionTask.TaskState.ASSIGNED, "set subsequent DELIVER task to ASSIGNED");
            subProductionTaskDao.save(sameRobotTask);

            sameRobotTask = subProductionTaskDao.findBySameRobotSubTask(sameRobotTask);
        }

        return task;
    }


    private boolean needToWaitForDelivery(SubProductionTask readyToDo) {
        // check if we need to wait for delivery
        if (readyToDo.getMachine() != null && readyToDo.getMachine().contains("DS")) {
            // check if we already can deliver, delivery period begin is specified in s, whereas gametime is specified in ns
            if (gameStateDao.getLatestGameTimeProductionPhase().longValue() < readyToDo.getProductTask().getProductOrder().getDeliveryPeriodBegin() * 1000L * 1000L * 1000L) {
                // we need to wait for the deliveryperiod to begin
                System.out.println("Wait for deliveryperiod to begin for delivering product");
                return true;
            } else {
                System.out.println("Deliveryperiod already started, so we can deliver the product");
            }
        }

        return false;
    }


    //Step1: Find linked tasks for same robot
    //Step2: See if we need to do some cleanup
    //Step3: Schedule task
    //Step4: Demand tasks
    //Step5: New Product
    //Step6+7: In new product: either new task or demand task
    public SubProductionTask findSuitableTask(int robotId) {

        synchronized (subProductionTaskDao) {
            // Step1: Find linked tasks for same robot
            SubProductionTask linkedTask = findSameRobotTask(robotId);
            if (linkedTask != null) {
                // if machine is in use by other robot, do circle around task in the meantime
                if (linkedTask.getType() == SubProductionTask.TaskType.DELIVER
                        && resourceManager.isMachineLockedByOtherRobot(linkedTask.getMachine(), linkedTask.getType(), robotId)) {
                    // we already have the product in gripper, so do a circle around task meanwhile
                    return null;
                }

                // in this case, we need to check for early delivery and not look for further tasks if this is the case
                if (!needToWaitForDelivery(linkedTask)) {
                    return assignTask(linkedTask, robotId);
                } else {
                    return null; // will get assigned a dummy task, has product in gripper and gets deliver task assigned later
                }
            }

            // Step2: See if we need to do some cleanup
            SubProductionTask cTask = findCleanupTasks();
            if (cTask != null) {
                return assignTask(cTask, robotId);
            }

            // Step3: Schedule task (in already active products)
            SubProductionTask task = findNextTaskInActiveProduct(robotId);
            if (task != null) {
                return assignTask(task, robotId);
            }

            // Step4: See if resources demanded
            SubProductionTask resourceDemand = findDemandedResourceTask();
            if (resourceDemand != null) {
                return assignTask(resourceDemand, robotId);
            }

            // sleep and check again if tasks can be done
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // Step3a: Schedule task (in already active products)
            task = findNextTaskInActiveProduct(robotId);
            if (task != null) {
                return assignTask(task, robotId);
            }

            // Step4a: See if resources demanded
            resourceDemand = findDemandedResourceTask();
            if (resourceDemand != null) {
                return assignTask(resourceDemand, robotId);
            }

            // Step5: Start a new product (if possible)
            startNewProductIfFeasible(robotId);

            // Step6: Schedule task (again)
            task = findNextTaskInActiveProduct(robotId);
            if (task != null) {
                return assignTask(task, robotId);
            }

            // Step7: Demand task (again)
            resourceDemand = findDemandedResourceTask();
            if (resourceDemand != null) {
                return assignTask(resourceDemand, robotId);
            }

            return null;
        }
    }

    private SubProductionTask findCleanupTasks() {
        String teamColor = gameStateDao.getTeamColor();
        MachineInfoRefBox.TeamColor tColor = MachineInfoRefBox.TeamColor.CYAN;
        if (teamColor.startsWith("M")) {
            tColor = MachineInfoRefBox.TeamColor.MAGENTA;
        }

        List<MachineInfoRefBox> machines = machineInfoRefBoxDao.findByTeamColor(tColor);
        List<MachineInfoRefBox> readyAtOutputMachines = machines.stream().filter(m -> READY_AT_OUTPUT.equals(m.getState())).collect(Collectors.toList());

        for (MachineInfoRefBox m : readyAtOutputMachines) {
            // method does not work for BS since we do not know if input or output
            if (m.getName().contains("BS")) {
                continue;
            }

            // see if no task can be done for the ready-at-output state machine
            Collection<SubProductionTask> tasksForMachine = subProductionTaskDao.findByMachineAndSideInAndStateIn(m.getName(), Arrays.asList(SubProductionTask.MachineSide.OUTPUT), Arrays.asList(INWORK, ASSIGNED, TBD));

            boolean readyTasksExist = false;
            for (SubProductionTask t : tasksForMachine) {
                if (ProductTask.ProductState.INWORK.equals(t.getProductTask().getState())
                    && t.getPreConditionTasks().stream().allMatch(p -> (p.getState() == SUCCESS || p.getState() == SUCCESS_PENDING))) {

                    readyTasksExist = true;
                }
            }

            if (!readyTasksExist) {
                ProductOrder pOrder = new ProductOrder();
                pOrder.setId(System.currentTimeMillis());
                pOrder.setBaseColor(ProductOrder.BaseColor.BASE_BLACK);
                ProductTask pTask = new ProductTask();
                pTask.setState(ProductTask.ProductState.INWORK, "started cleanup task");
                pTask.setProductOrder(pOrder);
                List<SubProductionTask> subProductionTasks = subTaskGenerator.prepareRing(pOrder, m.getName(), subTaskGenerator.getRSForCapDispose(m.getName()), SubProductionTask.MachineSide.OUTPUT, false);
                subProductionTasks.stream().forEach(s -> s.setProductTask(pTask));

                productOrderDao.save(pOrder);
                productTaskDao.save(pTask);
                subProductionTaskDao.save(subProductionTasks);

                return subProductionTasks.get(0);
            }
        }

        return null;
    }


    private SubProductionTask findDemandedResourceTask() {

        List<SubProductionTask> subProductionTasks = resourceManager.generateDemandTasksIfRequired();

        if (subProductionTasks != null && subProductionTasks.size() > 0) {
            subProductionTaskDao.save(subProductionTasks);
            return subProductionTasks.get(0);
        } else {
            return null;
        }
    }

    private SubProductionTask findSameRobotTask(int robotId) {
        Collection<SubProductionTask> tasks = subProductionTaskDao.findByStateAndRobotId(SubProductionTask.TaskState.ASSIGNED, robotId);
        tasks = tasks.stream().filter(t -> ProductTask.ProductState.INWORK.equals(t.getProductTask().getState())).collect(Collectors.toList());
        if (tasks != null) {
            // find task with all pre-tasks done
            SubProductionTask readyToDo = null;
            for (SubProductionTask task : tasks) {
                boolean allDone = true;
                for (SubProductionTask pre : task.getPreConditionTasks()) {
                    if (pre.getState() != SubProductionTask.TaskState.SUCCESS) {
                        allDone = false;
                    }
                }

                if (allDone) {
                    readyToDo = task;
                }
            }

            if (readyToDo != null) {

                if (readyToDo.isBindDisposeRsOnAssignment()) {
                    // first find from which machine we got the product
                    Optional<SubProductionTask> oPickupFromCsTask = readyToDo.getPreConditionTasks().stream().filter(p -> p.getMachine().contains("CS") && p.getType() == SubProductionTask.TaskType.GET).findFirst();

                    if (oPickupFromCsTask.isPresent()) {
                        String colorprefix;
                        try {
                            colorprefix = gameStateDao.getTeamColor().equals(Config.TEAM_CYAN_COLOR) ? "C-" : "M-";
                        } catch (Exception e) {
                            System.out.println("Team color undefined! Setting default to CYAN!");
                            colorprefix = "C-";
                        }
                        String ringStationForCapDispose = subTaskGenerator.getRSForCapDispose(oPickupFromCsTask.get().getMachine());
                        if (ringStationForCapDispose == null) {
                            if (Math.random() >= 0.5) {
                                ringStationForCapDispose = colorprefix + "RS2";
                            } else {
                                ringStationForCapDispose = colorprefix + "RS1";
                            }
                        }

                        readyToDo.setMachine(ringStationForCapDispose);
                        readyToDo.setLockMachine(ringStationForCapDispose);
                        readyToDo.setUnlockMachine(ringStationForCapDispose);
                        readyToDo.setIncrementMachine(ringStationForCapDispose);
                        readyToDo.setSide(SubProductionTask.MachineSide.SLIDE);
                        subProductionTaskDao.save(readyToDo);
                    }
                }

                return readyToDo;
            }
        }

        return null;
    }

    private SubProductionTask findHighestPriorityTaskAllResourcesAvailableAndPredecessorsDone(List<SubProductionTask> tasks) {
        // sort in decreasing order of priority
        Comparator<SubProductionTask> taskPriorityComp = Comparator.comparingInt(SubProductionTask::getPriority);
        Collections.sort(tasks, taskPriorityComp);
        Collections.reverse(tasks);

        for (SubProductionTask t : tasks) {
            SubProductionTask successorTask = getSuccessorDeliverTask(t, TBD);
            if (successorTask == null) continue;
            if (successorTask.getDecrementMachine() == null || resourceManager.tryLockParts(successorTask.getDecrementMachine(), successorTask.getDecrementCost(), successorTask.getProductTask())) {
                // there are enough resources for this task to be fulfilled
                return t;
            } else {
                if (ProductTask.ProductState.INWORK.equals(t.getProductTask().getState())) {
                    System.out.println("No resources for Task: " + successorTask.getName() + ", required: " + successorTask.getDecrementCost());

                    // check if there are enough demands
                    resourceManager.ensureEnoughDemands(successorTask.getDecrementMachine(), successorTask.getDecrementCost(), successorTask.getProductTask());
                }
            }
        }
        return null;
    }

    private SubProductionTask findNextTaskInActiveProductTask(List<ProductTask> productsToCheck, int robotId) {
        if (productsToCheck.size() > 0) {
            // we have a product to finish
            List<SubProductionTask> tasksToDo = subProductionTaskDao.findByProductTaskInAndState(productsToCheck, TBD);
            tasksToDo = tasksToDo.stream().filter(t -> !resourceManager.isMachineLockedByOtherRobot(t.getLockMachine(), t.getType(), robotId)).collect(Collectors.toList());


            // if a task is still in SUCCESS_PENDING, it is done for the robot and we are waiting for the machine state
            // to determine the actual result. the GET task can already be given to the robot if there is no other product at the output
            // if there is more than one predecessor, this will not work!
            List<SubProductionTask> includeSuccessPending = tasksToDo.stream().filter(t -> t.getPreConditionTasks() != null
                    && t.getPreConditionTasks().size() == 1
                    && t.getPreConditionTasks().stream().allMatch(p -> SUCCESS_PENDING.equals(p.getState()))).collect(Collectors.toList());
            if (includeSuccessPending != null && includeSuccessPending.size() > 0) {
                ArrayList<SubProductionTask> maybeToDo = new ArrayList<>();
                for (SubProductionTask t : includeSuccessPending) {
                    Collection<SubProductionTask> otherGetTasks = subProductionTaskDao.findByMachineAndSideInAndStateIn(t.getMachine(), Arrays.asList(t.getSide()), Arrays.asList(TBD, INWORK));
                    long otherReadyCount = otherGetTasks.stream().filter(o -> o.getPreConditionTasks().stream().allMatch(p -> SUCCESS.equals(p.getState()))).count();
                    if (otherReadyCount == 0) {
                        maybeToDo.add(t);
                    }
                }

                SubProductionTask foundTask = findHighestPriorityTaskAllResourcesAvailableAndPredecessorsDone(maybeToDo);
                if (foundTask != null) {
                    return foundTask;
                }
            }


            // see if there is a task without any precondition still not done
            List<SubProductionTask> parentsToDo = tasksToDo.stream().filter(t -> (t.getPreConditionTasks() == null || t.getPreConditionTasks().size() == 0)
                    && (TBD.equals(t.getState()))).collect(Collectors.toList());

            SubProductionTask foundTask = findHighestPriorityTaskAllResourcesAvailableAndPredecessorsDone(parentsToDo);
            if (foundTask != null) {
                return foundTask;
            }

            // all parents appear to be done, select the next best task with all preconditions done
            // find task with all preconditions met, that is not done yet
            List<SubProductionTask> childsToDo = tasksToDo.stream().filter(t -> (TBD.equals(t.getState()) &&
                    (t.getPreConditionTasks().stream().filter(p -> (p.getState() != SubProductionTask.TaskState.SUCCESS)).count() == 0)))
                    .collect(Collectors.toList());

            foundTask = findHighestPriorityTaskAllResourcesAvailableAndPredecessorsDone(childsToDo);

            if (foundTask != null) {
                return foundTask;
            }
        }

        return null;
    }

    private SubProductionTask findNextTaskInActiveProduct(int robotId) {
        // check first if there is a task left for an unfinished product
        List<ProductTask> productsInWork = productTaskDao.findByState(ProductTask.ProductState.INWORK);
        return findNextTaskInActiveProductTask(productsInWork, robotId);
    }

    private SubProductionTask getSuccessorDeliverTask(SubProductionTask t) {
        if (t.getType() != SubProductionTask.TaskType.GET && t.getProductTask().getProductOrder().getComplexity() != ProductOrder.Complexity.D0) {
            System.err.println("Task is no GET Task in getSuccessorDeliverTask (" + t.getName() + ")");
            return null;
        }

        Collection<SubProductionTask> successors = subProductionTaskDao.findByPreConditionTasksIn(Lists.newArrayList(t));
        Optional<SubProductionTask> foundSuccesor = successors.stream().filter(s -> s.getType() == SubProductionTask.TaskType.DELIVER && s.getSameRobotSubTask().getId() == t.getId()).findAny();
        if (foundSuccesor.isPresent()) {
            return foundSuccesor.get();
        }

        return null;
    }

    private SubProductionTask getSuccessorDeliverTask(SubProductionTask t, SubProductionTask.TaskState state) {
        if (t.getType() != SubProductionTask.TaskType.GET && t.getProductTask().getProductOrder().getComplexity() != ProductOrder.Complexity.D0) {
            System.err.println("Task is no GET Task in getSuccessorDeliverTask (" + t.getName() + ")");
            return null;
        }

        Collection<SubProductionTask> successors = subProductionTaskDao.findByStateAndPreConditionTasksIn(state, Lists.newArrayList(t));
        Optional<SubProductionTask> foundSuccesor = successors.stream().filter(s -> s.getType() == SubProductionTask.TaskType.DELIVER && s.getSameRobotSubTask().getId() == t.getId()).findAny();
        if (foundSuccesor.isPresent()) {
            return foundSuccesor.get();
        }

        return null;
    }

    private void startNewProductIfFeasible(int robotId) {

        int countBlack = 0;
        int countGrey = 0;

        List<ProductTask> inwork = productTaskDao.findByStateAndProductOrderComplexityIn(ProductTask.ProductState.INWORK, Arrays.asList(ProductOrder.Complexity.C0, ProductOrder.Complexity.C1, ProductOrder.Complexity.C2, ProductOrder.Complexity.C3));
        int count = 0;
        for (ProductTask pTask : inwork) {
            if (pTask.getSubProductionTask().stream().filter(s -> s.getState() == TBD && s.getType() == SubProductionTask.TaskType.DELIVER && s.getMachine() != null && s.getMachine().contains("DS")).count() > 0) {
                count++;

                if (ProductOrder.CapColor.CAP_GREY.equals(pTask.getProductOrder().getCapColor())) {
                    countGrey++;
                } else {
                    countBlack++;
                }
            }
        }

        // do not start more than 3 products, but do not consider products that already are delivered
        // there we only do cleanup demand tasks
        if (count >= Config.MAX_SIMULTANEOUS_PRODUCTS) {
            return;
        }

        // if there is a competitive order, start it immediately
        List<ProductTask> competitiveOrders = productTaskDao.findByStateAndProductOrderCompetitive(ProductTask.ProductState.TBD, true);
        if (competitiveOrders != null && competitiveOrders.size() > 0) {
            tryStartProduct(competitiveOrders.get(0), robotId);
            return;
        }

        // the currently inwork products do not have parallel tasks that can be done
        // or no product currently in work, start a new one
        // we prefer products with simpler complexity
        List<ProductTask> notStartedProducts = productTaskDao.findByState(ProductTask.ProductState.TBD);

        List<ProductTask> complexInWork = productTaskDao.findByProductOrderComplexityInAndState(Arrays.asList(ProductOrder.Complexity.C2, ProductOrder.Complexity.C3), ProductTask.ProductState.INWORK);
        boolean areComplexInWork = false;
        if (complexInWork != null && complexInWork.size() > 0) {
            areComplexInWork = true;
        }

        areComplexInWork = false;

        Comparator<ProductOrder> capColorComparator;
        if (countGrey > countBlack) {
            capColorComparator = Comparator.comparing(ProductOrder::getCapColor).thenComparing(ProductOrder::getDeliveryPeriodEnd);
        } else {
            capColorComparator = Comparator.comparing(ProductOrder::getCapColor).reversed().thenComparing(ProductOrder::getDeliveryPeriodEnd);
        }

        // earliest deadline first, that is not passed
        List<ProductOrder> notStartedProductOrders = notStartedProducts.stream().map(p -> p.getProductOrder()).sorted(capColorComparator).collect(Collectors.toList());


        if (countBlack >= Config.MAX_SIMULTANEOUS_PRODUCTS_SAME_CAP) {
            notStartedProductOrders = notStartedProductOrders.stream().filter(p -> ProductOrder.CapColor.CAP_GREY.equals(p.getCapColor())).collect(Collectors.toList());
        }

        if (countGrey >= Config.MAX_SIMULTANEOUS_PRODUCTS_SAME_CAP) {
            notStartedProductOrders = notStartedProductOrders.stream().filter(p -> ProductOrder.CapColor.CAP_BLACK.equals(p.getCapColor())).collect(Collectors.toList());
        }


        for (ProductOrder pOrder : notStartedProductOrders) {

            if (pOrder.getComplexity() == ProductOrder.Complexity.C0 && !Config.DO_C0) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C1 && !Config.DO_C1) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C2 && !Config.DO_C2) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C3 && !Config.DO_C3) {
                continue;
            }

            if (areComplexInWork && (pOrder.getComplexity() == ProductOrder.Complexity.C2 || pOrder.getComplexity() == ProductOrder.Complexity.C3)) {
                // do not start a C2 together with a C3
                continue;
            }

            long estimatedProductDuration = 0;
            switch (pOrder.getComplexity()) {
                case C0: estimatedProductDuration = estimateC0; break;
                case C1: estimatedProductDuration = estimateC1; break;
                case C2: estimatedProductDuration = estimateC2; break;
                case C3: estimatedProductDuration = estimateC3; break;
            }

            // first look for earliest product for which the deadline has not already passed
            long currentGameTime = gameStateDao.getLatestGameTimeProductionPhase().longValue();
            long deliver = currentGameTime + (estimatedProductDuration*60L*1000000000L);
            if (deliver < pOrder.getDeliveryPeriodEnd()*1000000000L) {
                // we found product with earliest deadline that is not infeasible
                Optional<ProductTask> pTask = notStartedProducts.stream().filter(n -> n.getProductOrder().getId() == pOrder.getId()).findAny();
                if (pTask.isPresent()) {
                    if (tryStartProduct(pTask.get(), robotId) == true) {
                        return;
                    }
                }
            }
        }

        // if no product with a not-passed deadline was found, do any product, but only when there are no others
        if (count > 1) {
            return;
        }

        Collections.shuffle(notStartedProducts);
        for (ProductOrder pOrder : notStartedProductOrders) {

            if (pOrder.getComplexity() == ProductOrder.Complexity.C0 && !Config.DO_C0) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C1 && !Config.DO_C1) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C2 && !Config.DO_C2) {
                continue;
            }
            if (pOrder.getComplexity() == ProductOrder.Complexity.C3 && !Config.DO_C3) {
                continue;
            }

            if (areComplexInWork && (pOrder.getComplexity() == ProductOrder.Complexity.C2 || pOrder.getComplexity() == ProductOrder.Complexity.C3)) {
                // do not start a C2 together with a C3
                continue;
            }

            Optional<ProductTask> pTask = notStartedProducts.stream().filter(n -> n.getProductOrder().getId() == pOrder.getId()).findAny();
            if (pTask.isPresent()) {
                if (tryStartProduct(pTask.get(), robotId) == true) {
                    return;
                }
            }
        }
    }

    public boolean tryStartProduct(ProductTask pTask, int robotId) {
        pTask.setState(ProductTask.ProductState.INWORK, "product started by " + robotId);
        productTaskDao.save(pTask);

        String colorprefix;
        try {
            colorprefix = gameStateDao.getTeamColor().equals(Config.TEAM_CYAN_COLOR) ? "C-" : "M-";
        } catch (Exception e) {
            System.out.println("Team color undefined! Setting default to CYAN!");
            colorprefix = "C-";
        }

        String cs = pTask.getProductOrder().getCapColor() == ProductOrder.CapColor.CAP_GREY ? Config.GREY_CAP_MACHINE : Config.BLACK_CAP_MACHINE;
        resourceManager.addNewDemand(colorprefix + cs, 1, pTask);

        if (pTask.getProductOrder().getRing1() != null) {
            Ring r1 = ringDao.findByRingColor(pTask.getProductOrder().getRing1());
            if (r1.getRawMaterial() > 0) {
                resourceManager.addNewDemand(subTaskGenerator.getRingStationForColor(r1.getRingColor()), r1.getRawMaterial(), pTask);
            }
        }

        if (pTask.getProductOrder().getRing2() != null) {
            Ring r2 = ringDao.findByRingColor(pTask.getProductOrder().getRing2());
            if (r2.getRawMaterial() > 0) {
                resourceManager.addNewDemand(subTaskGenerator.getRingStationForColor(r2.getRingColor()), r2.getRawMaterial(), pTask);
            }
        }

        if (pTask.getProductOrder().getRing3() != null) {
            Ring r3 = ringDao.findByRingColor(pTask.getProductOrder().getRing3());
            if (r3.getRawMaterial() > 0) {
                resourceManager.addNewDemand(subTaskGenerator.getRingStationForColor(r3.getRingColor()), r3.getRawMaterial(), pTask);
            }
        }

        return true;
    }

    /*
    public void successTaskHandler(long subtaskId, int robotId) {

        //try {
            //_mutex.acquire();


            SubProductionTask subTask = subProductionTaskDao.findByIdAndRobotId(subtaskId, robotId);
            if (subTask == null) {
                System.err.println("Error, task that should succeed was not found! (ID: " + subtaskId + ")");
                //_mutex.release();
                return;
            }
            subTask.setState(SubProductionTask.TaskState.SUCCESS_PENDING, "set to SUCCESS_PENDING due to robot success");
            subProductionTaskDao.save(subTask);

            String machineStateOk = subTask.getMachineStateTaskOk();
            MachineInfoRefBox machineInfoRefBox = machineInfoRefBoxDao.findByName(subTask.getMachine());


            if (subTask.isPrepareRequired() && subTask.getType() == SubProductionTask.TaskType.DELIVER) {
                //the machineinstructionsender sets success as soon as the message is delivered
                synchronized (mpsHandler) {
                    mpsHandler.prepareMachine(subTask);
                }
            }

            if (machineInfoRefBox != null) {
                if (!StringUtils.isEmpty(machineStateOk) && machineStateOk.equalsIgnoreCase(machineInfoRefBox.getState())) {
                    // not used at the moment
                    //System.out.println("Task result successful is validated by machine state!");
                }

                if (subTask.getType() == SubProductionTask.TaskType.GET) {
                    subTask.setState(SubProductionTask.TaskState.SUCCESS, "TODO");
                } else if (subTask.getType() == SubProductionTask.TaskType.DELIVER) {
                    if (machineInfoRefBox.getState().equalsIgnoreCase(IDLE) || subTask.getSide() != SubProductionTask.MachineSide.INPUT) {
                        // only send success if machine is idle, that means nothing is in the machine already
                        // otherwise, the mpsinstructionsender will set success as well if the prepare was successful
                        subTask.setState(SubProductionTask.TaskState.SUCCESS, "TODO");
                    } else {
                        subTask.setState(SubProductionTask.TaskState.SUCCESS_PENDING, "TODO");
                    }
                    subProductionTaskDao.save(subTask);
                } else if (subTask.getType() == SubProductionTask.TaskType.DUMMY) {
                    subTask.setState(SUCCESS, "TODO");
                    subTask.getProductTask().setState(ProductTask.ProductState.FINISHED);
                    subProductionTaskDao.save(subTask);
                    productTaskDao.save(subTask.getProductTask());
                }
            }

            if (!StringUtils.isEmpty(subTask.getIncrementMachine())) {
                System.out.println("[RM] Increment machine is: " + subTask.getIncrementMachine());

                resourceManager.addNewParts(subTask.getIncrementMachine());
            }

            if (subTask.getDecrementCost() > 0) {
                System.out.println("[RM] Decrement machine is: " + subTask.getDecrementMachine());
                resourceManager.tryDecreaseParts(subTask.getDecrementMachine(), subTask.getDecrementCost(), subTask.getProductTask().getProductOrder().getId());
            }

            if (subTask.isDemandTaskWithOutDemand()) {
                resourceManager.fulfillDemandByExplorationLoad(subTask.getMachine());
            } else {
                resourceManager.fulfillDemand(subTask);
            }

            subTask.setEndTime(System.currentTimeMillis());
            subProductionTaskDao.save(subTask);

            boolean allSuccess = true;
            for (SubProductionTask st : subTask.getProductTask().getSubProductionTask()) {
                if (st.getState() != SubProductionTask.TaskState.SUCCESS) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                subTask.getProductTask().setState(ProductTask.ProductState.FINISHED);
                productTaskDao.save(subTask.getProductTask());
                //resourceManager.clearDemands(subTask.getProductTask());
            }
        //} catch (InterruptedException ie) {
        //    System.err.println("Error when acquiring mutex in successTaskHandler");
        //} finally {
            //_mutex.release();
        //}
    }
    */


    public void failProduct(SubProductionTask subTask, String logging) {
        if (subTask.isDemandTask() || SubProductionTask.TaskType.DUMMY.equals(subTask.getType())) {
            return;
        }

        subTask.getProductTask().setState(ProductTask.ProductState.FAILED, logging);
        productTaskDao.save(subTask.getProductTask());

        resourceManager.clearDemands(subTask.getProductTask());
        resourceManager.releasePartsForProduct(subTask.getProductTask());

        List<SubProductionTask> byProductTaskAndState = subProductionTaskDao.findByProductTaskAndState(subTask.getProductTask(), INWORK);
        for (SubProductionTask t : byProductTaskAndState) {
            rcm.cancelTask(t.getRobotId());
        }
    }

    private List<SubProductionTask> getOtherReadyGetTasks(SubProductionTask subTask) {
        List<SubProductionTask> possibleOtherGets = subProductionTaskDao.findByTypeAndMachineAndStateIn(SubProductionTask.TaskType.GET, subTask.getMachine(), Arrays.asList(INWORK, ASSIGNED, TBD));
        possibleOtherGets = possibleOtherGets.stream().filter(p -> p.getId() != subTask.getId()).collect(Collectors.toList());

        List<SubProductionTask> otherGets = new ArrayList<>();
        boolean otherGetReady = false;
        // for this tasks, check if the predecessor was done
        if (possibleOtherGets != null && possibleOtherGets.size() > 0) {
            for (SubProductionTask get : possibleOtherGets) {
                boolean allPreDone = get.getPreConditionTasks().stream().allMatch(p -> SUCCESS.equals(p.getState()));
                if (allPreDone) {
                    otherGets.add(get);
                }
            }
        }

        return otherGets;
    }

    /*
    public void failTaskHandler(long subtaskId, int robotId) {
        SubProductionTask subTask = subProductionTaskDao.findByIdAndRobotId(subtaskId, robotId);

        if (subTask == null) {
            System.err.println("Error, task that should fail was not found!");
            return;
        }

        // trivial case 1: dummy task is rolled back
        if (handleDummyTask(subTask)) return; // just fail dummy task and do no logic

        /*
        synchronized (mpsInstructionSender) {
            mpsInstructionSender.stopPreparingMachine(subTask);
        }
        */

        /*
        // trivial case 3: for demand tasks, clear demand-task assignment
        if (subTask.isDemandTask()) {
            SubProductionTask taskToClear = subTask;
            subTask.setState(FAILED, "demand task is FAILED");
            subProductionTaskDao.save(subTask);

            if (subTask.getType() == SubProductionTask.TaskType.GET) {
                taskToClear = getSuccessorDeliverTask(subTask, ASSIGNED);
            }

            if (taskToClear != null) {
                taskToClear.setState(FAILED, "demand task is FAILED");
                subProductionTaskDao.save(taskToClear);

                PartDemand pDemand = partDemandDao.findByTask(taskToClear);
                if (pDemand != null) {
                    pDemand.setTask(null);
                    partDemandDao.save(pDemand);
                }
            }

            // if a CS task fails, also the RS task needs to be reset
            Collection<SubProductionTask> tasks = subProductionTaskDao.findByStateAndPreConditionTasksIn(TBD, Arrays.asList(taskToClear));
            if (tasks != null && tasks.size() > 0) {
                SubProductionTask rsGet = tasks.iterator().next();
                if (rsGet != null) {
                    rsGet.setState(FAILED, "demand task is FAILED");
                    subProductionTaskDao.save(rsGet);
                }

                SubProductionTask rsDeliver = getSuccessorDeliverTask(rsGet, TBD);
                if (rsDeliver != null) {
                    rsDeliver.setState(FAILED, "demand task is FAILED");
                    subProductionTaskDao.save(rsDeliver);
                }

                PartDemand rsDemand = partDemandDao.findByTask(rsDeliver);

                if (rsDemand != null) {
                    rsDemand.setTask(null);
                    partDemandDao.save(rsDemand);
                }
            }

            return;
        }



        // beginning of not so trivial fails, we need to check machine states
        String machineState = getCurrentMachineState(subTask);


        if (SubProductionTask.TaskType.GET.equals(subTask.getType())) {
            handleGetFail(subTask, machineState);
        } else if (SubProductionTask.TaskType.DELIVER.equals(subTask.getType())) {
            handleDeliverFail(machineState);
        } else {
            System.err.println("Unsported Tasktype of failed task");
        }
    }

*/



    @NotNull
    private String getCurrentMachineState(SubProductionTask subTask) {
        MachineInfoRefBox mInfoRb = machineInfoRefBoxDao.findByName(subTask.getMachine());
        String machineState = IDLE;
        if (mInfoRb != null) {
            // if we are still in exploration, so the state is IDLE for sure
            machineState = mInfoRb.getState();
        }

        // wait with decision while unconclusive machine state
        int breakCounter = 0;
        while (!machineState.equalsIgnoreCase(IDLE)
                && !machineState.equalsIgnoreCase(READY_AT_OUTPUT)
                && !machineState.equalsIgnoreCase("BROKEN")
                && breakCounter < 100) {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("Sleep was interrupted in failTaskHandler");
            }

            mInfoRb = machineInfoRefBoxDao.findByName(subTask.getMachine());

            if (mInfoRb != null) {
                // if we are still in exploration, so the state is IDLE for sure
                machineState = mInfoRb.getState();
            }

            if (mInfoRb != null && !"DOWN".equalsIgnoreCase(machineState)) {
                breakCounter++;
            }

            System.out.println("Wait for machine " + subTask.getMachine() + " to become IDLE/READY-AT-OUTPUT/BROKEN");
        }
        return machineState;
    }

    private boolean handleDummyTask(SubProductionTask subTask) {
        if (subTask != null && subTask.getType() == SubProductionTask.TaskType.DUMMY) {
            subTask.setState(FAILED, "dummy task FAILED");
            subProductionTaskDao.save(subTask);
            failProduct(subTask, "dummy task FAILED");
            productTaskDao.save(subTask.getProductTask());
            return true;
        }
        return false;
    }

    public SubProductionTask failPreviousRobotActivityAndAssignTask(int robotId) {

        // we got no game phase info from refbox yet
	    if (gameStateDao.getLatestGameTimeProductionPhase() == null) {
            return null;
        }

        /*
        Collection<SubProductionTask> prevActiveTasks = subProductionTaskDao.findByStateAndRobotId(SubProductionTask.TaskState.INWORK, robotId);
	    if (prevActiveTasks != null && prevActiveTasks.size() > 0) {
	        for (SubProductionTask prevTask : prevActiveTasks) {
	            if (SubProductionTask.TaskType.GET.equals(prevTask.getType())) {
	                handleGetResult(prevTask.getId(), robotId, PrsTaskProtos.PrsTask.ExecutionResult.FAIL);
                } else if (SubProductionTask.TaskType.DELIVER.equals(prevTask.getType())) {
                    handleDeliverResult(prevTask.getId(), robotId, PrsTaskProtos.PrsTask.ExecutionResult.FAIL, false);
                }
            }
        }
        */

        SubProductionTask task = findSuitableTask(robotId);

        if (task != null && task.getMachine().contains("BS")) {
            bsSide = (bsSide == SubProductionTask.MachineSide.INPUT) ? SubProductionTask.MachineSide.OUTPUT : SubProductionTask.MachineSide.INPUT;
            task.setSide(bsSide);
            subProductionTaskDao.save(task);
        }

        return task;
    }

    public boolean checkProductionReady() {

        Collection<Ring> allRings = Lists.newArrayList(ringDao.findAll());
        if (allRings.size() != Config.NUM_RING_COLORS) {
            return false;
        }

        for (Ring r : allRings) {
            if (machineInfoRefBoxDao.countByRing1OrRing2(r.getRingColor(), r.getRingColor()) == 0) {
                return false;
            }
        }

        return true;
    }

    public boolean hasOrders() {
        if (productTaskDao.countByProductOrderComplexity(ProductOrder.Complexity.C0) > 0
                && productTaskDao.countByProductOrderComplexity(ProductOrder.Complexity.C1) > 0
                && (productTaskDao.countByProductOrderComplexity(ProductOrder.Complexity.C2) > 0
                        || productTaskDao.countByProductOrderComplexity(ProductOrder.Complexity.C3) > 0)) {
            return true;
        }

        return false;
    }

    private int getMountRingPoints(Ring ring) {
        switch (ring.getRawMaterial()) {
            case 0: return Config.POINTS_FINISH_CC0;
            case 1: return Config.POINTS_FINISH_CC1;
            case 2: return Config.POINTS_FINISH_CC2;

            default:
                System.err.println("Ring requires num: " + ring.getRawMaterial() + " additional bases which we did not expect"); return 0;
        }
    }

    public int estimatePoints(ProductTask pTask) {
        int totalPoints = 0;

        // correct points for mounting the first ring
        ProductOrder.RingColor ring1color = pTask.getProductOrder().getRing1();
        if (ring1color != null) {
            Ring ring1config = ringDao.findByRingColor(ring1color);
            totalPoints += getMountRingPoints(ring1config);
        }

        // correct points for mounting the second ring
        ProductOrder.RingColor ring2color = pTask.getProductOrder().getRing2();
        if (ring2color != null) {
            Ring ring2config = ringDao.findByRingColor(ring2color);
            totalPoints += getMountRingPoints(ring2config);
        }

        // correct points for mounting the third ring
        ProductOrder.RingColor ring3color = pTask.getProductOrder().getRing3();
        if (ring3color != null) {
            Ring ring3config = ringDao.findByRingColor(ring3color);
            totalPoints += getMountRingPoints(ring3config);
        }

        // points for mounting the last ring of a product of complexity higher than C0
        switch (pTask.getProductOrder().getComplexity()) {
            case C1: totalPoints += Config.POINTS_LAST_RING_C1; break;
            case C2: totalPoints += Config.POINTS_LAST_RING_C2; break;
            case C3: totalPoints += Config.POINTS_LAST_RING_C3; break;
        }

        // we always get points for mounting the cap
        totalPoints += Config.POINTS_MOUNT_CAP;

        // we always get points for delivering the product
        totalPoints += Config.POINTS_IN_TIME_DELIVERY;

        return totalPoints;
    }

    public void machineBroken(String name) {

    }

    public void handleGetResult(long taskId, int robotId, PrsTaskProtos.PrsTask.ExecutionResult executionResult) {
        SubProductionTask subTask = subProductionTaskDao.findByIdAndRobotId(taskId, robotId);

        if (subTask == null) {
            System.err.println("Error, task that should fail was not found!");
            return;
        }

        if (PrsTaskProtos.PrsTask.ExecutionResult.SUCCESS.equals(executionResult)) {
            subTask.setState(SUCCESS, "robot said SUCCESS");
            subProductionTaskDao.save(subTask);
        } else if (PrsTaskProtos.PrsTask.ExecutionResult.FAIL.equals(executionResult)) {

            if (subTask.getMachine().contains("BS")) {
                handleBsGetFail(subTask);

            } else if (subTask.getMachine().contains("CS") || subTask.getMachine().contains("RS")) {
                handleCsRsGetFail(subTask);

            } else {
                System.err.println("GET task at unexpected machine " + subTask.getMachine() + " taskId: " + taskId);
            }
        } else {
            System.err.println("Unexpected task result: " + executionResult + " for task: " + taskId);
        }
    }

    private void handleCsRsGetFail(SubProductionTask subTask) {
        if (SubProductionTask.MachineSide.SHELF.equals(subTask.getSide())) {
            clearTasksInDemand(subTask);
            failTaskAndSuccessor(subTask, "get at SHELF failed");
        } else if (SubProductionTask.MachineSide.OUTPUT.equals(subTask.getSide())) {
            String currentMachineState = getCurrentMachineState(subTask);

            if (READY_AT_OUTPUT.equalsIgnoreCase(currentMachineState)) {
                updateTaskAndSuccessorStates(subTask, TBD, "task failed but machine still ready-at-output");
            } else if (IDLE.equalsIgnoreCase(currentMachineState)) {

                failTaskAndSuccessor(subTask, "GET failed and machine is IDLE we probably pushed product in machine");
                Collection<SubProductionTask> successPendingAtInput = subProductionTaskDao.findByMachineAndSideInAndStateIn(subTask.getMachine(), Arrays.asList(SubProductionTask.MachineSide.INPUT), Arrays.asList(SUCCESS_PENDING));
                if (successPendingAtInput == null || successPendingAtInput.size() == 0) {
                    // there is no other product at input but we failed, so to be sure: reset mps (we do not impact other product at input)
                    resetMPSSender.sendResetMachine(subTask);
                    mpsHandler.stopPreparingMachine(subTask);

                    // empty machine parts
                    resourceManager.clearAllParts(subTask.getMachine());
                }

                // clear demand or fail product in any case
                if (subTask.isDemandTask()) {
                    clearTasksInDemand(subTask);
                } else {
                    failProduct(subTask, "get product failed and product not at machine");
                }
            } else {
                System.err.println("Unexpected machinestate " + currentMachineState + " at machine " + subTask.getMachine());
            }
        } else {
            System.err.println("GET task FAILED for unexpected side " + subTask.getSide() + " at machine " + subTask.getMachine());
        }
    }

    private void handleBsGetFail(SubProductionTask subTask) {
        resetMPSSender.sendResetMachine(subTask);
        mpsHandler.stopPreparingMachine(subTask);
        if (!subTask.isDemandTask()) {
            failTaskAndSuccessor(subTask, "get at BS failed");
            failProduct(subTask, "get at BS failed");
        } else {
            clearTasksInDemand(subTask);
            failTaskAndSuccessor(subTask, "get at BS failed");
        }
    }

    private void clearTasksInDemand(SubProductionTask subTask) {
        SubProductionTask taskToClear = subTask;
        if (SubProductionTask.TaskType.GET.equals(subTask.getType())) {
            taskToClear = getSuccessorDeliverTask(subTask);
        }

        if (taskToClear == null) {
            System.err.println("Task to clear was null!!! SubTask: " + subTask.getId());
        }

        PartDemand byTask = partDemandDao.findByTask(taskToClear);
        if (byTask != null) {
            byTask.setTask(null);
            partDemandDao.save(byTask);
        }

        // if a CS task fails, also the RS task needs to be reset
        Collection<SubProductionTask> tasks = subProductionTaskDao.findByStateAndPreConditionTasksIn(TBD, Arrays.asList(taskToClear));
        if (tasks != null && tasks.size() > 0) {
            SubProductionTask rsGet = tasks.iterator().next();
            if (rsGet != null) {
                rsGet.setState(FAILED, "demand task is FAILED");
                subProductionTaskDao.save(rsGet);
            }

            SubProductionTask rsDeliver = getSuccessorDeliverTask(rsGet, TBD);
            if (rsDeliver != null) {
                rsDeliver.setState(FAILED, "demand task is FAILED");
                subProductionTaskDao.save(rsDeliver);
            }

            PartDemand rsDemand = partDemandDao.findByTask(rsDeliver);

            if (rsDemand != null) {
                rsDemand.setTask(null);
                partDemandDao.save(rsDemand);
            }
        }
    }

    private void updateTaskAndSuccessorStates(SubProductionTask subTask, SubProductionTask.TaskState taskState, String logging) {
        subTask.setState(taskState, logging);
        subProductionTaskDao.save(subTask);
        SubProductionTask successor = getSuccessorDeliverTask(subTask, ASSIGNED);
        if (successor != null) {
            successor.setState(taskState, logging);
            subProductionTaskDao.save(successor);
        }

        SubProductionTask successorGet = getSuccessorGetTask(subTask, INWORK);
        if (successorGet != null && taskState == FAILED) {
            rcm.cancelTask(successorGet.getRobotId());
            failTaskAndSuccessor(successorGet, logging);
        }
    }

    private SubProductionTask getSuccessorGetTask(SubProductionTask subTask, SubProductionTask.TaskState state) {
        Collection<SubProductionTask> successors = subProductionTaskDao.findByStateAndPreConditionTasksIn(state, Lists.newArrayList(subTask));
        Optional<SubProductionTask> foundSuccesor = successors.stream().filter(s -> s.getType() == SubProductionTask.TaskType.GET).findAny();
        if (foundSuccesor.isPresent()) {
            return foundSuccesor.get();
        }

        return null;
    }

    private void failTaskAndSuccessor(SubProductionTask subTask, String logging) {
        updateTaskAndSuccessorStates(subTask, FAILED, logging);
    }

    public void handleDeliverResult(long taskId, int robotId, PrsTaskProtos.PrsTask.ExecutionResult executionResult, boolean holdProduct) {
        SubProductionTask subTask = subProductionTaskDao.findByIdAndRobotId(taskId, robotId);

        if (subTask == null) {
            System.err.println("Error, task that should fail was not found!");
            return;
        }

        if (PrsTaskProtos.PrsTask.ExecutionResult.SUCCESS.equals(executionResult)
                || (PrsTaskProtos.PrsTask.ExecutionResult.FAIL.equals(executionResult) && !holdProduct)) {

            subTask.setState(SUCCESS_PENDING, "robot said task was SUCCESS");
            subProductionTaskDao.save(subTask);

            if (subTask.getMachine().contains("RS") && ProductOrder.Complexity.E0.equals(subTask.getProductTask().getProductOrder().getComplexity())) {
                successProductTask(subTask);
            }

            if (subTask.isPrepareRequired()) {
                //the machineinstructionsender sets success as soon as the message is delivered
                synchronized (mpsHandler) {
                    mpsHandler.prepareMachine(subTask);
                }

                if (subTask.getMachine().contains("DS")) {
                    subTask.setState(SUCCESS, "deliver was done");
                    subProductionTaskDao.save(subTask);

                    successProductTask(subTask);
                } else {
                    //updateTaskStateBasedOnMachineState(subTask);

                    // is updated as soon as MPSHandler receives update
                }
            } else {
                // no prepare required
                if (subTask.getMachine().contains("RS") && SubProductionTask.MachineSide.SLIDE.equals(subTask.getSide())) {
                    subTask.setState(SUCCESS, "deliver was done to slide");
                    subProductionTaskDao.save(subTask);
                    handleResourcesOnSuccess(subTask);
                }
            }
        } else if (PrsTaskProtos.PrsTask.ExecutionResult.FAIL.equals(executionResult) && holdProduct) {
            subTask.setState(ASSIGNED, "reasigned due to FAIL from robot + holdProduct == true");
            subProductionTaskDao.save(subTask);
        } else {
            System.err.println("Unexpected task result: " + executionResult + " for task: " + taskId + " holdProduct: " + holdProduct);
        }
    }

    private void successProductTask(SubProductionTask subTask) {
        resourceManager.clearDemands(subTask.getProductTask());
        subTask.getProductTask().setState(ProductTask.ProductState.FINISHED, "deliver was done");
        productTaskDao.save(subTask.getProductTask());
    }

    public void updateTaskStateBasedOnMachineState(SubProductionTask subTask) {
        int breakCounter = 0;
        String machineState = mpsHandler.getMachineInfo(subTask).getState();
        while (!mpsHandler.isTaskPrepareDone(subTask) &&
               !READY_AT_OUTPUT.equalsIgnoreCase(machineState) &&
               !IDLE.equalsIgnoreCase(machineState) &&
               !BROKEN.equalsIgnoreCase(machineState) &&
                breakCounter < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Wait for " + subTask.getMachine() + " to report READY-AT-OUTPUT");

            if (!DOWN.equalsIgnoreCase(machineState)) {
                breakCounter++;
            }

            machineState = mpsHandler.getMachineInfo(subTask).getState();
        }

        if (READY_AT_OUTPUT.equalsIgnoreCase(machineState)) {
            subTask.setState(SUCCESS, "machine said READY-AT-OUTPUT");
            subTask.setEndTime(System.currentTimeMillis());
            subProductionTaskDao.save(subTask);
            handleResourcesOnSuccess(subTask);
        } else {
            // we broke something, reset machine
            resetMPSSender.sendResetMachine(subTask);
            mpsHandler.stopPreparingMachine(subTask);

            failTaskAndSuccessor(subTask, "deliver caused machine to go into state " + machineState);
            //subTask.setState(FAILED, "deliver caused machine to go into state " + machineState);
            subTask.setEndTime(System.currentTimeMillis());
            subProductionTaskDao.save(subTask);

            if (subTask.isDemandTask()) {
                clearTasksInDemand(subTask);
            } else {
                failProduct(subTask, "deliver caused machine to go into state " + machineState);
            }
        }
    }

    private void handleResourcesOnSuccess(SubProductionTask subTask) {
        if (!StringUtils.isEmpty(subTask.getIncrementMachine())) {
            System.out.println("[RM] Increment machine is: " + subTask.getIncrementMachine());

            resourceManager.addNewParts(subTask.getIncrementMachine());
        }

        if (subTask.getDecrementCost() > 0) {
            System.out.println("[RM] Decrement machine is: " + subTask.getDecrementMachine());
            resourceManager.tryDecreaseParts(subTask.getDecrementMachine(), subTask.getDecrementCost(), subTask.getProductTask().getProductOrder().getId());
        }

        /*
        if (subTask.isDemandTaskWithOutDemand()) {
            resourceManager.fulfillDemandByExplorationLoad(subTask.getMachine());
        } else {
            resourceManager.fulfillDemand(subTask);
        }
        */
    }

    public void failOtherInWorkTasks(int robotId, long taskId) {
        List<SubProductionTask> robotTaks = subProductionTaskDao.findByRobotIdAndState(robotId, INWORK);
        for (SubProductionTask t : robotTaks) {
            if (t.getId() != taskId) {
                failTaskAndSuccessor(t, "Robot already is working on another task with ID: " + taskId);
            }
        }
    }
}
