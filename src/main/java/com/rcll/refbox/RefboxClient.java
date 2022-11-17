package com.rcll.refbox;

import com.rcll.domain.*;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.robocup_logistics.llsf_msgs.GameStateProtos;
import org.robocup_logistics.llsf_msgs.MachineInfoProtos;
import org.robocup_logistics.llsf_msgs.OrderInfoProtos;
import org.robocup_logistics.llsf_msgs.RingInfoProtos;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public class RefboxClient {
    private final RefBoxConnectionManager rbcm;
    private Optional<MachineClient> machineClient;
    private Optional<RobotClient> robotClient;
    private Optional<ExplorationClient> explorationClient;
    private Optional<OrderService> orderService;
    private TeamColor teamColor;
    private final Timer t;

    boolean inProduction;

    boolean publicServerStarted;
    boolean privateServerStarted;

    GameStateProtos.GameState lastGameState = null;

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
        this.teamColor = null;
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
            updateGameState(gameState);
            if (!privateServerStarted) {
                if (teamConfig.getName().equals(gameState.getTeamCyan())) {
                    teamColor = TeamColor.CYAN;
                } else if (teamConfig.getName().equals(gameState.getTeamMagenta())) {
                    teamColor = TeamColor.MAGENTA;
                } else {
                    log.warn("No Team is configured to be: " + teamConfig.getName());
                }
                if (teamColor != null) {
                    rbcm.startPrivateServer(teamColor);
                    this.privateServerStarted = true;
                    machineClient = Optional.of(new MachineClient(teamColor));
                    explorationClient = Optional.of(new ExplorationClient(teamColor));
                    robotClient = Optional.of(new RobotClient(teamColor, teamConfig.getName()));
                    orderService = Optional.of(new OrderService(machineClient.get(), teamColor));
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

    @Synchronized
    private void updateGameState(GameStateProtos.GameState gameState) {
        this.lastGameState = gameState;
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

    public List<Ring> getAllRings() {
        return Arrays.stream(MachineClientUtils.RingColor.values())
                .map(this::getRingByColor)
                .collect(Collectors.toList());
    }

    public Optional<TeamColor> getTeamColor() {
        return Optional.ofNullable(teamColor);
    }

    public boolean isMagenta() {
        return TeamColor.MAGENTA.equals(teamColor);
    }

    public boolean isCyan() {
        return TeamColor.CYAN.equals(teamColor);
    }

    @Synchronized
    public Long getLatestGameTimeInSeconds() {
        if (lastGameState == null) {
            return 0L;
        }
        return lastGameState.getGameTime().getSec();
    }
}
