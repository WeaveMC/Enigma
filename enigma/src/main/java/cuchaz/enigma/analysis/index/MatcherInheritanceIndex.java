/*******************************************************************************
* Copyright (c) 2015 Jeff Martin.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public
* License v3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl.html
*
* <p>Contributors:
* Jeff Martin - initial API and implementation
******************************************************************************/

package cuchaz.enigma.analysis.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import matcher.model.type.ClassEnvironment;
import matcher.model.type.ClassInstance;

import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class MatcherInheritanceIndex extends InheritanceIndex {
	private final ClassEnvironment env;
	private final EntryIndex entryIndex;
	private final Map<String, ClassEntry> classesByName;

	public MatcherInheritanceIndex(ClassEnvironment env, EntryIndex entryIndex, Map<String, ClassEntry> classesByName) {
		super(entryIndex);
		this.env = env;
		this.entryIndex = entryIndex;
		this.classesByName = classesByName;
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
	}

	public Collection<ClassEntry> getParents(ClassEntry classEntry) {
		ClassInstance clsInstance = env.getEnvA().getClsByName(classEntry.getFullName());
		List<ClassEntry> parents = new ArrayList<>();

		for (ClassInstance itf : clsInstance.getInterfaces()) {
			ClassEntry cls = classesByName.get(itf.getName());

			if (cls != null) {
				parents.add(cls);
			}
		}

		ClassInstance superCls = clsInstance.getSuperClass();

		if (superCls != null) {
			ClassEntry cls = classesByName.get(superCls.getName());

			if (cls != null) {
				parents.add(cls);
			}
		}

		return parents;
	}

	public Collection<ClassEntry> getChildren(ClassEntry classEntry) {
		ClassInstance clsInstance = env.getEnvA().getClsByName(classEntry.getFullName());
		List<ClassEntry> children = new ArrayList<>();

		for (ClassInstance impl : clsInstance.getImplementers()) {
			ClassEntry cls = classesByName.get(impl.getName());

			if (cls != null) {
				children.add(cls);
			}
		}

		for (ClassInstance child : clsInstance.getChildClasses()) {
			ClassEntry cls = classesByName.get(child.getName());

			if (cls != null) {
				children.add(cls);
			}
		}

		return children;
	}

	public Collection<ClassEntry> getDescendants(ClassEntry classEntry) {
		Collection<ClassEntry> descendants = new HashSet<>();

		LinkedList<ClassEntry> descendantQueue = new LinkedList<>();
		descendantQueue.push(classEntry);

		while (!descendantQueue.isEmpty()) {
			ClassEntry descendant = descendantQueue.pop();
			Collection<ClassEntry> children = getChildren(descendant);

			children.forEach(descendantQueue::push);
			descendants.addAll(children);
		}

		return descendants;
	}

	public Set<ClassEntry> getAncestors(ClassEntry classEntry) {
		Set<ClassEntry> ancestors = Sets.newHashSet();

		LinkedList<ClassEntry> ancestorQueue = new LinkedList<>();
		ancestorQueue.push(classEntry);

		while (!ancestorQueue.isEmpty()) {
			ClassEntry ancestor = ancestorQueue.pop();
			Collection<ClassEntry> parents = getParents(ancestor);

			parents.forEach(ancestorQueue::push);
			ancestors.addAll(parents);
		}

		return ancestors;
	}

	public Relation computeClassRelation(ClassEntry classEntry, ClassEntry potentialAncestor) {
		if (potentialAncestor.getName().equals("java/lang/Object")) {
			return Relation.RELATED;
		}

		ClassInstance classInstance = env.getEnvA().getClsByName(classEntry.getFullName());
		ClassInstance ancestorInstance = env.getEnvA().getClsByName(potentialAncestor.getFullName());

		if (classInstance == null || ancestorInstance == null) {
			return Relation.UNKNOWN;
		}

		ClassInstance commonSuperClass = classInstance.getCommonSuperClass(ancestorInstance);

		if (commonSuperClass != null && !commonSuperClass.getName().equals("java/lang/Object")) {
			return Relation.RELATED;
		}

		return Relation.UNRELATED;
	}

	public boolean isParent(ClassEntry classEntry) {
		ClassInstance clsInstance = env.getEnvA().getClsByName(classEntry.getFullName());
		return !clsInstance.getChildClasses().isEmpty() || !clsInstance.getImplementers().isEmpty();
	}

	public boolean hasParents(ClassEntry classEntry) {
		ClassInstance clsInstance = env.getEnvA().getClsByName(classEntry.getFullName());
		return clsInstance.getSuperClass() != null || !clsInstance.getInterfaces().isEmpty();
	}
}
