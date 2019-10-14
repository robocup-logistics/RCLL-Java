package com.grips.model.teamserver.dao;

import com.grips.model.scheduler.LockPart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LockPartDao extends CrudRepository<LockPart, Long> {

	public void deleteByMachine(String machine);
	public List<LockPart> findByMachine(String machine);
	public int countByMachine(String machine);
	public List<LockPart> findByProductId(long productId);

}
