package org.robocup_logistics.llsf_tools;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.robocup_logistics.llsf_comm.ProtobufUpdBroadcastConnection;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;
import org.robocup_logistics.llsf_msgs.TeamProtos.*;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos.*;
import org.robocup_logistics.llsf_msgs.OrderInfoProtos.*;
import org.robocup_logistics.llsf_msgs.GameStateProtos.*;
import org.robocup_logistics.llsf_msgs.VersionProtos.*;
import org.robocup_logistics.llsf_msgs.ExplorationInfoProtos.*;
import org.robocup_logistics.llsf_msgs.MachineInfoProtos.*;
import org.robocup_logistics.llsf_msgs.MachineReportProtos.*;
import org.robocup_logistics.llsf_msgs.RobotInfoProtos.*;
import org.robocup_logistics.llsf_msgs.TimeProtos.*;
import org.robocup_logistics.llsf_msgs.Pose2DProtos.*;

import org.robocup_logistics.llsf_utils.NanoSecondsTimestampProvider;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class FakeRobot {
	
	private static String ROBOT_NAME;
	private static String TEAM_NAME;
	private static Team TEAM_COLOR;
	private static String ENCRYPTION_KEY;
	
	private static String HOST;
	private static boolean local = true;
	
	private final static int SENDPORT = 4445;
	private final static int RECVPORT = 4444;
	
	private final static int CYAN_SENDPORT = 4446;
	private final static int CYAN_RECVPORT = 4441;
	
	private final static int MAGENTA_SENDPORT = 4447;
	private final static int MAGENTA_RECVPORT = 4442;
	
	private static ProtobufUpdBroadcastConnection peerPublic;
	private static ProtobufUpdBroadcastConnection peerPrivate;
	
	private static boolean crypto_setup = false;
	
	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("Usage: FakeRobot.jar <name> <team> <encryption-key> [-h <host> (only for a remote refbox)]");
			System.exit(1);
		}
		
		ROBOT_NAME = args[0];
		TEAM_NAME = args[1];
		ENCRYPTION_KEY = args[2];
		TEAM_COLOR = Team.CYAN;
		
		if (args.length > 3) {
			if (args[3].equals("-h")) {
				HOST = args[4];
				local = false;
			} else {
				System.out.println("Usage: FakeRobot.jar <name> <team> <encryption-key> [-h <host> (only for a remote refbox)]");
				System.exit(1);	
			}
		} else {
			try {
				HOST = Inet4Address.getLocalHost().getHostAddress();
				if (!HOST.endsWith("255")) {
					String[] address = HOST.split("\\.");
					HOST = "";
					for (int i = 0; i <= 2; i++) {
						HOST += address[i] + ".";
					}
					HOST += "255";
				}
				local = true;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		System.out.println("Using host " + HOST);
		
		System.out.println("Using team color cyan for BeaconSignals");
		
		peerPublic = new ProtobufUpdBroadcastConnection(HOST, local ? SENDPORT : RECVPORT, RECVPORT);
		try {
			peerPublic.start("FakeRobot");
		} catch (IOException e) {
			e.printStackTrace();
		}

		peerPublic.add_message(BeaconSignal.class);
		peerPublic.add_message(OrderInfo.class);
		peerPublic.add_message(GameState.class);
		peerPublic.add_message(VersionInfo.class);
		peerPublic.add_message(ExplorationInfo.class);
		peerPublic.add_message(MachineInfo.class);
		peerPublic.add_message(MachineReportInfo.class);
		peerPublic.add_message(RobotInfo.class);
		
		Handler handler = new Handler();
		peerPublic.register_handler(handler);
		
		BeaconThread thread = new BeaconThread();
		thread.start();
		
	}
	
	private static class BeaconThread extends Thread {
		
		public void run() {
			while(true) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				NanoSecondsTimestampProvider nstp = new NanoSecondsTimestampProvider();
				
				long ms = System.currentTimeMillis();
				long ns = nstp.currentNanoSecondsTimestamp();
		
				int sec = (int) (ms / 1000);
				long nsec = ns - (ms * 1000000L);
					
				Time t = Time.newBuilder().setSec(sec).setNsec(nsec).build();
				BeaconSignal bs = BeaconSignal.newBuilder().setTime(t).setSeq(1).setNumber(1).setPeerName(ROBOT_NAME).setTeamName(TEAM_NAME).setTeamColor(TEAM_COLOR).build();
				
				ProtobufMessage msg = new ProtobufMessage(2000, 1, bs);
				
				if (crypto_setup) {
					peerPrivate.enqueue(msg);	
				} else {
					peerPublic.enqueue(msg);
				}
				
			}
		}
		
	}
	
	private static class Handler implements ProtobufMessageHandler {
		
		public void handle_message(ByteBuffer in_msg, GeneratedMessageV3 msg) {
			
			if (msg instanceof BeaconSignal) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				BeaconSignal bs;
				Time t;
				
				try {
					bs = BeaconSignal.parseFrom(array);
					t = bs.getTime();
					System.out.printf("Detected robot: %d %s:%s (seq %d)\n", bs.getNumber(), bs.getTeamName(), bs.getPeerName(), t.getSec());
					
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof OrderInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				OrderInfo info;
				
				try {
					info = OrderInfo.parseFrom(array);
					System.out.println("ProductOrder info received:");
					
					for (int i = 0; i < info.getOrdersCount(); i++) {
						Order order = info.getOrders(i);
						
						int begin = order.getDeliveryPeriodBegin();
						int end = order.getDeliveryPeriodEnd();
						
						long min_begin = begin / 60;
						long sec_begin = begin - min_begin * 60;
						long min_end = end / 60;
						long sec_end = end - min_end * 60;
						
						System.out.printf("  %d: %d/%d/%d from %02d:%02d to %02d:%02d at gate %s\n",
								order.getId(), order.getQuantityDeliveredCyan(), order.getQuantityDeliveredMagenta(),
								order.getQuantityRequested(), min_begin, sec_begin, min_end, sec_end,
								order.getDeliveryGate());
					}
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof GameState) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				GameState state;
				Time t;
				
				try {
					state = GameState.parseFrom(array);
					t = state.getGameTime();
					
					long hour = t.getSec() / 3600;
					long min = (t.getSec() - hour * 3600) / 60;
					long sec = t.getSec() - hour * 3600 - min * 60;
					
					System.out.printf("GameState received: %02d:%02d:%02d.%02d  %s %s  %d:%d points, %s vs. %s\n",
							hour, min, sec, t.getNsec() / 1000000,
							state.getPhase().toString(), state.getState().toString(),
							state.getPointsCyan(), state.getPointsMagenta(),
							state.getTeamCyan(), state.getTeamMagenta());
					
					if (!crypto_setup) {
						crypto_setup = true;
						
						if (TEAM_NAME.equals(state.getTeamCyan())) {
							TEAM_COLOR = Team.CYAN;
						} else if (TEAM_NAME.equals(state.getTeamMagenta())) {
							TEAM_COLOR = Team.MAGENTA;
						} else {
							System.out.println("Our team is not set, training game? Disabling crypto.");
							crypto_setup = false;
							return;
						}
						
						switch (TEAM_COLOR) {
						case CYAN:
							peerPrivate = new ProtobufUpdBroadcastConnection(HOST, local ? CYAN_SENDPORT : CYAN_RECVPORT, CYAN_RECVPORT, true, 2, ENCRYPTION_KEY);
							break;
						case MAGENTA:
							peerPrivate = new ProtobufUpdBroadcastConnection(HOST, local ? MAGENTA_SENDPORT : MAGENTA_RECVPORT, MAGENTA_RECVPORT, true, 2, ENCRYPTION_KEY);
							break;
						}
					
						if (peerPrivate != null) {
							try {
								peerPrivate.start("FakeRobot1");
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							peerPrivate.add_message(BeaconSignal.class);
							peerPrivate.add_message(OrderInfo.class);
							peerPrivate.add_message(GameState.class);
							peerPrivate.add_message(VersionInfo.class);
							peerPrivate.add_message(ExplorationInfo.class);
							peerPrivate.add_message(MachineInfo.class);
							peerPrivate.add_message(MachineReportInfo.class);
							peerPrivate.add_message(RobotInfo.class);
							
							Handler handler = new Handler();
							peerPrivate.register_handler(handler);
						}
					}
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof VersionInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				VersionInfo version;
				
				try {
					version = VersionInfo.parseFrom(array);
					System.out.println("VersionInfo received: " + version.getVersionString());
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof ExplorationInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				ExplorationInfo exploration;
				
				try {
					exploration = ExplorationInfo.parseFrom(array);
					System.out.println("ExplorationInfo received:");
					
					for (int i = 0; i < exploration.getSignalsCount(); i++) {
						ExplorationSignal signal = exploration.getSignals(i);
						System.out.printf("  Machine type %s assignment:", signal.getType());
						
						for (int j = 0; j < signal.getLightsCount(); j++) {
							LightSpec light = signal.getLights(j);
							System.out.printf(" %s=%s", light.getColor().toString(), light.getState().toString());
						}
						System.out.printf("\n");
					}
					System.out.printf("  --\n");
					
					/*
					for (int i = 0; i < exploration.getMachinesCount(); i++) {
						ExplorationMachine machine = exploration.getMachines(i);
						System.out.printf("  Machine %s at (%f, %f, %f)\n",
								machine.getName(), machine.getPose().getX(),
								machine.getPose().getY(), machine.getPose().getOri());
					}
					*/
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof MachineInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				MachineInfo info;
				
				try {
					info = MachineInfo.parseFrom(array);
					System.out.println("MachineInfo received:");
					for (int i = 0; i < info.getMachinesCount(); i++) {
						Machine machine = info.getMachines(i);
						Pose2D pose = machine.getPose();
						
						System.out.printf("  %-3s|%2s|%s @ (%f, %f, %f)\n",
								machine.getName(), machine.getType().substring(0, 2),
								machine.getTeamColor().toString().substring(0, 2),
								pose.getX(), pose.getY(), pose.getOri());
					}
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof MachineReportInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				MachineReportInfo report;
				
				try {
					report = MachineReportInfo.parseFrom(array);
					System.out.println("MachineReportInfo received:");
					
					if (report.getReportedMachinesCount() > 0) {
						System.out.printf("  Reported machines:");
						for (int i = 0; i < report.getReportedMachinesCount(); i++) {
							System.out.print(" " + report.getReportedMachines(i));
						}
						System.out.printf("\n");
					} else {
						System.out.println("  no machines reported, yet");
					}
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof RobotInfo) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				RobotInfo info;
				
				try {
					info = RobotInfo.parseFrom(array);
					System.out.println("Robot info received:");
					
					for (int i = 0; i < info.getRobotsCount(); i++) {
						Robot r = info.getRobots(i);
						Time t = r.getLastSeen();
						
						long robotsec = t.getSec();
						long cursec = System.currentTimeMillis() / 1000;
						long diff = (int) (cursec - robotsec);
						
						System.out.printf("  %d %s/%s @ %s: state %s, last seen %d sec ago  Maint cyc: %d  rem: %f\n",
								r.getNumber(), r.getName(), r.getTeam(), r.getHost(),
								r.getState().toString().substring(0, 3),
								diff, r.getMaintenanceCycles(), r.getMaintenanceTimeRemaining());
					}
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			}
		}

		public void connection_lost(IOException e) {
			System.out.println("Connection lost");
		}

		public void timeout() {}
	}

}

