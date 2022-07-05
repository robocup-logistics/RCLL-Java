package com.grips.refbox;

import com.grips.model.teamserver.*;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.log4j.Log4j2;
import org.robocup_logistics.llsf_msgs.*;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

@Log4j2
public class RefboxClient {
    private final RefBoxConnectionManager rbcm;
    private final MachineClient machineClient;
    private final RobotClient robotClient;
    private final ExplorationClient explorationClient;
    private final Timer t;

    boolean inProduction;

    public RefboxClient(@NonNull RefboxConnectionConfig connectionConfig,
                        @NonNull TeamConfig teamConfig,
                        @NonNull RefboxHandler privateHandler,
                        @NonNull RefboxHandler publicHandler,
                        int sendIntervalInMs) {
        this(connectionConfig, teamConfig, privateHandler, publicHandler, sendIntervalInMs,
                new RefBoxConnectionManager(connectionConfig, teamConfig, privateHandler, publicHandler));
        inProduction = false;
    }

    RefboxClient(@NonNull RefboxConnectionConfig connectionConfig,
                 @NonNull TeamConfig teamConfig,
                 @NonNull RefboxHandler privateHandler,
                 @NonNull RefboxHandler publicHandler,
                 int sendIntervalInMs,
                 RefBoxConnectionManager rbcm) {
        this.rbcm = rbcm;
        machineClient = new MachineClient(TeamColor.fromString(teamConfig.getColor()));
        explorationClient = new ExplorationClient(TeamColor.fromString(teamConfig.getColor()));
        robotClient = new RobotClient(TeamColor.fromString(teamConfig.getColor()), teamConfig.getName());
        Consumer<MachineInfoProtos.MachineInfo> oldCallback = publicHandler.getMachineInfoCallback();
        publicHandler.setMachineInfoCallback(machineInfo -> {
            machineClient.update(machineInfo);
            oldCallback.accept(machineInfo);
            inProduction = true;
        });
        t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                log.debug("Sending data....");
                machineClient.fetchPrepareMessages().forEach(rbcm::sendPrivateMsg);
                machineClient.fetchResetMessages().forEach(rbcm::sendPrivateMsg);
                robotClient.fetchBeaconSignals().forEach(rbcm::sendPrivateMsg);
                robotClient.clearBeaconSignals();
                if (!inProduction) {
                    explorationClient.fetchExplorationMsg().forEach(rbcm::sendPrivateMsg);
                }
            }
        }, 0, sendIntervalInMs);
    }

    public void startServer() {
        this.rbcm.startServer();
        log.info("Started RefboxPeer connection!");
    }

    public void sendBeaconSignal(int robotNumber, String robotName,
                                 float x, float y, float yaw) {
        this.robotClient.sendRobotBeaconMsg(robotNumber, robotName, x, y, yaw);
    }

    public void sendReportMachine(MachineName machineName, ZoneName zone, int rotation) {
        this.explorationClient.sendExploreMachine(machineName, zone, rotation);
    }

    public void sendResetMachine(MachineClientUtils.Machine machine) {
        this.machineClient.sendResetMachine(machine);
    }

    public void sendPrepareBS(MachineClientUtils.MachineSide side, MachineClientUtils.BaseColor base_color) {
        this.machineClient.sendPrepareBS(side, base_color);
    }

    public void sendPrepareDS(int gate, int orderId) {
        this.machineClient.sendPrepareDS(gate, orderId);
    }

    public void sendPrepareRS(MachineClientUtils.Machine machine, MachineClientUtils.RingColor ringColor) {
        this.machineClient.sendPrepareRS(machine, ringColor);
    }

    public void sendPrepareCS(MachineClientUtils.Machine machine, MachineClientUtils.CSOp operation) {
        this.machineClient.sendPrepareCS(machine, operation);
    }

    public void sendPrepareSS(MachineClientUtils.Machine machine, int shelf, int slot) {
        this.machineClient.sendPrepareSS(machine, shelf, slot);
    }

    public Optional<MachineClientUtils.MachineState> getStateForMachine(MachineClientUtils.Machine machine) {
        return this.machineClient.getStateForMachine(machine);
    }

    public void updateMachineStates(MachineInfoProtos.MachineInfo info) {
        this.machineClient.updateMachineState(info);
        this.machineClient.updateMessages();
    }
}
