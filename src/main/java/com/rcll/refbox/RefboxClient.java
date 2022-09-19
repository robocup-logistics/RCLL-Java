package com.rcll.refbox;

import com.rcll.domain.*;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.robocup_logistics.llsf_msgs.*;

import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

@Log4j2
public class RefboxClient {
    private final RefBoxConnectionManager rbcm;
    private Optional<MachineClient> machineClient;
    private Optional<RobotClient> robotClient;
    private Optional<ExplorationClient> explorationClient;
    private Optional<OrderService> orderService;
    private final Timer t;

    boolean inProduction;

    boolean publicServerStarted;
    boolean privateServerStarted;

    public RefboxClient(@NonNull RefboxConnectionConfig connectionConfig,
                        @NonNull TeamConfig teamConfig,
                        @NonNull RefboxHandler privateHandler,
                        @NonNull RefboxHandler publicHandler,
                        int sendIntervalInMs) {
        this(connectionConfig, teamConfig, privateHandler, publicHandler, sendIntervalInMs,
                new RefBoxConnectionManager(connectionConfig, teamConfig, privateHandler, publicHandler));
    }

    RefboxClient(@NonNull RefboxConnectionConfig connectionConfig,
                 @NonNull TeamConfig teamConfig,
                 @NonNull RefboxHandler privateHandler,
                 @NonNull RefboxHandler publicHandler,
                 int sendIntervalInMs,
                 RefBoxConnectionManager rbcm) {
        inProduction = false;
        this.publicServerStarted = false;
        this.rbcm = rbcm;
        this.machineClient = Optional.empty();
        this.robotClient = Optional.empty();
        this.explorationClient = Optional.empty();
        this.orderService = Optional.empty();
        Consumer<MachineInfoProtos.MachineInfo> oldMachineInfoCallback = publicHandler.getMachineInfoCallback();
        publicHandler.setMachineInfoCallback(machineInfo -> {
            machineClient.ifPresent(m -> m.update(machineInfo));
            oldMachineInfoCallback.accept(machineInfo);
            inProduction = true;
        });
        Consumer<OrderInfoProtos.OrderInfo> oldOrderInfoCallback = publicHandler.getOrderInfoCallback();
        publicHandler.setOrderInfoCallback(orderInfo -> {
            this.orderService.ifPresent(o -> o.update(orderInfo));
            oldOrderInfoCallback.accept(orderInfo);
        });

        Consumer<RingInfoProtos.RingInfo> oldRingInfoCallback = privateHandler.getRingInfoCallback();
        privateHandler.setRingInfoCallback(ringInfo -> {
            this.machineClient.ifPresent(m ->m.updateRingInfo(ringInfo));
            oldRingInfoCallback.accept(ringInfo);
        });
        Consumer<GameStateProtos.GameState> oldGameStateCallback = publicHandler.getGameStateCallback();
        publicHandler.setGameStateCallback(gameState -> {
            if (!privateServerStarted) {
                TeamColor color = null;
                if (teamConfig.getName().equals(gameState.getTeamCyan())) {
                    color = TeamColor.CYAN;
                } else if (teamConfig.getName().equals(gameState.getTeamMagenta())) {
                    color = TeamColor.MAGENTA;
                } else {
                    log.warn("No Team is configured to be: " + teamConfig.getName());
                }
                if (color != null) {
                    rbcm.startPrivateServer(color);
                    this.privateServerStarted = true;
                    machineClient = Optional.of(new MachineClient(color));
                    explorationClient = Optional.of(new ExplorationClient(color));
                    robotClient = Optional.of(new RobotClient(color, teamConfig.getName()));
                    orderService = Optional.of(new OrderService(machineClient.get(), color));
                }
            }
            oldGameStateCallback.accept(gameState);
        });

        t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                log.debug("Sending data....");
                machineClient.ifPresent(m -> m.fetchPrepareMessages().forEach(rbcm::sendPrivateMsg));
                machineClient.ifPresent(m -> m.fetchResetMessages().forEach(rbcm::sendPrivateMsg));
                robotClient.ifPresent(r -> r.fetchBeaconSignals().forEach(rbcm::sendPrivateMsg));
                robotClient.ifPresent(RobotClient::clearBeaconSignals);
                if (!inProduction) {
                    explorationClient.ifPresent(e -> e.fetchExplorationMsg().forEach(rbcm::sendPrivateMsg));
                }
            }
        }, 0, sendIntervalInMs);
    }

    public void startServer() {
        this.rbcm.startPublicServer();
        this.publicServerStarted = true;
        log.info("Started public RefboxPeer connection!");
    }

    public void sendBeaconSignal(int robotNumber, String robotName,
                                 float x, float y, float yaw) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.robotClient.orElseThrow().sendRobotBeaconMsg(robotNumber, robotName, x, y, yaw);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendReportMachine(MachineName machineName, ZoneName zone, int rotation) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.explorationClient.orElseThrow().sendExploreMachine(machineName, zone, rotation);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendResetMachine(MachineClientUtils.Machine machine) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendResetMachine(machine);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendPrepareBS(MachineClientUtils.MachineSide side, MachineClientUtils.BaseColor base_color) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendPrepareBS(side, base_color);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendPrepareDS(int gate, int orderId) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendPrepareDS(gate, orderId);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendPrepareRS(MachineClientUtils.Machine machine, MachineClientUtils.RingColor ringColor) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendPrepareRS(machine, ringColor);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendPrepareCS(MachineClientUtils.Machine machine, MachineClientUtils.CSOp operation) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendPrepareCS(machine, operation);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public void sendPrepareSS(MachineClientUtils.Machine machine, int shelf, int slot) {
        checkIfPublicStarted();
        if (privateServerStarted) {
            this.machineClient.orElseThrow().sendPrepareSS(machine, shelf, slot);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    public Optional<MachineClientUtils.MachineState> getStateForMachine(MachineClientUtils.Machine machine) {
        if (privateServerStarted) {
            return this.machineClient.orElseThrow().getStateForMachine(machine);
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
        return Optional.empty();
    }

    public void updateMachineStates(MachineInfoProtos.MachineInfo info) {
        if (privateServerStarted) {
            this.machineClient.orElseThrow().updateMachineState(info);
            this.machineClient.orElseThrow().updateMessages();
        } else {
            log.warn("Private Server not yet started! Is your team configured on Refbox?");
        }
    }

    private void checkIfPublicStarted() {
        if (!this.publicServerStarted) {
            log.warn("Public Server not yet started! Did you forget to call startServer on RefboxClient?");
        }
    }

    public Order getOrderById(int orderId) {
        return orderService.orElseThrow().getOrder(orderId);
    }

    public List<Order> getAllOrders() {
        return orderService.orElseThrow().getOrders();
    }

    public Ring getRingByColor(MachineClientUtils.RingColor ringColor) {
        return machineClient.orElseThrow().getRingForColor(ringColor);
    }
}
