package com.grips.refbox;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
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
@CommonsLog
public class RefboxHandler implements ProtobufMessageHandler {
    private Consumer<MachineInfoProtos.MachineInfo> consumer_machine_info;
    private Consumer<GameStateProtos.GameState> consumer_game_state;
    private Consumer<ExplorationInfoProtos.ExplorationInfo> consumer_exploration_info;
    private Consumer<VersionProtos.VersionInfo> consumer_version_info;
    private Consumer<RobotInfoProtos.RobotInfo> consumer_robot_info;
    private Consumer<MachineReportProtos.MachineReportInfo> consumer_machine_report;
    private Consumer<BeaconSignalProtos.BeaconSignal> consumer_beacon_sig;
    private Consumer<OrderInfoProtos.OrderInfo> consumer_order_info;
    private Consumer<RingInfoProtos.RingInfo> consumer_ring_info;

    private RefboxHandler() {
        //setting default handler which only log a message that nothing will be don
        consumer_machine_info = (msg) -> log.debug("consumer_machine_info not set");
        consumer_game_state = (msg) -> log.debug("consumer_game_state not set");
        consumer_exploration_info = (msg) -> log.debug("consumer_exploration_info not set");
        consumer_version_info = (msg) -> log.debug("consumer_version_info not set");
        consumer_robot_info = (msg) -> log.debug("consumer_robot_info not set");
        consumer_machine_report = (msg) -> log.debug("consumer_machine_report not set");
        consumer_beacon_sig = (msg) -> log.debug("consumer_beacon_sig not set");
        consumer_order_info = (msg) -> log.debug("consumer_order_info not set");
        consumer_ring_info = (msg) -> log.debug("consumer_ring_info not set");
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
                consumer_beacon_sig.accept(BeaconSignalProtos.BeaconSignal.parseFrom(array));
            } else if (msg instanceof OrderInfoProtos.OrderInfo) {
                consumer_order_info.accept(OrderInfoProtos.OrderInfo.parseFrom(array));
            } else if (msg instanceof RingInfoProtos.RingInfo) {
                consumer_ring_info.accept(RingInfoProtos.RingInfo.parseFrom(array));
            } else if (msg instanceof MachineInfoProtos.MachineInfo) {
                consumer_machine_info.accept(MachineInfoProtos.MachineInfo.parseFrom(array));
            } else if (msg instanceof MachineReportProtos.MachineReportInfo) {
                consumer_machine_report.accept(MachineReportProtos.MachineReportInfo.parseFrom(array));
            } else if (msg instanceof GameStateProtos.GameState) {
                consumer_game_state.accept(GameStateProtos.GameState.parseFrom(array));
            } else if (msg instanceof ExplorationInfoProtos.ExplorationInfo) {
                consumer_exploration_info.accept(ExplorationInfoProtos.ExplorationInfo.parseFrom(array));
            } else if (msg instanceof VersionProtos.VersionInfo) {
                consumer_version_info.accept(VersionProtos.VersionInfo.parseFrom(array));
            } else if (msg instanceof RobotInfoProtos.RobotInfo) {
                consumer_robot_info.accept(RobotInfoProtos.RobotInfo.parseFrom(array));
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
