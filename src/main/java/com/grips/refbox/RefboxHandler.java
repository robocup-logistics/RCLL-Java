package com.grips.refbox;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;
import org.robocup_logistics.llsf_msgs.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@Setter
@Getter
@CommonsLog
public class RefboxHandler implements ProtobufMessageHandler {
    private Consumer<MachineInfoProtos.MachineInfo> machineInfoCallback;
    private Consumer<GameStateProtos.GameState> gameStateCallback;
    private Consumer<ExplorationInfoProtos.ExplorationInfo> explorationInfoCallback;
    private Consumer<VersionProtos.VersionInfo> versionInfoCallback;
    private Consumer<RobotInfoProtos.RobotInfo> robotInfoCallback;
    private Consumer<MachineReportProtos.MachineReportInfo> machineReportInfoCallback;
    private Consumer<BeaconSignalProtos.BeaconSignal> beaconSignalCallback;
    private Consumer<OrderInfoProtos.OrderInfo> orderInfoCallback;
    private Consumer<RingInfoProtos.RingInfo> ringInfoCallback;
    private Consumer<NavigationChallengeProtos.NavigationRoutes> navigationRoutesCallback;

    public RefboxHandler() {
        //setting default handler which only log a message that nothing will be don
        machineInfoCallback = (msg) -> log.debug("machineInfoCallback not set");
        gameStateCallback = (msg) -> log.debug("gameStateCallback not set");
        explorationInfoCallback = (msg) -> log.debug("explorationInfoCallback not set");
        versionInfoCallback = (msg) -> log.debug("versionInfoCallback not set");
        robotInfoCallback = (msg) -> log.debug("robotInfoCallback not set");
        machineReportInfoCallback = (msg) -> log.debug("machineReportInfoCallback not set");
        beaconSignalCallback = (msg) -> log.debug("beaconSignalCallback not set");
        orderInfoCallback = (msg) -> log.debug("orderInfoCallback not set");
        ringInfoCallback = (msg) -> log.debug("ringInfoCallback not set");
        navigationRoutesCallback = (msg) -> log.debug("navigationRoutesCallback not set");
    }

    @Override
    public void handle_message(@NonNull ByteBuffer in_msg, @NonNull GeneratedMessageV3 msg) {
        byte[] array = new byte[in_msg.capacity()];
        in_msg.rewind();
        in_msg.get(array);

        //todo remove this!
        try {
            msg.writeTo(new BufferedOutputStream(System.out));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (msg instanceof BeaconSignalProtos.BeaconSignal) {
                beaconSignalCallback.accept(BeaconSignalProtos.BeaconSignal.parseFrom(array));
            } else if (msg instanceof OrderInfoProtos.OrderInfo) {
                orderInfoCallback.accept(OrderInfoProtos.OrderInfo.parseFrom(array));
            } else if (msg instanceof RingInfoProtos.RingInfo) {
                ringInfoCallback.accept(RingInfoProtos.RingInfo.parseFrom(array));
            } else if (msg instanceof MachineInfoProtos.MachineInfo) {
                machineInfoCallback.accept(MachineInfoProtos.MachineInfo.parseFrom(array));
            } else if (msg instanceof MachineReportProtos.MachineReportInfo) {
                machineReportInfoCallback.accept(MachineReportProtos.MachineReportInfo.parseFrom(array));
            } else if (msg instanceof GameStateProtos.GameState) {
                gameStateCallback.accept(GameStateProtos.GameState.parseFrom(array));
            } else if (msg instanceof ExplorationInfoProtos.ExplorationInfo) {
                explorationInfoCallback.accept(ExplorationInfoProtos.ExplorationInfo.parseFrom(array));
            } else if (msg instanceof VersionProtos.VersionInfo) {
                versionInfoCallback.accept(VersionProtos.VersionInfo.parseFrom(array));
            } else if (msg instanceof RobotInfoProtos.RobotInfo) {
                robotInfoCallback.accept(RobotInfoProtos.RobotInfo.parseFrom(array));
            } else if (msg instanceof NavigationChallengeProtos.NavigationRoutes) {
                navigationRoutesCallback.accept(NavigationChallengeProtos.NavigationRoutes.parseFrom(array));
            } else {
                log.error(this.getClass() + " received unkown msg!");
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid ProtoBuf Message!", e);
        }
    }

    @Override
    public void connection_lost(IOException e) {
        log.error(this.getClass() + " lost connection to the RefBox!", e);
    }


    @Override
    public void timeout() {
        log.warn(this.getClass() + " timeout in connection to RefBox at channel Public!");
    }
}
