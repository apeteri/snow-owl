/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.core.api;

/**
 * 
 * Represents a terminology independent statement with an <b>object</b> (source),
 * an <b>attribute</b> (type) and a <b>value</b> (destination) ID.
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link IStatement#getObjectId() <em>Retrieve the source concept ID.</em>}</li>
 *   <li>{@link IStatement#getAttributeId() <em>Retrieve the type conceptID.</em>}</li>
 *   <li>{@link IStatement#getValueId() <em>Retrieve the destination concept ID.</em>}</li>
 * </ul>
 * </p>
 * @param <K> - type parameter of the key (this is the unique identifier).
 */
public interface IStatement<K> extends IComponent<K> {

	/**
	 * Returns with the source concept ID of the relationship.
	 * @return the source concept ID of the statement.
	 * @see IStatement
	 */
	K getObjectId();
	
	/**
	 * Returns with the relationship type concept ID of the relationship. <i>(E.g.: IS_A)<i/>
	 * @return type concept ID of the statement.
	 * @see IStatement
	 */
	K getAttributeId();
	
	/**
	 * Returns with the destination concept ID of the relationship.
	 * @return the destination concept ID of the statement.
	 * @see IStatement
	 */
	K getValueId();
}