/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.core.domain.constraint;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.b2international.snowowl.snomed.datastore.index.constraint.EnumeratedDefinitionFragment;
import com.google.common.base.Joiner;

/**
 * @since 6.5
 */
public final class SnomedEnumeratedDefinition extends SnomedConceptSetDefinition {

	private Set<String> conceptIds = newHashSet();

	public Set<String> getConceptIds() {
		return conceptIds;
	}
	
	public void setConceptIds(Set<String> conceptIds) {
		this.conceptIds = conceptIds;
	}
	
	@Override
	public String toEcl() {
		return Joiner.on(" OR ").join(conceptIds);
	}
	
	@Override
	public EnumeratedDefinitionFragment createModel() {
		return new EnumeratedDefinitionFragment(getId(), isActive(), getEffectiveTime(), getAuthor(), getConceptIds());
	}
	
	@Override
	public SnomedEnumeratedDefinition deepCopy(Date date, String userName) {
		final SnomedEnumeratedDefinition copy = new SnomedEnumeratedDefinition();
		
		copy.setActive(isActive());
		copy.setAuthor(userName);
		copy.setConceptIds(newHashSet(getConceptIds()));
		copy.setEffectiveTime(date.getTime());
		copy.setId(UUID.randomUUID().toString());
		
		return copy;
	}
	
	@Override
	public void collectConceptIds(Collection<String> conceptIds) {
		conceptIds.addAll(getConceptIds());
	}
	
	@Override
	public String validate() {
		final String parentMessage = super.validate();
		
		if (parentMessage != null) {
			return parentMessage;
		}
		
		if (getConceptIds().isEmpty()) { return String.format("%s with UUID %s should include at least one concept ID.", displayName(), getId()); }
		
		return null;
	}

	@Override
	public int structuralHashCode() {
		return 31 * super.structuralHashCode() + structuralHashCode(conceptIds);
	}

	@Override
	public boolean structurallyEquals(final SnomedConceptModelComponent obj) {
		if (this == obj) { return true; }
		if (!super.structurallyEquals(obj)) { return false; }
		if (getClass() != obj.getClass()) { return false; }

		final SnomedEnumeratedDefinition other = (SnomedEnumeratedDefinition) obj;

		if (!Objects.equals(conceptIds, other.conceptIds)) { return false; }
		return true;
	}
}
