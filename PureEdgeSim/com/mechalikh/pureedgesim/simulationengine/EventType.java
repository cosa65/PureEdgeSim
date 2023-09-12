package com.mechalikh.pureedgesim.simulationengine;

public enum EventType {
    PRINT_LOG(1),
    SHOW_PROGRESS(2),
    EXECUTE_TASK(3),
    TRANSFER_RESULTS_TO_ORCH(4),
    RESULT_RETURN_FINISHED(5),
    SEND_TO_ORCH(6),
    UPDATE_REAL_TIME_CHARTS(7),
    SEND_TASK_FROM_ORCH_TO_DESTINATION(8),
    NEXT_BATCH(9),
    UPDATE_STATUS(10),
    UPDATE_PROGRESS(11),
    SEND_REQUEST_FROM_ORCH_TO_DESTINATION(12),
	TRANSFER_FINISHED(13),
	DOWNLOAD_CONTAINER(14),
	SEND_REQUEST_FROM_DEVICE_TO_ORCH(15),
	SEND_RESULT_TO_ORCH(16),
	SEND_RESULT_FROM_ORCH_TO_DEV(17),
    EXECUTION_FINISHED(18),
    UPDATE_CLUSTERS(11000);
    private final int tag;

    private EventType(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return this.tag;
    };

    public String toString() {
        return this.name();
    }
}