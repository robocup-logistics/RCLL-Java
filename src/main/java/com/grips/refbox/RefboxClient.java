package com.grips.refbox;

import com.grips.model.teamserver.MachineClient;
import com.grips.model.teamserver.TeamColor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_msgs.MachineInfoProtos;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

@Getter
@CommonsLog
public class RefboxClient {
    private final RefBoxConnectionManager rbcm;
    private final MachineClient machineClient;
    private final RobotClient robotClient;
    private final ExplorationClient explorationClient;
    public RefboxClient(@NonNull RefboxConnectionConfig connectionConfig,
                        @NonNull TeamConfig teamConfig,
                        @NonNull RefboxHandler privateHandler,
                        @NonNull RefboxHandler publicHandler,
                        int sendIntervalInMs) {
        machineClient = new MachineClient(TeamColor.fromString(teamConfig.getColor()));
        explorationClient = new ExplorationClient(TeamColor.fromString(teamConfig.getColor()));
        robotClient = new RobotClient(TeamColor.fromString(teamConfig.getColor()));
        Consumer<MachineInfoProtos.MachineInfo> oldCallback = publicHandler.getMachineInfoCallback();
        publicHandler.setMachineInfoCallback(machineInfo -> {
            machineClient.update(machineInfo);
            oldCallback.accept(machineInfo);
        });
        rbcm = new RefBoxConnectionManager(connectionConfig, teamConfig, privateHandler, publicHandler);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                machineClient.fetchPrepareMessages().forEach(rbcm::sendPrivateMsg);
                machineClient.fetchResetMessages().forEach(rbcm::sendPrivateMsg);
                robotClient.fetchBeaconSignals().forEach(rbcm::sendPrivateMsg);
                robotClient.clearBeaconSignals();
                explorationClient.fetchExplorationMsg().forEach(rbcm::sendPrivateMsg);
                explorationClient.clearExploraionMsgs();
            }
        }, 0, sendIntervalInMs);
    }

    public void startServer() {
        this.rbcm.startServer();
        log.info("Started RefboxPeer connection!");
    }
}
