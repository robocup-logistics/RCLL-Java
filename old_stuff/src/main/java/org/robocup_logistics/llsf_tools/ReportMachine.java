package org.robocup_logistics.llsf_tools;

public class ReportMachine {
	/*

	private static String MACHINE_NAME;
	private static String MACHINE_TYPE;
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
	
	private static ProtobufBroadcastPeer peerPublic;
	private static ProtobufBroadcastPeer peerPrivate;
	
	private static boolean crypto_setup = false;
	
	public static void main(String[] args) {
		
		if (args.length < 4) {
			System.out.println("Usage: ReportMachine.jar <machine-name> <machine-type> <team-name> <encryption-key> [-h <host> (only for a remote refbox)]");
			System.exit(1);
		}
		
		MACHINE_NAME = args[0];
		MACHINE_TYPE = args[1];
		TEAM_NAME = args[2];
		ENCRYPTION_KEY = args[3];
		TEAM_COLOR = Team.CYAN;
		
		if (args.length > 4) {
			if (args[4].equals("-h")) {
				HOST = args[5];
				local = false;
			} else {
				System.out.println("Usage: ReportMachine.jar <machine-name> <machine-type> <team-name> <encryption-key> [-h <host> (only for a remote refbox)]");
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
		
		peerPublic = new ProtobufBroadcastPeer(HOST, local ? SENDPORT : RECVPORT, RECVPORT);
		try {
			peerPublic.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		peerPublic.<BeaconSignal>add_message(BeaconSignal.class);
		peerPublic.<GameState>add_message(GameState.class);
		peerPublic.<MachineReport>add_message(MachineReport.class);
		peerPublic.<MachineReportInfo>add_message(MachineReportInfo.class);
		
		Handler handler = new Handler();
		peerPublic.register_handler(handler);
		
		System.out.println("Waiting for BeaconSignal...");
		
	}
	
	private static class Handler implements ProtobufMessageHandler {
		
		public void handle_message(ByteBuffer in_msg, GeneratedMessage msg) {
			
			if (msg instanceof BeaconSignal) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				BeaconSignal bs;
				
				try {
					bs = BeaconSignal.parseFrom(array);
					
					if (crypto_setup) {
						if (bs.getTeamName().equals("LLSF") && bs.getPeerName().equals("RefBox")) {
							System.out.println("Announcing machine type");
							MachineReportProtos.MachineReportEntry mi = MachineReportProtos.MachineReportEntry.newBuilder().setName(MACHINE_NAME).setType(MACHINE_TYPE).build();
							MachineReport mr = MachineReport.newBuilder().addMachines(mi).setTeamColor(TEAM_COLOR).build();
							ProtobufMessage machineReport = new ProtobufMessage(2000, 61, mr);
							peerPrivate.enqueue(machineReport);
						}	
					}
					
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
				
			} else if (msg instanceof GameState) {
				
				byte[] array = new byte[in_msg.capacity()];
				in_msg.rewind();
				in_msg.get(array);
				GameState state;
				
				try {
					state = GameState.parseFrom(array);
					
					if (!crypto_setup) {
						crypto_setup = true;
						
						if (TEAM_NAME.equals(state.getTeamCyan())) {
							TEAM_COLOR = Team.CYAN;
						} else if (TEAM_NAME.equals(state.getTeamMagenta())) {
							TEAM_COLOR = Team.MAGENTA;
						} else {
							crypto_setup = false;
							return;
						}
						
						switch (TEAM_COLOR) {
						case CYAN:
							peerPrivate = new ProtobufBroadcastPeer(HOST, local ? CYAN_SENDPORT : CYAN_RECVPORT, CYAN_RECVPORT, true, 2, ENCRYPTION_KEY);
							break;
						case MAGENTA:
							peerPrivate = new ProtobufBroadcastPeer(HOST, local ? MAGENTA_SENDPORT : MAGENTA_RECVPORT, MAGENTA_RECVPORT, true, 2, ENCRYPTION_KEY);
							break;
						}
					
						if (peerPrivate != null) {
							try {
								peerPrivate.start();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							peerPrivate.<BeaconSignal>add_message(BeaconSignal.class);
							peerPrivate.<GameState>add_message(GameState.class);
							peerPrivate.<MachineReport>add_message(MachineReport.class);
							peerPrivate.<MachineReportInfo>add_message(MachineReportInfo.class);
							
							Handler handler = new Handler();
							peerPrivate.register_handler(handler);
						}
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
					
					if (report.getTeamColor() == TEAM_COLOR) {
						System.out.printf("Reported machines (%s):", report.getTeamColor().toString());
						for (int i = 0; i < report.getReportedMachinesCount(); i++) {
							System.out.print(" " + report.getReportedMachines(i));
						}
						System.out.printf("\n");
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
	*/
}

