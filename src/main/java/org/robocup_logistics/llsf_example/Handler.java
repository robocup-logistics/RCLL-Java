/*
 *
 * This file is part of the software provided by the tug ais group
 * Copyright (c) 2017, Clemens Muehlbacher
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.robocup_logistics.llsf_example;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos.BeaconSignal;
import org.robocup_logistics.llsf_msgs.GameStateProtos.GameState;
import org.robocup_logistics.llsf_msgs.MachineInfoProtos.Machine;
import org.robocup_logistics.llsf_msgs.MachineInfoProtos.MachineInfo;
import org.robocup_logistics.llsf_msgs.MachineReportProtos.MachineReportInfo;
import org.robocup_logistics.llsf_msgs.RobotInfoProtos.Robot;
import org.robocup_logistics.llsf_msgs.RobotInfoProtos.RobotInfo;
import org.robocup_logistics.llsf_msgs.TeamProtos.Team;
import org.robocup_logistics.llsf_msgs.TimeProtos.Time;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class Handler implements ProtobufMessageHandler {
	
	public void handle_message(ByteBuffer in_msg, GeneratedMessageV3 msg) {
		
		if (msg instanceof RobotInfo) {
			
			byte[] array = new byte[in_msg.capacity()];
			in_msg.rewind();
			in_msg.get(array);
			RobotInfo info;
			
			try {
				info = RobotInfo.parseFrom(array);
				int count = info.getRobotsCount();
				System.out.println("robot info:");
				System.out.println("  number of robots: " + count);
				List<Robot> robots = info.getRobotsList();
				for (int i = 0; i < robots.size(); i++) {
					Robot robot = robots.get(i);
					String name = robot.getName();
					String team = robot.getTeam();
					int number = robot.getNumber();
					System.out.println("    robot #" + number + ": " + name + " - " + team);
				}
				
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			
		} else if (msg instanceof BeaconSignal) {

			byte[] array = new byte[in_msg.capacity()];
			in_msg.rewind();
			in_msg.get(array);
			BeaconSignal bs;
			Time t;
			
			try {
				bs = BeaconSignal.parseFrom(array);
				t = bs.getTime();
				System.out.println("beacon signal:");
				System.out.println("  sec: " + t.getSec() + ", nanosec: " + t.getNsec());
				System.out.println("  name: " + bs.getPeerName() + " " + bs.getTeamName());
				
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
				User.gameStateReceived(state);
				
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
				int count = info.getMachinesCount();
				System.out.println("machine info:");
				System.out.println("  number of machines: " + count);
				List<Machine> machines = info.getMachinesList();
				for (int i = 0; i < machines.size(); i++) {
					Machine machine = machines.get(i);
					String name = machine.getName();
					String type = machine.getType();
					Team color = machine.getTeamColor();
					System.out.println("    machine " + name + ": " + type + " - " + color.toString());
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			
		} else if (msg instanceof MachineReportInfo) {
			
			byte[] array = new byte[in_msg.capacity()];
			in_msg.rewind();
			in_msg.get(array);
			MachineReportInfo info;
			
			try {
				info = MachineReportInfo.parseFrom(array);
				Team t = info.getTeamColor();
				System.out.println("machine report info:");
				System.out.println("  team color: " + t.toString());
				int count = info.getReportedMachinesCount();
				System.out.println("  number of reported machines: " + count);
				List<String> machines = info.getReportedMachinesList();
				for (int i = 0; i < machines.size(); i++) {
					String machine = machines.get(i);
					System.out.println("    machine " + machine);
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			
		}
	}

	@Override
	public void connection_lost(IOException e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void timeout() {
		// TODO Auto-generated method stub	
	}
}
