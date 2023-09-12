package com.mechalikh.pureedgesim.simulationengine;

import java.util.Objects;

public class Event implements Comparable<Event>, QueueElement {
	double time;
	protected SimEntity simEntity;
	protected int tag;
	protected Object data;
	protected long serial;

	protected EventType type;

	public Event(SimEntity simEntity, Double time, EventType type) {
		this.simEntity = simEntity;
		this.time = time;
		this.type = type;
	}

	public Event(SimEntity simEntity, Double time, EventType eventType, Object data) {
		this.simEntity = simEntity;
		this.time = time;
		this.type = eventType;
		this.data = data;
	}

	public EventType getType() { return this.type; }

//	public int getTag() {
//		return tag;
//	}

	public double getTime() {
		return time;
	}

	public SimEntity getSimEntity() {
		return simEntity;
	}

	public Object getData() {
		return data;
	}

	public void setSerial(long l) {
		this.serial = l;
	}

	@Override
	public int compareTo(final Event that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(time, that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Event that = (Event) obj;
		return Double.compare(that.getTime(), getTime()) == 0 && this.getType() == that.getType()
			&& getSerial() == that.getSerial();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTime(), this.getType(), getSerial());
	}

	public long getSerial() {
		return this.serial;
	}

}
