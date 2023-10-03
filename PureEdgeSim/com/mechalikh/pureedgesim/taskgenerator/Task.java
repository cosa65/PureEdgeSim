/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.QueueElement;

import java.util.Objects;

/**
 * 
 * This class represents a default task in the PureEdgeSim simulation. It
 * extends the TaskAbstract class and adds additional properties such as
 * offloading time, device, container size, application ID, failure reason,
 * status, file size, computing node, output size, type, and orchestrator. It
 * also implements several methods for setting and getting the values of these
 * properties.
 */
public class Task implements Comparable<Task>, QueueElement {
	/**
	 * Enumeration for failure reasons of a Task.
	 */
	public enum FailureReason {
		FAILED_DUE_TO_LATENCY, FAILED_BECAUSE_DEVICE_DEAD, FAILED_DUE_TO_DEVICE_MOBILITY,
		NOT_GENERATED_BECAUSE_DEVICE_DEAD, NO_OFFLOADING_DESTINATIONS, INSUFFICIENT_RESOURCES, INSUFFICIENT_POWER
	}

	/**
	 * Enumeration for status of a Task.
	 */
	public enum Status {
		SUCCESS, FAILED
	}

	/**
	 *
	 * The maximum latency that this task can tolerate
	 */
	protected double maxLatency = 0;
	/**
	 *
	 * The actual network time this task experiences
	 */
	protected double actualNetworkTime = 0;
	/**
	 *
	 * The execution finish time of this task
	 */
	protected double execFinishTime = 0;
	/**
	 *
	 * The execution start time of this task
	 */
	protected double execStartTime = 0;
	/**
	 *
	 * The arrival time of this task
	 */
	protected double arrivalTime = 0;

	protected double completedTime;
	/**
	 *
	 * The length of this task
	 */
	protected double length = 0;
	/**
	 *
	 * The unique identifier of this task
	 */
	protected int id;
	/**
	 *
	 * The serial number of this task
	 */
	protected long serial;

	/**
	 *
	 * Gets the maximum allowed latency of the task.
	 *
	 * @return the maximum allowed latency
	 */
	public double getMaxLatency() {
		return maxLatency;
	}

	/**
	 *
	 * Sets the maximum allowed latency of the task and returns the modified task.
	 *
	 * @param maxLatency the maximum allowed latency to set
	 * @return the modified task
	 */
	public Task setMaxLatency(double maxLatency) {
		this.maxLatency = maxLatency;
		return this;
	}

	/**
	 *
	 * Gets the actual network time of the task.
	 *
	 * @return the actual network time
	 */
	public double getActualNetworkTime() {
		return actualNetworkTime;
	}

	/**
	 *
	 * Adds the given actual network time to the existing actual network time of the
	 * task.
	 *
	 * @param actualNetworkTime the actual network time to add
	 */
	public void addActualNetworkTime(double actualNetworkTime) {
		this.actualNetworkTime += actualNetworkTime;
	}

	public void setCompletedTime(double taskFinishTime) {
		double taskStartTime = this.getTime();
		this.completedTime = taskFinishTime - taskStartTime;
	};

	public double getCompletionTime() {
		return this.completedTime;
	}
	/**
	 *
	 * Gets the actual CPU time of the task.
	 *
	 * @return the actual CPU time
	 */
	public double getActualCpuTime() {
		return this.execFinishTime - this.getExecStartTime();
	}

	/**
	 *
	 * Gets the execution start time of the task.
	 *
	 * @return the execution start time
	 */
	public double getExecStartTime() {
		return execStartTime;
	}

	/**
	 *
	 * Gets the waiting time of the task.
	 *
	 * @return the waiting time
	 */
	public double getWatingTime() {
		return this.execStartTime - this.arrivalTime;
	}

	/**
	 *
	 * Sets the arrival time of the task to the given clock value.
	 *
	 * @param clock the clock value to set
	 */
	public void setArrivalTime(double clock) {
		this.arrivalTime = clock;
		this.execStartTime = clock;
	}

	/**
	 *
	 * Returns the total delay of this task, which is the sum of the actual network
	 * time, waiting time, and actual CPU time.
	 *
	 * @return the total delay of this task
	 */
	public double getTotalDelay() {
		return this.getActualNetworkTime() + this.getWatingTime() + this.getActualCpuTime();
	}

	/**
	 *
	 * Sets the start time of the execution of this task to the specified clock
	 * value.
	 *
	 * @param clock the clock value to set as the execution start time
	 */
	public void setExecutionStartTime(double clock) {
		this.execStartTime = clock;
		this.execFinishTime = clock;
	}

