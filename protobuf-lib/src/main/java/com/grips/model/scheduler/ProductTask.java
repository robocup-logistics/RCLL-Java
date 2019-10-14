package com.grips.model.scheduler;

import com.grips.model.teamserver.ProductOrder;

import javax.persistence.*;
import java.util.Collection;

@Entity
public class ProductTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

    @Enumerated(EnumType.STRING)
	private ProductState state;

    @OneToMany(mappedBy = "productTask", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Collection<SubProductionTask> subProductionTasks;

    @OneToOne
	private ProductOrder productOrder;

    @Lob
    private String stateLogging;

	public ProductTask() {
	}

	public Collection<SubProductionTask> getSubProductionTask() {
		return subProductionTasks;
	}

	public void setSubProductionTasks(Collection<SubProductionTask> subProductionTasks) {
		this.subProductionTasks = subProductionTasks;
	}

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state, String stateLogging) {
        this.state = state;
        this.stateLogging += ("; " + stateLogging);
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public String getStateLogging() {
        return stateLogging;
    }

    public void setStateLogging(String stateLogging) {
        this.stateLogging = stateLogging;
    }

    public enum ProductState {
	    TBD, INWORK, FINISHED, FAILED
    }
}
