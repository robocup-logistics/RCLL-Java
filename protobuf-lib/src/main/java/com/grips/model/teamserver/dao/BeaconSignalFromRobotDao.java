/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
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

package com.grips.model.teamserver.dao;

import com.grips.model.teamserver.BeaconSignalFromRobot;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BeaconSignalFromRobotDao extends CrudRepository<BeaconSignalFromRobot, Long> {


    /*
    @Query(value = "select time_nano_seconds from beacon_signal_from_robot " +
            "where robot_id = 1 ORDER BY time_nano_seconds DESC LIMIT 1", nativeQuery = true)
    public BigInteger getLastTimeStampRobot1();

    @Query(value = "select time_nano_seconds from beacon_signal_from_robot " +
            "where robot_id = 2 ORDER BY time_nano_seconds DESC LIMIT 1", nativeQuery = true)
    public BigInteger getLastTimeStampRobot2();

    @Query(value = "select time_nano_seconds from beacon_signal_from_robot " +
            "where robot_id = 3 ORDER BY time_nano_seconds DESC LIMIT 1", nativeQuery = true)
    public BigInteger getLastTimeStampRobot3();
    */

    @Query(value = "SELECT local_timestamp from beacon_signal_from_robot " +
            "where robot_id = 1 order by local_timestamp DESC LIMIT 1", nativeQuery =  true)
    public Long getLatestLocalTimeStampRobot1();

    @Query(value = "SELECT local_timestamp from beacon_signal_from_robot " +
            "where robot_id = 2 order by local_timestamp DESC LIMIT 1", nativeQuery =  true)
    public Long getLatestLocalTimeStampRobot2();

    @Query(value = "SELECT local_timestamp from beacon_signal_from_robot " +
            "where robot_id = 2 order by local_timestamp DESC LIMIT 1", nativeQuery =  true)
    public Long getLatestLocalTimeStampRobot3();

    public BeaconSignalFromRobot findFirstByRobotIdOrderByLocalTimestampDesc(String robotId);
}