	/**
	 *
	 * Sets the finish time of the execution of this task to the specified clock
	 * value.
	 *
	 * @param clock the clock value to set as the execution finish time
	 */
	public void setExecutionFinishTime(double clock) {
		this.execFinishTime = clock;
	}

	/**
	 *
	 * Sets the ID of this task to the specified value.
	 *
	 * @param id the ID to set for this task
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 *
	 * Returns the ID of this task.
	 *
	 * @return the ID of this task
	 */
	public int getId() {
		return id;
	}

	/**
	 *
	 * Returns the length of this task.
	 *
	 * @return the length of this task
	 */
	public double getLength() {
		return length;
	}

	/**
	 *
	 * Sets the length of this task to the specified value.
	 *
	 * @param length the length to set for this task
	 * @return a reference to this task
	 */
	public Task setLength(double length) {
		this.length = length;
		return this;
	}

	/**
	 *
	 * Sets the serial number of this task to the specified value.
	 *
	 * @param l the serial number to set for this task
	 */
	public void setSerial(long l) {
		this.serial = l;
	}

	/**
	 *
	 * Compares this task with the specified task for order. Returns a negative
	 * integer, zero, or a positive integer as this task is less than, equal to, or
	 * greater than the specified task.
	 *
	 * @param that the task to be compared
	 *
	 * @return a negative integer, zero, or a positive integer as this task is less
	 *         than, equal to, or greater than the specified task
	 */
	public int compareTo(final Task that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(this.getTime(), that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	/**
	 *
	 * Indicates whether some other object is "equal to" this one.
	 *
	 * @param obj the object to compare to
	 * @return true if this object is the same as the obj argument; false otherwise
	 */
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Task that = (Task) obj;
		return Double.compare(that.getTime(), getTime()) == 0 && getSerial() == that.getSerial();
	}

	/**
	 *
	 * Returns a hash code value for the object. The hash code is generated based on
	 * the time and serial number of the task.
	 *
	 * @return the hash code value for the object.
	 */
	public int hashCode() {
		return Objects.hash(getTime(), getSerial());
	}

	/**
	 *
	 * Returns the serial number of the task.
	 *
	 * @return the serial number of the task.
	 */
	public long getSerial() {
		return this.serial;
	}

	/**
	 * The time required for offloading the task in seconds.
	 */
	protected double offloadingTime;

	/**
	 * The edge device where the task will be offloaded.
	 */
	protected ComputingNode device = ComputingNode.NOT_THE_REAL_NULL;

	/**
	 * The size of the container where the task is encapsulated in bits.
	 */
	protected long containerSize;

	/**
	 * The registry node where the task will be registered.
	 */
	protected ComputingNode registry = ComputingNode.NOT_THE_REAL_NULL;

	/**
	 * The ID of the application that this task belongs to.
	 */
	protected int applicationID;

	/**
	 * The reason of failure for the task, if any.
	 */
	protected FailureReason failureReason;

	/**
	 * The status of the task.
	 */
	protected Status status = Status.SUCCESS;

	/**
	 * The size of the input file for the task in bits.
	 */
	protected long fileSize;

	/**
	 * The computing node where the task will be offloaded to.
	 */
	protected ComputingNode computingNode = ComputingNode.NOT_THE_REAL_NULL;

	/**
	 * The size of the output file for the task in bits.
	 */
	protected double outputSize;

	/**
	 * The type of the task.
	 */
	protected String type;

	/**
	 * The orchestrator node that will manage the execution of this task.
	 */
	protected ComputingNode orchestrator = ComputingNode.NOT_THE_REAL_NULL;

	/**
	 * Constructs a Task object with a specified task ID.
	 *
	 * @param id The ID of the task.
	 */
	public Task(int id) {
		this.setId(id);
	}

	/**
	 * Sets the offloading time for the task.
	 *
	 * @param time The offloading time in seconds.
	 */
	public void setTime(double time) {
		this.offloadingTime = time;
	}

	/**
	 * Returns the offloading time for the task.
	 *
	 * @return The offloading time in seconds.
	 */
	public double getTime() {
		return offloadingTime;
	}

	/**
	 * Returns the edge device assigned to the task.
	 *
	 * @return The edge device assigned to the task.
	 */
	public ComputingNode getEdgeDevice() {
		return device;
	}

	/**
	 * Assigns an edge device to the task.
	 *
	 * @param device The edge device to be assigned to the task.
	 */
	public Task setEdgeDevice(ComputingNode device) {
		this.device = device;
		return this;
	}

	/**
	 * Sets the container size for the task in bits.
	 *
	 * @param containerSize The container size in bits.
	 */
	public Task setContainerSizeInBits(long containerSize) {
		this.containerSize = containerSize;
		return this;
	}

	/**
	 * Returns the container size for the task in bits.
	 *
	 * @return The container size in bits.
	 */
	public long getContainerSizeInBits() {
		return containerSize;
	}

	/**
	 * Returns the container size for the task in megabytes.
	 *
	 * @return The container size in megabytes.
	 */
	public double getContainerSizeInMBytes() {
		return containerSize / 8000000.0;
	}

	/**
	 * Returns the orchestrator for the task. If no orchestrator has been set, the
	 * edge device is set as the orchestrator.
	 *
	 * @return The orchestrator for the task.
	 */
	public ComputingNode getOrchestrator() {
		if (this.orchestrator == ComputingNode.NOT_THE_REAL_NULL) {
			this.getEdgeDevice().setAsOrchestrator(true);
			return this.getEdgeDevice();
		}
		return this.orchestrator;
	}

	/**
	 * Assigns an orchestrator to the task.
	 *
	 * @param orchestrator The orchestrator to be assigned to the task.
	 */

	public void setOrchestrator(ComputingNode orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * 
	 * Returns the computing node registry.
	 * 
	 * @return the computing node registry.
	 */
	public ComputingNode getRegistry() {
		return registry;
	}

	/**
	 * 
	 * Sets the computing node registry.
	 * 
	 * @param registry the computing node registry.
	 */
	public Task setRegistry(ComputingNode registry) {
		this.registry = registry;
		return this;
	}

	/**
	 * 
	 * Returns the ID of the application.
	 * 
	 * @return the ID of the application.
	 */
	public int getApplicationID() {
		return applicationID;
	}

	public boolean getOrchestratorOnly() {
		Application application = SimulationParameters.applicationList.get(this.getApplicationID());

		return application.getOrchestratorOnly();
	}

	/**
	 * 
	 * Sets the ID of the application.
	 * 
	 * @param applicationID the ID of the application.
	 */
	public Task setApplicationID(int applicationID) {
		this.applicationID = applicationID;
		return this;
	}

	/**
	 * 
	 * Returns the failure reason of the task.
	 * 
	 * @return the failure reason of the task.
	 */
	public FailureReason getFailureReason() {
		return failureReason;
	}

	/**
	 * 
	 * Sets the failure reason of the task and updates its status to "FAILED".
	 * 
	 * @param reason the failure reason of the task.
	 */
	public void setFailureReason(FailureReason reason) {
		this.setStatus(Task.Status.FAILED);
		this.failureReason = reason;
	}

	/**
	 * 
	 * Returns the offloading destination computing node.
	 * 
	 * @return the offloading destination computing node.
	 */
	public ComputingNode getOffloadingDestination() {
		return computingNode;
	}

	/**
	 * 
	 * Sets the offloading destination computing node.
	 * 
	 * @param applicationPlacementLocation the offloading destination computing
	 *                                     node.
	 */
	public void setOffloadingDestination(ComputingNode applicationPlacementLocation) {
		this.computingNode = applicationPlacementLocation;
	}

	/**
	 * 
	 * Sets the size of the file for the task and returns this task.
	 * 
	 * @param requestSize the size of the file for the task.
	 * @return this task.
	 */
	public Task setFileSizeInBits(long requestSize) {
		this.fileSize = requestSize;
		return this;
	}

	/**
	 * 
	 * Sets the output size of the task and returns this task.
	 * 
	 * @param outputSize the output size of the task.
	 * @return this task.
	 */
	public Task setOutputSizeInBits(long outputSize) {
		this.outputSize = outputSize;
		return this;
	}

	/**
	 * 
	 * Returns the size of the file for the task.
	 * 
	 * @return the size of the file for the task.
	 */
	public double getFileSizeInBits() {
		return fileSize;
	}

	/**
	 * 
	 * Returns the output size of the task.
	 * 
	 * @return the output size of the task.
	 */
	public double getOutputSizeInBits() {
		return this.outputSize;
	}

	/**
	 * 
	 * Sets the status of the task.
	 * 
	 * @param status the status of the task.
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * 
	 * Returns the status of the task.
	 * 
	 * @return the status of the task.
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * 
	 * Returns the type of the task.
	 * 
	 * @return the type of the task.
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Sets the type of the task.
	 * 
	 * @param type the type of the task.
	 */
	public Task setType(String type) {
		this.type = type;
		return this;
	}

}
