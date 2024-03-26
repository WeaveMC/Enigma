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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import matcher.model.type.ClassEnvironment;
import matcher.model.type.ClassInstance;
import matcher.model.type.FieldInstance;
import matcher.model.type.MethodInstance;
import matcher.model.type.Signature.ClassSignature;
import matcher.model.type.Signature.FieldSignature;
import matcher.model.type.Signature.MethodSignature;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.IndexEntryResolver;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;

public class MatcherIndex extends JarIndex {
	private final ClassEnvironment env;
	private final EntryIndex entryIndex;
	private final Map<String, ClassEntry> classesByName;
	private final MatcherInheritanceIndex inheritanceIndex;
	private final ReferenceIndex referenceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;
	private final PackageVisibilityIndex packageVisibilityIndex;
	private final EntryResolver entryResolver;

	private final Collection<JarIndexer> indexers;

	private final Multimap<String, MethodDefEntry> methodImplementations = HashMultimap.create();
	private final ListMultimap<ClassEntry, ParentedEntry> childrenByClass;

	public MatcherIndex(ClassEnvironment env, Map<String, ClassEntry> classesByName, EntryIndex entryIndex, MatcherInheritanceIndex inheritanceIndex, ReferenceIndex referenceIndex, BridgeMethodIndex bridgeMethodIndex, PackageVisibilityIndex packageVisibilityIndex) {
		super(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);

		this.env = env;
		this.classesByName = classesByName;
		this.entryIndex = entryIndex;
		this.inheritanceIndex = inheritanceIndex;
		this.referenceIndex = referenceIndex;
		this.bridgeMethodIndex = bridgeMethodIndex;
		this.packageVisibilityIndex = packageVisibilityIndex;
		this.indexers = List.of(entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
		this.entryResolver = new IndexEntryResolver(this);
		this.childrenByClass = ArrayListMultimap.create();
	}

	public static MatcherIndex empty(ClassEnvironment env) {
		EntryIndex entryIndex = new EntryIndex();
		Map<String, ClassEntry> classesByName = new HashMap<>();
		MatcherInheritanceIndex inheritanceIndex = new MatcherInheritanceIndex(env, entryIndex, classesByName);
		ReferenceIndex referenceIndex = new ReferenceIndex();
		BridgeMethodIndex bridgeMethodIndex = new BridgeMethodIndex(entryIndex, inheritanceIndex, referenceIndex);
		PackageVisibilityIndex packageVisibilityIndex = new PackageVisibilityIndex();
		return new MatcherIndex(env, classesByName, entryIndex, inheritanceIndex, referenceIndex, bridgeMethodIndex, packageVisibilityIndex);
	}

	public void indexJar(Set<String> realClasses, ProgressListener progress) {
		progress.init(4, I18n.translate("progress.jar.indexing"));

		progress.step(1, I18n.translate("progress.jar.indexing.entries"));

		for (String className : realClasses) {
			ClassInstance clsInstance = env.getEnvA().getClsByName(className);
			ClassSignature clsSignature = clsInstance.getSignature();
			ClassInstance superCls = clsInstance.getSuperClass();
			ClassDefEntry clsEntry;

			indexClass((clsEntry = ClassDefEntry.parse(clsInstance.getAccess(), className, clsSignature == null ? null : clsSignature.toString(),
					superCls == null ? null : superCls.getName(), clsInstance.getInterfaces().stream().map(ClassInstance::getName).toArray(String[]::new))));

			for (FieldInstance fld : clsInstance.getFields()) {
				FieldSignature fldSignature = fld.getSignature();
				indexField(FieldDefEntry.parse(clsEntry, fld.getAccess(), fld.getName(), fld.getDesc(), fldSignature == null ? null : fldSignature.toString()));
			}

			for (MethodInstance mth : clsInstance.getMethods()) {
				MethodSignature mthSignature = mth.getSignature();
				indexMethod(MethodDefEntry.parse(clsEntry, mth.getAccess(), mth.getName(), mth.getDesc(), mthSignature == null ? null : mthSignature.toString()));
			}
		}

		progress.step(2, I18n.translate("progress.jar.indexing.references"));

		for (String className : realClasses) {
			ClassInstance cls = env.getEnvA().getClsByName(className);

			try {
				cls.getMergedAsmNode().accept(new IndexReferenceVisitor(this, entryIndex, inheritanceIndex, Enigma.ASM_VERSION));
			} catch (Exception e) {
				throw new RuntimeException("Exception while indexing class: " + className, e);
			}
		}

		progress.step(3, I18n.translate("progress.jar.indexing.methods"));
		bridgeMethodIndex.findBridgeMethods();

		progress.step(4, I18n.translate("progress.jar.indexing.process"));
		processIndex(this);
	}

	@Override
	public void processIndex(JarIndex index) {
		indexers.forEach(indexer -> indexer.processIndex(index));
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		classesByName.put(classEntry.getFullName(), classEntry);

		if (classEntry.isJre()) {
			return;
		}

		ClassEntry superClass = classEntry.getSuperClass();

		if (superClass != null) {
			classesByName.putIfAbsent(superClass.getFullName(), superClass);
		}

		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			if (classEntry.equals(interfaceEntry)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + classEntry);
			}

			classesByName.putIfAbsent(interfaceEntry.getFullName(), interfaceEntry);
		}

		indexers.forEach(indexer -> indexer.indexClass(classEntry));

		if (classEntry.isInnerClass() && !classEntry.getAccess().isSynthetic()) {
			childrenByClass.put(classEntry.getParent(), classEntry);
		}
	}

	@Override
	public void indexField(FieldDefEntry fieldEntry) {
		if (fieldEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexField(fieldEntry));

		if (!fieldEntry.getAccess().isSynthetic()) {
			childrenByClass.put(fieldEntry.getParent(), fieldEntry);
		}
	}

	@Override
	public void indexMethod(MethodDefEntry methodEntry) {
		if (methodEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethod(methodEntry));

		if (!methodEntry.getAccess().isSynthetic() && !methodEntry.getName().equals("<clinit>")) {
			childrenByClass.put(methodEntry.getParent(), methodEntry);
		}

		if (!methodEntry.isConstructor()) {
			methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	@Override
	public void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexClassReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexMethodReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexFieldReference(callerEntry, referencedEntry, targetType));
	}

	@Override
	public void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
		if (callerEntry.getParent().isJre()) {
			return;
		}

		indexers.forEach(indexer -> indexer.indexLambda(callerEntry, lambda, targetType));
	}

	public EntryIndex getEntryIndex() {
		return entryIndex;
	}

	public InheritanceIndex getInheritanceIndex() {
		return this.inheritanceIndex;
	}

	public ReferenceIndex getReferenceIndex() {
		return referenceIndex;
	}

	public BridgeMethodIndex getBridgeMethodIndex() {
		return bridgeMethodIndex;
	}

	public PackageVisibilityIndex getPackageVisibilityIndex() {
		return packageVisibilityIndex;
	}

	public EntryResolver getEntryResolver() {
		return entryResolver;
	}

	public ListMultimap<ClassEntry, ParentedEntry> getChildrenByClass() {
		return this.childrenByClass;
	}

	public boolean isIndexed(String internalName) {
		return env.getClsByName(internalName) != null;
	}
}
