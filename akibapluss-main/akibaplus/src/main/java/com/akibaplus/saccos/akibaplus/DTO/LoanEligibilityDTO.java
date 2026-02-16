package com.akibaplus.saccos.akibaplus.DTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LoanEligibilityDTO implements Serializable {

	private boolean eligible;
	private List<String> reasons = new ArrayList<>();

	public LoanEligibilityDTO() {
		this.eligible = false;
	}

	public LoanEligibilityDTO(boolean eligible) {
		this.eligible = eligible;
	}

	public boolean isEligible() {
		return eligible;
	}

	public void setEligible(boolean eligible) {
		this.eligible = eligible;
	}

	public List<String> getReasons() {
		return reasons;
	}

	public void setReasons(List<String> reasons) {
		this.reasons = reasons;
	}

	public void addReason(String reason) {
		if (reason == null || reason.isBlank()) return;
		this.reasons.add(reason);
		// Mark not eligible if a reason is added
		this.eligible = false;
	}
}

