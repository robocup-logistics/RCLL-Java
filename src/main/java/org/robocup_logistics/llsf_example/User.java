package org.robocup_logistics.llsf_example;

import org.robocup_logistics.llsf_comm.ProtobufUpdBroadcastConnection;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;
import org.robocup_logistics.llsf_msgs.GameStateProtos;
import org.robocup_logistics.llsf_msgs.RobotInfoProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;

import java.io.IOException;

public class User {
	
	private final static String TEAM_NAME = "YourTeam";
	private final static String ENCRYPTION_KEY = "YourKey";
	private static TeamProtos.Team TEAM_COLOR;
	
	private static ProtobufUpdBroadcastConnection peerPublic;
	private static ProtobufUpdBroadcastConnection peerPrivate;
	
	public static void main(String[] args) {
		
		//Send and receive message via broadcast
		//IMPORTANT: If you want to connect to a remote refbox, set both ports to 4444.
		//If you want to communicate with a local refbox (as in this example), you need to
		//set the send port to 4445 and edit the refbox's config.yaml as described in the
		//Configuration tutorial. This is required, because your refbox and your Java program
		//cannot listen on the same ports. After changing the configuration, your local refbox
		//will listen on port 4445 and send to port 4444, so your send and receive ports have
		//to be inverted.
		//See the Usage tutorial for more information: https://trac.fawkesrobotics.org/wiki/LLSFRefBox/Java/Usage
		peerPublic = new ProtobufUpdBroadcastConnection("x.x.x.255", 4445, 4444);
		try {
			peerPublic.start("User");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		peerPublic.add_message(RobotInfoProtos.RobotInfo.class);
		peerPublic.add_message(BeaconSignalProtos.BeaconSignal.class);
		peerPublic.add_message(GameStateProtos.GameState.class);
		
		Handler handler = new Handler();
		peerPublic.register_handler(handler);
		
		//Send and receive message via stream
//		ProtobufClient client = new ProtobufClient("localhost", 4444);
//		try {
//			client.connect();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (UnresolvedAddressException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//		client.<RobotInfo>add_message(RobotInfo.class);
//		
//		Handler handler = new Handler();
//		client.register_handler(handler);
//		
//		NanoSecondsTimestampProvider nstp = new NanoSecondsTimestampProvider();
//
//		long ms = System.currentTimeMillis();
//		long ns = nstp.currentNanoSecondsTimestamp();
//
//		int sec = (int) (ms / 1000);
//		long nsec = ns - (ms * 1000000L);
//			
//		Time t = Time.newBuilder().setSec(sec).setNsec(nsec).build();
//		BeaconSignal bs = BeaconSignal.newBuilder().setTime(t).setSeq(1).setNumber(1).setPeerName("R-1").setTeamName(TEAM_NAME).build();
//			
//		ProtobufMessage msg = new ProtobufMessage(2000,1,bs);		
//		client.enqueue(msg);
		
//		
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//		}
//
//		client.disconnect(true);
		
	}
	
	public static void gameStateReceived(GameStateProtos.GameState state) {
		/*
		if (peerPrivate != null) {
			return;
		}
		
		//If you get a GameState containing a team name equal to yours,
		//start sending and receiving encrypted messages on the corresponding ports.
		if (TEAM_NAME.equals(state.getTeamCyan())) {
			TEAM_COLOR = Team.CYAN;
		} else if (TEAM_NAME.equals(state.getTeamMagenta())) {
			TEAM_COLOR = Team.MAGENTA;
		} else {
			return;
		}
		
		//The same procedure as in the public peer: Use different ports when you run a local refbox.
		//See config.yaml for further information on the ports.
		switch (TEAM_COLOR) {
		case CYAN:
			peerPrivate = new ProtobufBroadcastPeer("x.x.x.255", 4446, 4441, true, 2, ENCRYPTION_KEY);
			break;
		case MAGENTA:
			peerPrivate = new ProtobufBroadcastPeer("x.x.x.255", 4447, 4442, true, 2, ENCRYPTION_KEY);
			break;
		}
	
		if (peerPrivate != null) {
			try {
				peerPrivate.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			peerPrivate.<MachineInfo>add_message(MachineInfo.class);
			peerPrivate.<MachineReportInfo>add_message(MachineReportInfo.class);
			peerPrivate.<OrderInfo>add_message(OrderInfo.class);
			
			Handler handler = new Handler();
			peerPrivate.register_handler(handler);
	
			MachineReportProtos.MachineReportEntry mi = MachineReportProtos.MachineReportEntry.newBuilder().setName("M1").setType("T1").build();
			MachineReport mr = MachineReport.newBuilder().addMachines(mi).setTeamColor(Team.CYAN).build();
			ProtobufMessage machineReport = new ProtobufMessage(2000, 61, mr);
			peerPrivate.enqueue(machineReport);
			
			new Thread(new Runnable() {
				
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
						BeaconSignal bs = BeaconSignal.newBuilder().setTime(t).setSeq(1).setNumber(1).setPeerName("R-1").setTeamName(TEAM_NAME).setTeamColor(TEAM_COLOR).build();
						
						ProtobufMessage msg = new ProtobufMessage(2000, 1, bs);
						peerPrivate.enqueue(msg);
					}
					
				}
				
			}).start();
		}
	*/
	}


}

