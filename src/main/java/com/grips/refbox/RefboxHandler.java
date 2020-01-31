package com.grips.refbox;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.robocup_logistics.llsf_msgs.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class RefboxHandler {
    private Consumer<MachineInfoProtos.MachineInfo> consumer_machine_info;
    private Consumer<GameStateProtos.GameState> consumer_game_state;
    private Consumer<ExplorationInfoProtos.ExplorationInfo> consumer_exploration_info;
    private Consumer<VersionProtos.VersionInfo> consumer_version_info;
    private Consumer<RobotInfoProtos.RobotInfo> consumer_robot_info;
    private Consumer<MachineReportProtos.MachineReportInfo> consumer_machine_report;
    private Consumer<BeaconSignalProtos.BeaconSignal> consumer_beacon_sig;
    private Consumer<OrderInfoProtos.OrderInfo> consumer_order_info;
    private Consumer<RingInfoProtos.RingInfo> consumer_ring_info;

    public void RegisterHandleMachineInfo(Consumer<MachineInfoProtos.MachineInfo> machine_info)
    {
        this.consumer_machine_info = machine_info;
    }

    public void RegisterHandleGameState (Consumer<GameStateProtos.GameState> game_state)
    {
        this.consumer_game_state = game_state;
    }

    public void RegisterHandleExplorationInfo (Consumer<ExplorationInfoProtos.ExplorationInfo> exploration_info)
    {
        this.consumer_exploration_info = exploration_info;
    }

    public void RegisterHandleVersionInfo (Consumer<VersionProtos.VersionInfo> version_info)
    {
        this.consumer_version_info = version_info;
    }

    public void RegisterHandleRobotInfo (Consumer<RobotInfoProtos.RobotInfo> robot_info)
    {
        this.consumer_robot_info = robot_info;
    }

    public void RegisterHandleMachineReport (Consumer<MachineReportProtos.MachineReportInfo> machine_report)
    {
        this.consumer_machine_report = machine_report;
    }

    public void RegisterHandleBeaconSignal (Consumer<BeaconSignalProtos.BeaconSignal> beacon_sig)
    {
        this.consumer_beacon_sig = beacon_sig;
    }

    public void RegisterHandleOrderInfo (Consumer<OrderInfoProtos.OrderInfo> order_info)
    {
        this.consumer_order_info = order_info;
    }

    public void RegisterHandleRingInfo (Consumer<RingInfoProtos.RingInfo> ring_info)
    {
        this.consumer_ring_info = ring_info;
    }

    public void handle_message(ByteBuffer in_msg, GeneratedMessage msg) {
        if (null == in_msg) {
            System.err.println("ERROR: in_msg is null in TeamHandler!");
            return;
        }
        if (null == msg) {
            System.err.println("ERROR: msg is null in TeamHandler!");
            return;
        }

        byte[] array = new byte[in_msg.capacity()];
        in_msg.rewind();
        in_msg.get(array);

        try {
            msg.writeTo(new BufferedOutputStream(System.out));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (msg instanceof BeaconSignalProtos.BeaconSignal) {
            BeaconSignalProtos.BeaconSignal beacon_signal;
            try {
                beacon_signal = BeaconSignalProtos.BeaconSignal.parseFrom(array);
                //handleBeaconSignal(beacon_signal);
                consumer_beacon_sig.accept(beacon_signal);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
        else if (msg instanceof OrderInfoProtos.OrderInfo) {
            OrderInfoProtos.OrderInfo order_info;
            try {
                System.out.println("received private order !!!!!!!!!");
                order_info = OrderInfoProtos.OrderInfo.parseFrom(array);
                //handleOrderInfo(order_info);
                consumer_order_info.accept(order_info);

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof RingInfoProtos.RingInfo) {
            RingInfoProtos.RingInfo ring_info;
            try {
                ring_info = RingInfoProtos.RingInfo.parseFrom(array);
                //handleRingInfo(ring_info);
                consumer_ring_info.accept(ring_info);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof MachineInfoProtos.MachineInfo) {
            MachineInfoProtos.MachineInfo machine_info;
            try {
                machine_info = MachineInfoProtos.MachineInfo.parseFrom(array);
                //handleMachineInfo(machine_info);
                consumer_machine_info.accept(machine_info);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof MachineReportProtos.MachineReportInfo) {
            MachineReportProtos.MachineReportInfo machine_report_info;
            try {
                machine_report_info = MachineReportProtos.MachineReportInfo.parseFrom(array);
                //handleMachineReport(machine_report_info);
                consumer_machine_report.accept(machine_report_info);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof GameStateProtos.GameState) {
            GameStateProtos.GameState game_state;
            try {
                game_state = GameStateProtos.GameState.parseFrom(array);
                //handleGameState(game_state);
                consumer_game_state.accept(game_state);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof ExplorationInfoProtos.ExplorationInfo) {
            ExplorationInfoProtos.ExplorationInfo exploration_info;
            try {
                exploration_info = ExplorationInfoProtos.ExplorationInfo.parseFrom(array);
                //handleExplorationInfo(exploration_info);
                consumer_exploration_info.accept(exploration_info);
            } catch (InvalidProtocolBufferException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        } else if (msg instanceof VersionProtos.VersionInfo) {
            VersionProtos.VersionInfo version_info;
            try {
                version_info = VersionProtos.VersionInfo.parseFrom(array);
                //handleVersionInfo(version_info);
                consumer_version_info.accept(version_info);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (msg instanceof RobotInfoProtos.RobotInfo) {
            RobotInfoProtos.RobotInfo robot_info;
            try {
                robot_info = RobotInfoProtos.RobotInfo.parseFrom(array);
                //handleRobotInfo(robot_info);
                consumer_robot_info.accept(robot_info);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("ERROR: TeamHandler received unkown msg!");
        }
    }
}
