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

import com.grips.model.scheduler.ProductTask;
import com.grips.model.scheduler.SubProductionTask;
import com.grips.model.teamserver.ProductOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubProductionTaskDao extends CrudRepository<SubProductionTask, Long> {

    public List<Object[]> findByOrderInfoId(Long id);

    public Collection<SubProductionTask> findByStateAndRobotId(SubProductionTask.TaskState state, int robotId);

    public List<SubProductionTask> findByProductTaskInAndState(List<ProductTask> activeTasks, SubProductionTask.TaskState state);

    public List<SubProductionTask> findByProductTask(ProductTask task);

    public SubProductionTask findByIdAndRobotId(long id, int robotId);

    public Collection<SubProductionTask> findByRobotId(int robotId);

    public SubProductionTask findBySameRobotSubTask(SubProductionTask task);

    public Collection<SubProductionTask> findByStateAndPreConditionTasksIn(SubProductionTask.TaskState state, Collection<SubProductionTask> pre);

    public Collection<SubProductionTask> findByPreConditionTasksIn(Collection<SubProductionTask> pre);

    public List<SubProductionTask> findByRobotIdAndState(int robotId, SubProductionTask.TaskState state);

    @Query(value = "SELECT * FROM sub_production_task " +
            "WHERE robot_id = :robotId AND (state LIKE 'FAILED' OR state LIKE 'SUCCESS') ORDER BY end_time DESC LIMIT 1", nativeQuery = true)
    public SubProductionTask findLatestDoneTaskbyRobotId(@Param("robotId") int robotId);

    public List<SubProductionTask> findByProductTaskProductOrderComplexityAndState(ProductOrder.Complexity complexity, SubProductionTask.TaskState state);

    public Collection<SubProductionTask> findByMachineAndSideInAndStateIn(String machine, Collection<SubProductionTask.MachineSide> sides, Collection<SubProductionTask.TaskState> states);

    public Collection<SubProductionTask> findByMachineAndTypeAndStateIn(String machine, SubProductionTask.TaskType type, Collection<SubProductionTask.TaskState> states);

    public List<SubProductionTask> findByTypeAndMachineAndStateIn(SubProductionTask.TaskType type, String machine, Collection<SubProductionTask.TaskState> states);

    public List<SubProductionTask> findByProductTaskAndState(ProductTask productTask, SubProductionTask.TaskState state);
}
