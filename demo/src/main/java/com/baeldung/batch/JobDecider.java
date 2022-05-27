package com.baeldung.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class JobDecider implements JobExecutionDecider {

	private int num=0;
	@Override
	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        num++;
        if (num >= 5) {
            return new FlowExecutionStatus("COMPLETED");
          } else {
            return new FlowExecutionStatus("CONTINUE");
          }
	}

}
