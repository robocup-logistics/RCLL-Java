package com.grips.model.teamserver.dao;

import com.grips.model.scheduler.PartDemand;
import com.grips.model.scheduler.ProductTask;
import com.grips.model.scheduler.SubProductionTask;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartDemandDao extends CrudRepository<PartDemand, Long> {

	public Iterable<PartDemand> findByTaskIsNullAndMachineContainsOrderByIdAsc(String machine);
	public PartDemand findByTask(SubProductionTask task);
	public int countByMachine(String machine);
	public void deleteByTask(SubProductionTask task);
	public List<PartDemand> findByTaskIsNullAndMachine(String machine);
	public void deleteByProductTask(ProductTask pTask);
	public int countByProductTask(ProductTask pTask);
	public List<PartDemand> findByTaskIsNullAndProductTask(ProductTask pTask);
	public void deleteByMachineAndProductTask(String machine, ProductTask pTask);

}
