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

package com.dialoguebranch.web.service.repository;

import com.dialoguebranch.web.service.storage.model.DBProject;
import com.dialoguebranch.web.service.storage.model.DBUser;
import com.dialoguebranch.web.service.storage.model.DBVariableHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link DBVariableHistory} entities (#259).
 *
 * @author Harm op den Akker
 */
public interface DBVariableHistoryRepository extends JpaRepository<DBVariableHistory, Long> {

	/**
	 * Finds a page of a user's variable history within a project, across all variables, newest
	 * first.
	 *
	 * @param user the user whose history to retrieve.
	 * @param project the project the variables are scoped to.
	 * @param pageable the page to retrieve.
	 * @return the matching entries, newest first.
	 */
	List<DBVariableHistory> findByUserAndProjectOrderByIdDesc(DBUser user, DBProject project,
			Pageable pageable);

	/**
	 * Finds a page of the history of a single variable of a user within a project, newest first.
	 *
	 * @param user the user whose history to retrieve.
	 * @param project the project the variable is scoped to.
	 * @param name the name of the variable.
	 * @param pageable the page to retrieve.
	 * @return the matching entries, newest first.
	 */
	List<DBVariableHistory> findByUserAndProjectAndNameOrderByIdDesc(DBUser user,
			DBProject project, String name, Pageable pageable);

}
