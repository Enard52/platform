/*
 *
 *                 Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 *
 *     This material is part of the Dialogue Branch Platform, and is covered by the MIT License
 *                                        as outlined below.
 *
 *                                            ----------
 *
 * Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dialoguebranch.web.service.storage.model;

import com.dialoguebranch.execution.VariableUpdatedSource;
import jakarta.persistence.*;

/**
 * JPA entity representing one entry in the append-only {@code variable_history} table: the value a
 * single Dialogue Branch Variable was given (or {@code null}, if it was removed) by one change to a
 * user's variable store, within one project (#259).
 *
 * <p>Unlike {@link DBVariable}, which holds only the current value and is overwritten in place,
 * rows of this table are only ever inserted. The {@link #getId() id} is an auto-increment value, so
 * it also gives the order in which entries were recorded.</p>
 *
 * @author Harm op den Akker
 */
@Entity
@Table(
	name = "variable_history",
	indexes = {
		@Index(name = "ix_variable_history_user_project_name",
				columnList = "user_id, project_id, name, id"),
		@Index(name = "ix_variable_history_user_project",
				columnList = "user_id, project_id, id")
	})
public class DBVariableHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private DBUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private DBProject project;

	@Column(nullable = false)
	private String name;

	/** The JSON-serialized value, or {@code null} if the variable was removed by this change. */
	private String value;

	@Column(name = "recorded_time", nullable = false)
	private long recordedTime;

	@Column(name = "recorded_time_zone", nullable = false, length = 64)
	private String recordedTimeZone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private VariableUpdatedSource source;

	/**
	 * Creates an empty instance of {@link DBVariableHistory}, for JPA.
	 */
	protected DBVariableHistory() {
	}

	/**
	 * Creates an instance of {@link DBVariableHistory}.
	 *
	 * @param user the user whose variable changed.
	 * @param project the project the variable is scoped to.
	 * @param name the variable name.
	 * @param value the new value as a JSON string, or {@code null} if the variable was removed.
	 * @param recordedTime when the change was recorded by the Web Service, in epoch milliseconds.
	 * @param recordedTimeZone the user's time zone at that moment (an IANA time zone name).
	 * @param source the source of the change.
	 */
	public DBVariableHistory(DBUser user, DBProject project, String name, String value,
							 long recordedTime, String recordedTimeZone,
							 VariableUpdatedSource source) {
		this.user = user;
		this.project = project;
		this.name = name;
		this.value = value;
		this.recordedTime = recordedTime;
		this.recordedTimeZone = recordedTimeZone;
		this.source = source;
	}

	/**
	 * Returns the auto-increment identifier of this entry, which also orders entries by the moment
	 * they were recorded.
	 *
	 * @return the identifier.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the {@link DBUser} whose variable changed.
	 *
	 * @return the user.
	 */
	public DBUser getUser() {
		return user;
	}

	/**
	 * Returns the project the variable is scoped to.
	 *
	 * @return the project.
	 */
	public DBProject getProject() {
		return project;
	}

	/**
	 * Returns the name of the variable.
	 *
	 * @return the variable name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the JSON-serialized value the variable was given, or {@code null} if this entry
	 * records its removal.
	 *
	 * @return the value as a JSON string, or {@code null}.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns when this change was recorded by the Web Service, in epoch milliseconds.
	 *
	 * @return the recorded time in epoch milliseconds.
	 */
	public long getRecordedTime() {
		return recordedTime;
	}

	/**
	 * Returns the user's time zone at the moment this change was recorded (an IANA name).
	 *
	 * @return the time zone.
	 */
	public String getRecordedTimeZone() {
		return recordedTimeZone;
	}

	/**
	 * Returns the source of this change.
	 *
	 * @return the {@link VariableUpdatedSource}.
	 */
	public VariableUpdatedSource getSource() {
		return source;
	}

}
