package com.grips.model.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

@Entity
public class SubProductionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
  
	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "preconditions", joinColumns = {
			@JoinColumn(name = "sub_production_task_id", nullable = false) }, inverseJoinColumns = {
					@JoinColumn(name = "pre_sub_production_task_id", nullable = true) })
	@JsonIgnore
    private Set<SubProductionTask> preConditionTasks;
	
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy="preConditionTasks")
	@JsonIgnore
    private Set<SubProductionTask> subConditionTasks;

	private String name;

    private String machine;

    @Enumerated(EnumType.STRING)
    private TaskState state;

    private Long orderInfoId;
    
    private String lockMachine;
    
    private String unlockMachine;
    
    private String incrementMachine;
    
    private String decrementMachine;

    private String requiredColor;

    private String optCode;

    private boolean prepareRequired;

    private boolean requiresReset;

    private int decrementCost;

    private boolean isDemandTask;
    
    @OneToOne
	@JsonIgnore
    private SubProductionTask sameRobotSubTask;
    
    @OneToOne(mappedBy="sameRobotSubTask")
	@JsonIgnore
    private SubProductionTask postsameRobotSubTask;

    @ManyToOne
    @JoinColumn(name="product_task_id")
	@JsonIgnore
    private ProductTask productTask;

    private Integer robotId;
    
    private Long startTime;

    private Long endTime;

    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    private MachineSide side;

    private boolean fatal;

    private String machineStateTaskOk;

    private String machineStateRequiresReset;

    private String machineStateTaskReasign;

    private boolean bindDisposeRsOnAssignment;

    private boolean demandTaskWithOutDemand;

    private int priority;

	@Lob
    private String stateLogging;

    public SubProductionTask() {
    }

    public SubProductionTask(Set<SubProductionTask> preConditionTasks, String name, String machine,
                             TaskState state, TaskType type, MachineSide side, Long orderInfoId, String lock, String unlock, String incrementMachine,
                             String decrementMachine, int decrementCost, SubProductionTask sameRobotSubTask, String requiredColor, String optCode,
                             boolean prepareRequired, boolean fatal, boolean requiresReset, boolean bindRsOnAssignment, String machineStateTaskOk, String machineStateRequiresReset,
                             String machineStateTaskReasign, boolean demandTaskWithOutDemand, int priority, boolean isDemandTask) {
		super();
		this.preConditionTasks = preConditionTasks;
		this.name = name;
		this.machine = machine;
		this.state = state;
		this.orderInfoId = orderInfoId;
		this.lockMachine = lock;
		this.unlockMachine = unlock;
		this.incrementMachine = incrementMachine;
		this.decrementMachine = decrementMachine;
		this.sameRobotSubTask = sameRobotSubTask;
		this.type = type;
		this.side = side;
		this.requiredColor = requiredColor;
		this.optCode = optCode;
		this.prepareRequired = prepareRequired;
		this.fatal = fatal;
		this.requiresReset = requiresReset;
		this.machineStateTaskOk = machineStateTaskOk;
		this.machineStateRequiresReset = machineStateRequiresReset;
		this.machineStateTaskReasign = machineStateTaskReasign;
		this.decrementCost = decrementCost;
		this.bindDisposeRsOnAssignment = bindRsOnAssignment;
		this.demandTaskWithOutDemand = demandTaskWithOutDemand;
		this.priority = priority;
		this.isDemandTask = isDemandTask;
	}


    public long getId() {
        return id;
    }

	public Collection<SubProductionTask> getPreConditionTasks() {
		return preConditionTasks;
	}

	public void setPreConditionTasks(Set<SubProductionTask> preConditionTasks) {
		this.preConditionTasks = preConditionTasks;
	}

	public Collection<SubProductionTask> getSubConditionTasks() {
		return subConditionTasks;
	}

	public void setSubConditionTasks(Set<SubProductionTask> subConditionTasks) {
		this.subConditionTasks = subConditionTasks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public TaskState getState() {
		return state;
	}

	public void setState(TaskState state, String stateLogging) {
		this.state = state;
		this.stateLogging += ("; " + stateLogging);
	}

	public Long getOrderInfoId() {
		return orderInfoId;
	}

	public void setOrderInfoId(Long orderInfoId) {
		this.orderInfoId = orderInfoId;
	}

	public String getLockMachine() {
		return lockMachine;
	}

	public void setLockMachine(String lockMachine) {
		this.lockMachine = lockMachine;
	}

	public String getUnlockMachine() {
		return unlockMachine;
	}

	public void setUnlockMachine(String unlockMachine) {
		this.unlockMachine = unlockMachine;
	}

	public String getIncrementMachine() {
		return incrementMachine;
	}

	public void setIncrementMachine(String incrementMachine) {
		this.incrementMachine = incrementMachine;
	}

	public String getDecrementMachine() {
		return decrementMachine;
	}

	public void setDecrementMachine(String decrementMachine) {
		this.decrementMachine = decrementMachine;
	}

	public SubProductionTask getSameRobotSubTask() {
		return sameRobotSubTask;
	}

	public void setSameRobotSubTask(SubProductionTask sameRobotSubTask) {
		this.sameRobotSubTask = sameRobotSubTask;
	}

	public SubProductionTask getPostsameRobotSubTask() {
		return postsameRobotSubTask;
	}

	public void setPostsameRobotSubTask(SubProductionTask postsameRobotSubTask) {
		this.postsameRobotSubTask = postsameRobotSubTask;
	}

	public Integer getRobotId() {
		return robotId;
	}

	public void setRobotId(Integer robotId) {
		if (robotId == null) {
			System.err.println("RobotId set to NULL!!!!");
			throw new NullPointerException();
		}
    	this.robotId = robotId;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

    public ProductTask getProductTask() {
        return productTask;
    }

    public void setProductTask(ProductTask productTask) {
        this.productTask = productTask;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public MachineSide getSide() {
        return side;
    }

    public void setSide(MachineSide side) {
        this.side = side;
    }

	public String getRequiredColor() {
		return requiredColor;
	}

	public void setRequiredColor(String requiredColor) {
		this.requiredColor = requiredColor;
	}

	public String getOptCode() {
		return optCode;
	}

	public void setOptCode(String optCode) {
		this.optCode = optCode;
	}

	public boolean isPrepareRequired() {
		return prepareRequired;
	}

	public void setPrepareRequired(boolean prepareRequired) {
		this.prepareRequired = prepareRequired;
	}

	public boolean isFatal() {
		return fatal;
	}

	public void setFatal(boolean fatal) {
		this.fatal = fatal;
	}

	public boolean isRequiresReset() {
		return requiresReset;
	}

	public void setRequiresReset(boolean requiresReset) {
		this.requiresReset = requiresReset;
	}

	public String getMachineStateTaskOk() {
		return machineStateTaskOk;
	}

	public void setMachineStateTaskOk(String machineStateTaskOk) {
		this.machineStateTaskOk = machineStateTaskOk;
	}

	public String getMachineStateRequiresReset() {
		return machineStateRequiresReset;
	}

	public void setMachineStateRequiresReset(String machineStateRequiresReset) {
		this.machineStateRequiresReset = machineStateRequiresReset;
	}

	public String getMachineStateTaskReasign() {
		return machineStateTaskReasign;
	}

	public void setMachineStateTaskReasign(String machineStateTaskReasign) {
		this.machineStateTaskReasign = machineStateTaskReasign;
	}

	public int getDecrementCost() {
		return decrementCost;
	}

	public void setDecrementCost(int decrementCost) {
		this.decrementCost = decrementCost;
	}

	public boolean isBindDisposeRsOnAssignment() {
		return bindDisposeRsOnAssignment;
	}

	public void setBindDisposeRsOnAssignment(boolean bindDisposeRsOnAssignment) {
		this.bindDisposeRsOnAssignment = bindDisposeRsOnAssignment;
	}

	public boolean isDemandTaskWithOutDemand() {
		return demandTaskWithOutDemand;
	}

	public void setDemandTaskWithOutDemand(boolean demandTaskWithOutDemand) {
		this.demandTaskWithOutDemand = demandTaskWithOutDemand;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isDemandTask() {
		return isDemandTask;
	}

	public void setDemandTask(boolean demandTask) {
		isDemandTask = demandTask;
	}

	public String getStateLogging() {
		return stateLogging;
	}

	public void setStateLogging(String stateLogging) {
		this.stateLogging = stateLogging;
	}

	public enum TaskState {SUCCESS, FAILED, TBD, INWORK, ASSIGNED, SUCCESS_PENDING, ASSIGNED_EXPLORATION }

    public enum TaskType {GET, DELIVER, DUMMY}

    public enum MachineSide {INPUT, OUTPUT, SHELF, SLIDE}
}