package com.grips.protobuf_lib;

import com.google.common.collect.Lists;
import com.grips.model.scheduler.ProductTask;
import com.grips.model.scheduler.SubProductionTask;
import com.grips.model.scheduler.SubProductionTask.TaskState;
import com.grips.model.scheduler.SubProductionTaskBuilder;
import com.grips.model.teamserver.MachineInfoRefBox;
import com.grips.model.teamserver.ProductOrder;
import com.grips.model.teamserver.ProductOrder.CapColor;
import com.grips.model.teamserver.ProductOrder.RingColor;
import com.grips.model.teamserver.Ring;
import com.grips.model.teamserver.dao.GameStateDao;
import com.grips.model.teamserver.dao.MachineInfoRefBoxDao;
import com.grips.model.teamserver.dao.ProductTaskDao;
import com.grips.model.teamserver.dao.RingDao;
import com.grips.scheduler.ProductionScheduler;
import com.grips.tools.PathEstimator;
import org.robocup_logistics.llsf_msgs.MachineDescriptionProtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class SubTaskGenerator {

	@Autowired
	private RingDao ringDao;

	@Autowired
	private MachineInfoRefBoxDao machineInfoRefBoxDao;

	@Autowired
	private GameStateDao gameStateDao;

	@Autowired
    private PathEstimator pathEstimator;

	@Autowired
	private ProductTaskDao productTaskDao;

	@Autowired
	private ProductionScheduler productionScheduler;

	@Autowired
    private ResourceManager resourceManager;

	private String colorprefix;

	private static String TASK_STATE_BROKEN = "BROKEN";
	private static String TASK_STATE_IDLE = "IDLE";
	private static String TASK_STATE_READY_AT_OUTPUT = "READY-AT-OUTPUT";

	public void generateSubTasks(ProductOrder productOrder) {
		String teamcolor = gameStateDao.getTeamColor();
		long alreadyDelivered = 0;
		if (teamcolor.startsWith("C")) {
			alreadyDelivered = productOrder.getQuantityDeliveredCyan();
		} else if (teamcolor.startsWith("M")) {
			alreadyDelivered = productOrder.getQuantityDeliveredMagenta();
		}

		if (alreadyDelivered == productOrder.getQuantityRequested()) {
			//System.out.println("Refbox says we already delivered this product!");
			// refbox says we already delivered
			return;
		}

		//TODO: what if there was only 1 productTask but the order contains 2 products?!?!?!?!?!
		int productTasksInDb = productTaskDao.findByProductOrderAndStateNot(productOrder, ProductTask.ProductState.FAILED).size();
		int productTasksSuccess = productTaskDao.findByProductOrderAndState(productOrder, ProductTask.ProductState.FINISHED).size();

		if (productTasksInDb >= productOrder.getQuantityRequested()) {
			// we already generated tasks for this order
			if (productTasksSuccess == productTasksInDb && alreadyDelivered != productOrder.getQuantityRequested()) {
				// refbox has not said we delivered, so do not skip
			} else {
				return;
			}
		}

		try {
			colorprefix = gameStateDao.getTeamColor().equals(Config.TEAM_CYAN_COLOR) ? "C-" : "M-";
		} catch (Exception e) {
			System.out.println("Team color undefined! Setting default to CYAN!");
			colorprefix = "C-";
		}

		String capStation = colorprefix + (productOrder.getCapColor() == CapColor.CAP_GREY ? Config.GREY_CAP_MACHINE : Config.BLACK_CAP_MACHINE);
		String baseStation = colorprefix + "BS";
		String deliveryStation = colorprefix + "DS";

		long productsToGenerate = productOrder.getQuantityRequested() - productTasksInDb;
		for(long i = 0; i < productsToGenerate; i++) {
			ProductTask productTask = new ProductTask();
			productTask.setProductOrder(productOrder);

			productTask.setState(ProductTask.ProductState.TBD, "initially set to TBD");
			List<SubProductionTask> subtasks = new ArrayList<>();


			// fetch standing C0 from SS
			if (Config.DO_STANDING_C0_SS && ProductOrder.Complexity.C0.equals(productOrder.getComplexity()) && productOrder.getDeliveryPeriodEnd() == 17L*60L /* 17 min in s */) {
				subtasks.addAll(fetchFromSs(productOrder, colorprefix));
			} else {
				List<SubProductionTask> mntRings = mountRing(productOrder, baseStation, Collections.emptySet());
				subtasks.addAll(mntRings);

				String pickupStationForCapMount = null;
				if (mntRings.size() > 0) {
					pickupStationForCapMount = mntRings.get(mntRings.size() - 1).getMachine();
				}

				if (mntRings.size() > 0) {
					subtasks.addAll(mountCapAndDeliverProduct(productOrder, baseStation, capStation, deliveryStation, pickupStationForCapMount, Collections.singleton(mntRings.get((mntRings.size() - 1)))));
				} else {
					subtasks.addAll(mountCapAndDeliverProduct(productOrder, baseStation, capStation, deliveryStation, pickupStationForCapMount, Collections.emptySet()));
				}
			}

			productTask.setSubProductionTasks(subtasks);
			for (SubProductionTask task : subtasks) {
				task.setProductTask(productTask);
			}

			productTaskDao.save(productTask);
		}
	}

	private List<SubProductionTask> fetchFromSs(ProductOrder productOrder, String colorprefix) {
		List<SubProductionTask> tasks = new ArrayList<>();

		String ss = colorprefix + "SS";
		String ds = colorprefix + "DS";
		SubProductionTask getProdFromSS = SubProductionTaskBuilder.newBuilder()
				.setPreConditionTasks(null)
				.setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromSS")
				.setMachine(ss)
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.GET)
				.setSide(SubProductionTask.MachineSide.OUTPUT)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(ss)
				.setUnlockMachine(ss)
				.setIncrementMachine(null)
				.setDecrementMachine(null)
				.setSameRobotSubTask(null)
				.setRequiredColor(null)
				.setOptCode(MachineDescriptionProtos.SSOp.RETRIEVE.toString())
				.setPrepareRequired(true)
				.setFatal(true)
				.setRequiresReset(true)
				.setPriority(Config.PRIORITY_DELIVER_C0)
				.build();
		tasks.add(getProdFromSS);

		SubProductionTask deliverBaseToDS = SubProductionTaskBuilder.newBuilder()
				.setName("[" + productOrder.getComplexity() + "] DeliverBaseToDS")
				.setPreConditionTasks(Collections.singleton(getProdFromSS))
				.setMachine(ds)
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.DELIVER)
				.setSide(SubProductionTask.MachineSide.INPUT)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(ds)
				.setUnlockMachine(ds)
				.setIncrementMachine(null)
				.setDecrementMachine(null)
				.setSameRobotSubTask(getProdFromSS)
				.setRequiredColor(null)
				.setOptCode(productOrder.getDeliveryGate()+"")
				.setPrepareRequired(true)
				.setFatal(true)
				.setRequiresReset(false)
				.build();
		tasks.add(deliverBaseToDS);

		return tasks;
	}

	private List<SubProductionTask> mountRing(ProductOrder productOrder, String baseStation, Set<SubProductionTask> preconds) {

		List<SubProductionTask> tasks = new ArrayList<>();

		String ringStation1 = null, ringStation2 = null, ringStation3 = null;

		if (productOrder.getRing1() != null) {
			int decrementCount = ringDao.findByRingColor(productOrder.getRing1()).getRawMaterial();

			SubProductionTask getBaseFromBS = SubProductionTaskBuilder.newBuilder()
												.setPreConditionTasks(preconds)
												.setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromBS")
												.setMachine(baseStation)
												.setState(TaskState.TBD)
												.setType(SubProductionTask.TaskType.GET)
												.setSide(SubProductionTask.MachineSide.OUTPUT)
												.setOrderInfoId(productOrder.getId())
												.setLockMachine(baseStation)
												.setUnlockMachine(baseStation)
												.setIncrementMachine(null)
												.setDecrementMachine(null)
												.setSameRobotSubTask(null)
												.setRequiredColor(productOrder.getBaseColor().toString())
												.setOptCode(null)
												.setPrepareRequired(true)
												.setFatal(true)
												.setRequiresReset(true)
                                                .setPriority(Config.PRIORITY_FIRST_RING)
										.build();
			tasks.add(getBaseFromBS);

			ringStation1 = getRingStationForColor(productOrder.getRing1());

			SubProductionTask deliverBaseToRS1 = SubProductionTaskBuilder.newBuilder()
                                                .setPreConditionTasks(Collections.singleton(getBaseFromBS))
                                                .setName("[" + productOrder.getComplexity().toString() + "] DeliverBaseToRS")
                                                .setMachine(ringStation1)
                                                .setState(TaskState.TBD)
                                                .setType(SubProductionTask.TaskType.DELIVER)
                                                .setSide(SubProductionTask.MachineSide.INPUT)
                                                .setOrderInfoId(productOrder.getId())
                                                .setLockMachine(ringStation1)
                                                .setUnlockMachine(ringStation1)
                                                .setDecrementMachine(ringStation1)
                                                .setDecrementCost(decrementCount)
                                                .setSameRobotSubTask(getBaseFromBS)
                                                .setRequiredColor(productOrder.getRing1().toString())
                                                .setPrepareRequired(true)
                                                .setFatal(true)
                                                .setRequiresReset(false)
                                            .build();
			tasks.add(deliverBaseToRS1);


			if (productOrder.getRing2() != null) {

				decrementCount = ringDao.findByRingColor(productOrder.getRing2()).getRawMaterial();

				SubProductionTask getBaseFromRS1 = SubProductionTaskBuilder.newBuilder()
						.setPreConditionTasks(Collections.singleton(deliverBaseToRS1))
						.setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromRS")
						.setMachine(ringStation1)
						.setState(TaskState.TBD)
						.setType(SubProductionTask.TaskType.GET)
						.setSide(SubProductionTask.MachineSide.OUTPUT)
						.setOrderInfoId(productOrder.getId())
						.setLockMachine(ringStation1)
						.setUnlockMachine(ringStation1)
						.setPrepareRequired(false)
						.setFatal(true)
						.setRequiresReset(true)
                        .setPriority(Config.PRIORITY_SECOND_RING)
						.build();

				tasks.add(getBaseFromRS1);

				ringStation2 = getRingStationForColor(productOrder.getRing2());

				SubProductionTask deliverBaseToRS2 = SubProductionTaskBuilder.newBuilder()
						.setPreConditionTasks(Collections.singleton(getBaseFromRS1))
						.setName("[" + productOrder.getComplexity().toString() + "] DeliverBaseToRS")
						.setMachine(ringStation2)
						.setState(TaskState.TBD)
						.setType(SubProductionTask.TaskType.DELIVER)
						.setSide(SubProductionTask.MachineSide.INPUT)
						.setOrderInfoId(productOrder.getId())
						.setLockMachine(ringStation2)
						.setUnlockMachine(ringStation2)
						.setDecrementMachine(ringStation2)
                        .setDecrementCost(decrementCount)
						.setSameRobotSubTask(getBaseFromRS1)
						.setRequiredColor(productOrder.getRing2().toString())
						.setPrepareRequired(true)
						.setFatal(true)
						.setRequiresReset(true)
						.build();

				tasks.add(deliverBaseToRS2);


				if (productOrder.getRing3() != null) {

					decrementCount = ringDao.findByRingColor(productOrder.getRing3()).getRawMaterial();

					SubProductionTask getBaseFromRS2 = SubProductionTaskBuilder.newBuilder()
							.setPreConditionTasks(Collections.singleton(deliverBaseToRS2))
							.setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromRS")
							.setMachine(ringStation2)
							.setState(TaskState.TBD)
							.setType(SubProductionTask.TaskType.GET)
							.setSide(SubProductionTask.MachineSide.OUTPUT)
							.setOrderInfoId(productOrder.getId())
							.setLockMachine(ringStation2)
							.setUnlockMachine(ringStation2)
							.setPrepareRequired(false)
							.setFatal(true)
							.setRequiresReset(true)
                            .setPriority(Config.PRIORITY_THIRD_RING)
							.build();

					tasks.add(getBaseFromRS2);

					ringStation3 = getRingStationForColor(productOrder.getRing3());

					SubProductionTask deliverBaseToRS3 = SubProductionTaskBuilder.newBuilder()
							.setPreConditionTasks(Collections.singleton(getBaseFromRS2))
							.setName("[" + productOrder.getComplexity().toString() + "] DeliverBaseToRS")
							.setMachine(ringStation3)
							.setState(TaskState.TBD)
							.setType(SubProductionTask.TaskType.DELIVER)
							.setSide(SubProductionTask.MachineSide.INPUT)
							.setOrderInfoId(productOrder.getId())
							.setLockMachine(ringStation3)
							.setUnlockMachine(ringStation3)
							.setIncrementMachine(null)
							.setDecrementMachine(ringStation3)
                            .setDecrementCost(decrementCount)
							.setSameRobotSubTask(getBaseFromRS2)
							.setRequiredColor(productOrder.getRing3().toString())
							.setPrepareRequired(true)
							.setFatal(true)
							.setRequiresReset(true)
							.build();

					tasks.add(deliverBaseToRS3);
				}
			}
		}

		return tasks;
	}

	public List<SubProductionTask> prepareRing(ProductOrder productOrder, String pickupStation, String ringStation, SubProductionTask.MachineSide side, boolean prepareRequired) {
		List<SubProductionTask> tasks = new ArrayList<>();

		SubProductionTask getBaseFromStation = SubProductionTaskBuilder.newBuilder()
				.setName("[" + ProductOrder.Complexity.D1.toString() + "] GetBaseFromStation")
				.setPreConditionTasks(Collections.emptySet())
				.setMachine(pickupStation)
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.GET)
				.setSide(side)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(pickupStation)
				.setUnlockMachine(pickupStation)
				.setPrepareRequired(prepareRequired)
                .setRequiredColor(productOrder.getBaseColor().toString())
				.setFatal(true)
				.setRequiresReset(prepareRequired)
				.setIsDemandTask(true)
				.build();
		tasks.add(getBaseFromStation);

		SubProductionTask deliverBaseToRS = SubProductionTaskBuilder.newBuilder()
				.setName("[" + ProductOrder.Complexity.D1.toString() + "] DeliverBaseToRS")
				.setMachine(ringStation)
				.setPreConditionTasks(Collections.singleton(getBaseFromStation))
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.DELIVER)
				.setSide(SubProductionTask.MachineSide.SLIDE)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(ringStation)
				.setUnlockMachine(ringStation)
				.setIncrementMachine(ringStation)
				.setSameRobotSubTask(getBaseFromStation)
				.setPrepareRequired(false)
				.setFatal(true)
				.setRequiresReset(false)
				.setIsDemandTask(true)
				.build();

		tasks.add(deliverBaseToRS);
		return tasks;
	}


	public String getRSForCapDispose(String capStation) {
		ArrayList<Ring> rings = Lists.newArrayList(ringDao.findAll());

        String disposalRs = null;
        String alternateDisposalRs = null;
		pathEstimator.calculateShortestDisposalMachines();
        if (capStation.contains("CS1")) {
            disposalRs = pathEstimator.getDisposalCS1();
            alternateDisposalRs = pathEstimator.getDisposalCS2();
        } else if (capStation.contains("CS2")) {
            disposalRs = pathEstimator.getDisposalCS2();
            alternateDisposalRs = pathEstimator.getDisposalCS1();
        } else {
            System.err.println("Requested disposal RS for machine " + capStation + " which is invalid!");
        }

		if (rings == null || rings.isEmpty()) return disposalRs; // we do not have ring information yet, so we cannot
																// check if bases are required

        MachineInfoRefBox disposalRsInfo = machineInfoRefBoxDao.findByName(disposalRs);

        if (disposalRsInfo == null) return disposalRs;

        int rawSum = rings.stream().filter(r -> r.getRingColor().equals(disposalRsInfo.getRing1()) || r.getRingColor().equals(disposalRsInfo.getRing2())).mapToInt(r -> r.getRawMaterial()).sum();
        if (rawSum == 0) {
            // preferred disposal machine does not need any bases, so use other ringstation
            return alternateDisposalRs;
        }

        if (resourceManager.getMaterialCount(disposalRs) >= 3) {
        	if (resourceManager.getMaterialCount(alternateDisposalRs) < 3) {
        		return alternateDisposalRs;
			}
		}

        return disposalRs;
	}

    public List<SubProductionTask> prepareCapAssignDisposeLater(ProductOrder productOrder, String capStation) {
        return prepareCapAssignDispose(productOrder, capStation, null, false, null);
    }

	public List<SubProductionTask> prepareCapAssignDispose(ProductOrder productOrder, String capStation, String disposeStation, boolean prepareRequired, SubProductionTask.MachineSide side) {
		List<SubProductionTask> subtasks = new ArrayList<>();

		SubProductionTask getBaseFromShelf = SubProductionTaskBuilder.newBuilder()
				.setName("[" + ProductOrder.Complexity.D2.toString() + "] GetBaseFromShelf")
				.setPreConditionTasks(null)
				.setMachine(capStation)
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.GET)
				.setSide(SubProductionTask.MachineSide.SHELF)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(capStation)
				.setPrepareRequired(false)
				.setFatal(false)
				.setRequiresReset(false)
				.setIsDemandTask(true)
				.build();

		subtasks.add(getBaseFromShelf);


		SubProductionTask deliverBaseToCS = SubProductionTaskBuilder.newBuilder()
				.setName("[" + ProductOrder.Complexity.D2.toString() + "] DeliverBaseToCS")
				.setPreConditionTasks(Collections.singleton(getBaseFromShelf))
				.setMachine(capStation)
				.setState(TaskState.TBD)
				.setType(SubProductionTask.TaskType.DELIVER)
				.setSide(SubProductionTask.MachineSide.INPUT)
				.setOrderInfoId(productOrder.getId())
				.setLockMachine(capStation)
				.setIncrementMachine(capStation)
				.setSameRobotSubTask(getBaseFromShelf)
				.setOptCode(MachineDescriptionProtos.CSOp.RETRIEVE_CAP.toString())
				.setPrepareRequired(true)
				.setFatal(false)
				.setRequiresReset(false)
				.setMachineStateTaskReasign(TASK_STATE_IDLE)
				.setMachineStateTaskOk(TASK_STATE_READY_AT_OUTPUT)
				.setIsDemandTask(true)
			.build();
		subtasks.add(deliverBaseToCS);


		SubProductionTask getBaseFromCS = SubProductionTaskBuilder.newBuilder()
                .setName("[" + ProductOrder.Complexity.D2.toString() + "] GetBaseFromCS")
                .setPreConditionTasks(Collections.singleton(deliverBaseToCS))
                .setMachine(capStation)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.GET)
                .setSide(SubProductionTask.MachineSide.OUTPUT)
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(capStation)
                .setUnlockMachine(capStation)
                .setSameRobotSubTask(null)
                .setPrepareRequired(false)
                .setFatal(false)
                .setRequiresReset(true)
                .setBindRsOnAssignment(false)
                .setMachineStateTaskReasign(TASK_STATE_READY_AT_OUTPUT)
                .setMachineStateTaskOk(TASK_STATE_IDLE)
				.setIsDemandTask(true)
            .build();
     	subtasks.add(getBaseFromCS);

		SubProductionTask deliverBaseToRS = SubProductionTaskBuilder.newBuilder()
                .setName("[" + ProductOrder.Complexity.D2.toString() + "] DeliverBaseToMachine")
                .setPreConditionTasks(Collections.singleton(getBaseFromCS))
                .setMachine(disposeStation)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.DELIVER)
                .setSide(side)
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(disposeStation)
                .setUnlockMachine(disposeStation)
                .setSameRobotSubTask(getBaseFromCS)
                .setIncrementMachine(disposeStation)
                .setBindRsOnAssignment((disposeStation == null) ? true : false)
                .setPrepareRequired(prepareRequired)
                .setFatal(false)
                .setRequiresReset(false)
				.setIsDemandTask(true)
            .build();

		subtasks.add(deliverBaseToRS);

		return subtasks;
	}

	public String getRingStationForColor(RingColor ring1_color) {
		String ring1_station = null;

		List<MachineInfoRefBox> ringstations = machineInfoRefBoxDao.findByTypeAndTeamColor("RS", gameStateDao.getTeamColor().startsWith("C") ? MachineInfoRefBox.TeamColor.CYAN : MachineInfoRefBox.TeamColor.MAGENTA);

		for (MachineInfoRefBox station : ringstations) {
			RingColor r1 = station.getRing1();
			RingColor r2 = station.getRing2();

			if (ring1_color.compareTo(r1) == 0 || ring1_color.compareTo(r2) == 0) {
				ring1_station = station.getName();
			}
		}
		if (ring1_station == null) {
			System.out.println("Error! No correct ring station found for given ring color!");
		}
		return ring1_station;
	}


	private List<SubProductionTask> mountCapAndDeliverProduct(ProductOrder productOrder, String baseStation,
                                                              String capStation, String deliveryStation, String pickupStationForCapMount, Set<SubProductionTask> preconds) {
		List<SubProductionTask> subtasks = new ArrayList<>();

		String station = pickupStationForCapMount == null ? baseStation : pickupStationForCapMount; // pickupstation == null --> we pick from basestation
		boolean prepareRequired = pickupStationForCapMount == null ? true : false;  // basestation needs to be prepared
		boolean resetRequired = pickupStationForCapMount == null ? true : false;   // basestation needs to be reset

		int PRIORITY_MOUNT_CAP = 0;
		int PRIORITY_DELIVER = 0;

		if (productOrder.getComplexity() == ProductOrder.Complexity.C0 && !productOrder.isCompetitive()) {
			PRIORITY_MOUNT_CAP = Config.PRIORITY_MOUNT_CAP_C0;
			PRIORITY_DELIVER = Config.PRIORITY_DELIVER_C0;
		} else if (productOrder.getComplexity() == ProductOrder.Complexity.C0 && productOrder.isCompetitive()) {
			PRIORITY_MOUNT_CAP = Config.PRIORITY_MOUNT_CAP_C0_COMPETITIVE;
			PRIORITY_DELIVER = Config.PRIORITY_DELIVER_C0_COMPETITIVE;
        } else if (productOrder.getComplexity() == ProductOrder.Complexity.C1) {
            PRIORITY_MOUNT_CAP = Config.PRIORITY_MOUNT_CAP_C1;
            PRIORITY_DELIVER = Config.PRIORITY_DELIVER_C1;
        } else if (productOrder.getComplexity() == ProductOrder.Complexity.C2) {
            PRIORITY_MOUNT_CAP = Config.PRIORITY_MOUNT_CAP_C2;
            PRIORITY_DELIVER = Config.PRIORITY_DELIVER_C2;
        } else if (productOrder.getComplexity() == ProductOrder.Complexity.C3) {
            PRIORITY_MOUNT_CAP = Config.PRIORITY_MOUNT_CAP_C3;
            PRIORITY_DELIVER = Config.PRIORITY_DELIVER_C3;
        }

		SubProductionTask getBaseFromMachine = SubProductionTaskBuilder.newBuilder()
                .setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromMachine")
                .setPreConditionTasks(preconds)
                .setMachine(station)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.GET)
                .setSide(pickupStationForCapMount == null ? SubProductionTask.MachineSide.INPUT : SubProductionTask.MachineSide.OUTPUT) //OUTPUT if RS, INPUT otherwise
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(station) // either BS or RS
                .setIncrementMachine(null)
                .setDecrementMachine(null)
                .setSameRobotSubTask(null)
                .setRequiredColor(productOrder.getBaseColor().toString())
                .setOptCode(null)
                .setPrepareRequired(prepareRequired)
                .setFatal(true)
                .setRequiresReset(resetRequired)
                .setPriority(PRIORITY_MOUNT_CAP)
            .build();
		subtasks.add(getBaseFromMachine);


		SubProductionTask deliverBaseToCS = SubProductionTaskBuilder.newBuilder()
                .setName("[" + productOrder.getComplexity().toString() + "] DeliverBaseToCS")
                .setPreConditionTasks(Collections.singleton(getBaseFromMachine))
                .setMachine(capStation)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.DELIVER)
                .setSide(SubProductionTask.MachineSide.INPUT)
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(capStation)
                .setUnlockMachine(null)
                .setIncrementMachine(null)
                .setDecrementMachine(capStation)
                .setDecrementCost(1)
                .setSameRobotSubTask(getBaseFromMachine)
                .setRequiredColor(null)
                .setOptCode(MachineDescriptionProtos.CSOp.MOUNT_CAP.toString())
                .setPrepareRequired(true)
                .setFatal(true)
                .setRequiresReset(true)
            .build();
		subtasks.add(deliverBaseToCS);

		SubProductionTask getBaseFromCS = SubProductionTaskBuilder.newBuilder()
                .setName("[" + productOrder.getComplexity().toString() + "] GetBaseFromCS")
                .setPreConditionTasks(Collections.singleton(deliverBaseToCS))
                .setMachine(capStation)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.GET)
                .setSide(SubProductionTask.MachineSide.OUTPUT)
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(capStation)
                .setUnlockMachine(capStation)
                .setIncrementMachine(null)
                .setDecrementMachine(null)
                .setSameRobotSubTask(null)
                .setRequiredColor(null)
                .setOptCode(null)
                .setPrepareRequired(false)
                .setFatal(true)
                .setRequiresReset(true)
                .setMachineStateTaskReasign(TASK_STATE_READY_AT_OUTPUT)
                .setPriority(PRIORITY_DELIVER)
            .build();
		subtasks.add(getBaseFromCS);

		SubProductionTask deliverBaseToDS = SubProductionTaskBuilder.newBuilder()
                .setName("[" + productOrder.getComplexity() + "] DeliverBaseToDS")
                .setPreConditionTasks(Collections.singleton(getBaseFromCS))
                .setMachine(deliveryStation)
                .setState(TaskState.TBD)
                .setType(SubProductionTask.TaskType.DELIVER)
                .setSide(SubProductionTask.MachineSide.INPUT)
                .setOrderInfoId(productOrder.getId())
                .setLockMachine(deliveryStation)
                .setUnlockMachine(deliveryStation)
                .setIncrementMachine(null)
                .setDecrementMachine(null)
                .setSameRobotSubTask(getBaseFromCS)
                .setRequiredColor(null)
                .setOptCode(productOrder.getDeliveryGate()+"")
                .setPrepareRequired(true)
                .setFatal(true)
                .setRequiresReset(false)
            .build();
		subtasks.add(deliverBaseToDS);

		return subtasks;
	}
}
