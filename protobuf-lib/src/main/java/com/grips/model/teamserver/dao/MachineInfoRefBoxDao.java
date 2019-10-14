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

import com.grips.model.teamserver.MachineInfoRefBox;
import com.grips.model.teamserver.ProductOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineInfoRefBoxDao extends CrudRepository<MachineInfoRefBox, String> {

    @Query(value = "select * from machine_info_ref_box where type like 'RS'", nativeQuery = true)
    public List<Object[]> getRingStations();

    public List<MachineInfoRefBox> findByTypeAndTeamColor(String type, MachineInfoRefBox.TeamColor color);

    public int countByRing1OrRing2(ProductOrder.RingColor r1, ProductOrder.RingColor r2);

    public List<MachineInfoRefBox> findByTeamColor(MachineInfoRefBox.TeamColor color);

    @Query(value = "SELECT state FROM machine_info_refbox WHERE name LIKE :machine", nativeQuery = true)
    public String getLatestMachineState(@Param(value = "machine") String machine);

    @Query(value = "SELECT bases_loaded FROM machine_info_refbox WHERE name LIKE :machine", nativeQuery = true)
    public int getBasesLoaded(@Param(value = "machine") String machine);

    public MachineInfoRefBox findByName(String name);
}
