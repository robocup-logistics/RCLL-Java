package com.grips.refbox;

import com.grips.model.teamserver.MachineClient;
import com.grips.model.teamserver.TeamColor;
import lombok.Getter;
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

    public RefboxClient(RefboxConnectionConfig connectionConfig,
                        TeamConfig teamConfig,
                        RefboxHandler privateHandler,
                        RefboxHandler publicHandler,
                        int sendIntervalInMs) {
        machineClient = new MachineClient(TeamColor.fromString(teamConfig.getColor()));
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
            }
        }, 0, sendIntervalInMs);
    }
}
